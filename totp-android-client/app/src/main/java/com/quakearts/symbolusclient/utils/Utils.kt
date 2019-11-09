package com.quakearts.symbolusclient.utils

import android.app.Application
import android.content.Context
import android.view.animation.Interpolator
import com.squareup.moshi.Moshi
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.lifecycle.android.AndroidLifecycle
import com.tinder.scarlet.messageadapter.moshi.MoshiMessageAdapter
import com.tinder.scarlet.retry.ExponentialBackoffStrategy
import com.tinder.scarlet.streamadapter.rxjava2.RxJava2StreamAdapterFactory
import com.tinder.scarlet.websocket.ShutdownReason
import com.tinder.scarlet.websocket.okhttp.OkHttpWebSocket
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import io.reactivex.Flowable
import io.reactivex.disposables.Disposable
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.*
import javax.crypto.spec.SecretKeySpec
import java.nio.ByteBuffer
import java.security.KeyStore
import java.security.spec.KeySpec
import java.util.*
import javax.crypto.*
import javax.crypto.spec.PBEKeySpec
import kotlin.collections.HashMap
import kotlin.math.E
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow

object Options{
    const val pbeKeyAlgorithm = "PBEwithSHAANDTWOFISH-CBC"
    const val fileStoreAlgorithm = "PBEwithSHAANDTWOFISH-CBC"
    const val deviceFileName = "device.properties"
    const val keystoreName = "AndroidKeystore"
    const val macAlgorithm = "HmacSHA256"
    const val otpLength = 6
    const val timeStep = 30000L
    const val totpUrl = "http://localhost:8081/totp-provisioning"
    const val totpWsUrl = "ws://localhost:8081/device-connection"
    val pbeSalt = "64MTYEQN4HSLRWEW".toByteArray()
    const val pbeIterations = 23
    const val seedKey = "com.quakearts.HH39BRS6"
    const val aliasProperty = "ALIAS"
}

class Counter(val updateTimeAction:(timeString:String)->Unit,
              val updateOTPAction:()->Unit){
    var run = false

    @Synchronized
    fun start() {
        if(run)
            return

        run = true
        Thread{runCounter()}.start()
    }

    private fun runCounter() {
        var lastTime = 0L
        while (run){
            val time = (System.currentTimeMillis() % Options.timeStep)/1000
            updateTimeAction(String.format(":%02d", time))
            if(time < lastTime){
                updateOTPAction()
            }
            lastTime = time
            Thread.sleep(500)
        }
    }

    @Synchronized
    fun stop() {
        run = false
    }
}

class Device(private val _id:String="",
             private val _initial:Long=0){
    private val format = "%0"+ Options.otpLength +"d"
    val id : String
        get() = _id
    val initialCounter : Long
        get() = _initial

    var keyStore:KeyStore = KeyStore.getInstance(Options.keystoreName)

    fun generateOtp():String{
        return truncatedStringOf(
            generatedHmacFrom(
                timeValueUsing(System.currentTimeMillis())))
    }

    fun generateOtpFromTimestamp(timestamp: Long):String{
        return truncatedStringOf(
            generatedHmacFrom(
                timeValueUsing(timestamp)))
    }

    private fun truncatedStringOf(hashBytes:ByteArray) : String{
        val offset = abs(hashBytes[hashBytes.size - 1] % (hashBytes.size - 4))
        var code = hashBytes[offset].toInt() and 0x7f shl 24 or (
                hashBytes[offset + 1].toInt() and 0xff shl 16) or (
                hashBytes[offset + 2].toInt() and 0xff shl 8) or (
                hashBytes[offset + 3].toInt() and 0xff)
        code = (code % (10.0).pow(Options.otpLength
                .toDouble())).toInt()
        return String.format(format, code)
    }

    private fun generatedHmacFrom(currentTimeBytes:ByteArray):ByteArray{
        val key = (keyStore.getEntry(Options.seedKey, null)!! as KeyStore.SecretKeyEntry).secretKey
        val mac = Mac.getInstance(Options.macAlgorithm)
        mac.init(key)
        mac.update(id.toByteArray())
        return mac.doFinal(currentTimeBytes)
    }

    private fun timeValueUsing(timesStamp:Long):ByteArray {
        val timestamp = (timesStamp - initialCounter) / Options.timeStep
        return ByteBuffer.allocate(8).putLong(timestamp).array()
    }

    fun discard(){
        keyStore.deleteEntry(Options.keystoreName)
    }
}

data class Payload(val id:Long, val message:HashMap<String,String>, val timestamp: Long)

interface TOTPWebsocketService {
    @Send
    fun sendResponse(payload:Payload)

    @Receive
    fun observePayload(): Flowable<Payload>
}

typealias DeviceConnectionListener = (doOk : ()->Unit, doCancel: () -> Unit, messages: Map<String,String>)->Unit

class DeviceConnection(private val device: Device){
    private var subscription : Disposable? = null
    var otpAuthorizationRequestListener : DeviceConnectionListener? = null
        @Synchronized set
    var otpSigningRequestListener : DeviceConnectionListener? = null
        @Synchronized set

    fun connect(application: Application){
        val okHTTPClient = OkHttpClient()
        val protocol = OkHttpWebSocket(
            okHTTPClient,
            OkHttpWebSocket.SimpleRequestFactory(
                {Request.Builder().url(Options.totpWsUrl).build()},
                { ShutdownReason.GRACEFUL }
            )
        )
        val websocketConfiguration = Scarlet.Configuration(
            messageAdapterFactories = listOf(MoshiMessageAdapter.Factory()),
            streamAdapterFactories = listOf(RxJava2StreamAdapterFactory()),
            backoffStrategy = ExponentialBackoffStrategy(10000,360000),
            lifecycle = AndroidLifecycle.ofApplicationForeground(application)
        )
        val websocket = Scarlet(protocol, websocketConfiguration)
        val totpWebsocketService = websocket.create<TOTPWebsocketService>()

        subscription = totpWebsocketService.observePayload().subscribe{
            if(it.message.containsKey("requestType")
                && otpAuthorizationRequestListener != null
                && otpSigningRequestListener != null){
                when(it.message["requestType"]) {
                    "otp" -> synchronized(otpAuthorizationRequestListener!!){
                            otpAuthorizationRequestListener!!({
                                it.message["otp"] = device.generateOtp()
                                totpWebsocketService.sendResponse(it)
                            }, {
                                sendError("Request rejected", it, totpWebsocketService)
                            }, it.message)
                        }
                    "otp-signing"-> synchronized(otpSigningRequestListener!!){
                        otpSigningRequestListener!!({
                            val timestamp = System.currentTimeMillis()
                            it.message["otp"]=device.generateOtpFromTimestamp(timestamp)
                            it.message["totp-timestamp"]=timestamp.toString()
                            totpWebsocketService.sendResponse(it)
                        },{
                            sendError("Request rejected", it, totpWebsocketService)
                        }, it.message)
                    }
                    else -> sendError("Invalid request type",it,totpWebsocketService)
                }
            } else {
                sendError("Unavailable to respond",it,totpWebsocketService)
                it.message["error"]="Invalid request"
            }
        }
    }

    private fun sendError(error: String, it:Payload, totpWebsocketService: TOTPWebsocketService){
        it.message["error"] = error
        totpWebsocketService.sendResponse(it)
    }

    fun dispose(){
        if(subscription!=null)
            subscription!!.dispose()
    }
}

object HexTool {
     fun byteAsHex(buf: ByteArray?): String {
        if (buf == null)
            return ""

        val strbuf = StringBuilder(buf.size * 2)
        var i = 0
        while (i < buf.size) {
            if (buf[i].toInt() and 0xff < 0x10)
                strbuf.append("0")

            strbuf.append((buf[i].toInt() and 0xff).toLong().toString(16))
            i++
        }

        return strbuf.toString()
    }

    fun hexAsByte(hexstring: String?): ByteArray {
        if (hexstring == null || hexstring.isEmpty())
            return ByteArray(0)

        require(hexstring.length % 2 == 0) { "The hexidecimal string is not valid" }
        val results = ByteArray(hexstring.length / 2)
        try {
            var i = 0
            while (i < hexstring.length - 1) {
                results[i / 2] =
                    java.lang.Long.parseLong(hexstring.substring(i, i + 2), 16).toByte()
                i += 2
            }
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("The hexidecimal string is not valid", e)
        }
        return results
    }
}

data class ProvisioningResponse(val seed: String, val initialCounter: Long)

data class ActivationRequest(var token: String?, var alias: String?)

object DeviceProvisioner {
    fun provision(deviceId:String, alias:String, pin:String, context: Context):Device{
        val moshi = Moshi.Builder().build()
        val provisioningResponseAdapter = moshi.adapter(ProvisioningResponse::class.java)
        val activationRequestAdapter = moshi.adapter(ActivationRequest::class.java)

        val totpServiceClient = OkHttpClient()

        var device = Device()
        val provisioningRequest = Request.Builder()
            .url(Options.totpUrl+"/provisioning/$deviceId").build()
        var seed = ByteArray(0)
        totpServiceClient.newCall(provisioningRequest).execute().use {
            if(!it.isSuccessful) throw IOException("Provisioning for device ID $deviceId failed $it")

            val provisioningResponse = provisioningResponseAdapter.fromJson(it.body()!!.source())
            seed = HexTool.hexAsByte(provisioningResponse!!.seed)
            val key = SecretKeySpec(seed, Options.macAlgorithm)
            val keyStore = KeyStore.getInstance(Options.keystoreName)
            keyStore.setEntry(Options.keystoreName,KeyStore.SecretKeyEntry(key), null)

            device = Device(deviceId, provisioningResponse.initialCounter)
        }

        val activationRequest = Request.Builder()
            .url(Options.totpUrl+"/provisioning/$deviceId")
            .post(
                RequestBody.create(MediaType.get("application/json; charset=utf-8"),
                    activationRequestAdapter
                        .toJson(ActivationRequest(token = device.generateOtp(), alias = alias))))
            .build()

        totpServiceClient.newCall(activationRequest).execute().use {
            if(!it.isSuccessful) throw IOException("Provisioning for device ID $deviceId failed $it")
        }

        DeviceStorage.saveDevice(device, seed, pin, context)

        return device
    }
}

object DeviceStorage {
    fun hasDeviceFile(context: Context):Boolean {
        return File(context.filesDir,Options.deviceFileName).isFile
    }

    fun loadDeviceFromStorage(pin:String, context: Context):Device{
        val cipher = prepareCipher(pin, Cipher.DECRYPT_MODE)
        val fileIn = CipherInputStream(FileInputStream(File(context.filesDir,Options.deviceFileName)), cipher)
        val deviceProperties = Properties()
        fileIn.use {
            deviceProperties.load(fileIn)
        }

        val seed = HexTool.hexAsByte(deviceProperties.getProperty("seed"))
        val keyStore = KeyStore.getInstance(Options.keystoreName)
        keyStore.setEntry(Options.seedKey, KeyStore.SecretKeyEntry(SecretKeySpec(seed, Options.macAlgorithm)), null)

        val initialCounter = deviceProperties.getProperty("initial.counter").toLong()
        val deviceId = deviceProperties.getProperty("device.id")

        return Device(deviceId,initialCounter)
    }

    fun saveDevice(device: Device, seed:ByteArray, pin:String, context: Context){
        val deviceProperties = Properties()
        deviceProperties.setProperty("seed", HexTool.byteAsHex(seed))
        deviceProperties.setProperty("initial.counter", device.initialCounter.toString())
        deviceProperties.setProperty("device.id", device.id)

        val cipher = prepareCipher(pin, Cipher.ENCRYPT_MODE)
        val fileOut = CipherOutputStream(FileOutputStream(File(context.filesDir,Options.deviceFileName)), cipher)
        fileOut.use {
            deviceProperties.store(fileOut,"")
        }
    }

    private fun generatePBEKeySpec(pin:String): KeySpec {
        return PBEKeySpec(pin.toCharArray(),Options.pbeSalt,Options.pbeIterations, 128)
    }

    private fun prepareCipher(pin: String, mode:Int): Cipher {
        val keySpec = generatePBEKeySpec(pin)
        val keyFactory = SecretKeyFactory.getInstance(Options.pbeKeyAlgorithm)
        val key = keyFactory.generateSecret(keySpec)
        val cipher = Cipher.getInstance(Options.fileStoreAlgorithm)
        cipher.init(mode, key)
        return cipher
    }

}

class BounceInterpolator(private val amplitude:Double, private val frequency:Double) :
    Interpolator {
    override fun getInterpolation(time: Float): Float {
        return ((-1 * E.pow(-time / amplitude) *
                cos(frequency * time)) + 1).toFloat()
    }
}

object TOTPApplication {
    private var device: Device? = null
    private var deviceConnection : DeviceConnection? = null

    fun started() : Boolean {
        return device != null && deviceConnection != null
    }

    fun loadDevice(alias: String, pin:String, application: Application){
        device = if(DeviceStorage.hasDeviceFile(application)){
            DeviceStorage.loadDeviceFromStorage(pin, application)
        } else {
            DeviceProvisioner.provision("",alias, pin, application)
        }
        deviceConnection = DeviceConnection(device!!)
        deviceConnection!!.connect(application)
    }

    fun generateOtp() : String? {
        return device?.generateOtp()
    }

    fun registerListeners(
        otpAuthorizationRequestListener: DeviceConnectionListener,
        otpSigningRequestListener: DeviceConnectionListener
    ) {
        deviceConnection?.otpAuthorizationRequestListener = otpAuthorizationRequestListener
        deviceConnection?.otpSigningRequestListener = otpSigningRequestListener
    }

    fun unRegisterListener(){
        deviceConnection?.otpAuthorizationRequestListener = null
        deviceConnection?.otpSigningRequestListener = null
    }

    fun onCleared() {
        if(device!=null){
            device!!.discard()
            device = null
        }
        if(deviceConnection!=null){
            deviceConnection!!.dispose()
            deviceConnection = null
        }
    }
}