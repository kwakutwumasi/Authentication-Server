package com.quakearts.symbolusclient.utils

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.security.KeyPairGeneratorSpec
import android.telephony.TelephonyManager
import android.view.animation.Interpolator
import androidx.core.content.ContextCompat
import com.squareup.moshi.*
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
import java.math.BigInteger
import javax.crypto.spec.SecretKeySpec
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.spec.KeySpec
import java.util.*
import javax.crypto.*
import javax.crypto.spec.PBEKeySpec
import javax.security.auth.x500.X500Principal
import kotlin.collections.HashMap
import kotlin.experimental.and
import kotlin.math.E
import kotlin.math.cos
import kotlin.math.pow

object Options{
    const val showHideThreshold = 5
    const val counterSleepTime = 500L
    const val keyPairValidity = 10
    const val keyPairAlias = "PBEKeyPair"
    const val deviceFileSaltName = "device.salt"
    const val keyPairAlgorithm = "RSA/ECB/PKCS1Padding"
    const val pbeKeyAlgorithm = "PBEwithSHAANDTWOFISH-CBC"
    const val fileStoreAlgorithm = "PBEwithSHAANDTWOFISH-CBC"
    const val deviceFileName = "device.properties"
    const val macAlgorithm = "HmacSHA256"
    const val otpLength = 6
    const val timeStep = 30000L
    const val totpUrl = "http://10.0.2.2:8082/totp-provisioning"
    const val totpWsUrl = "ws://10.0.2.2:8082/device-connection"
    const val aliasProperty = "ALIAS"
    const val pbeIterations = 23
    const val resetThreshold = 3
}

class Counter(val updateTimeAction:(timeString:String)->Unit,
              val updateOTPAction:()->Unit,
              private val initialCounter: Long){
    private var run = false

    @Synchronized
    fun start() {
        if(run)
            return

        run = true
        Thread{runCounter()}.start()
    }

    private fun runCounter() {
        while (run){
            val time = ((System.currentTimeMillis()-initialCounter) % Options.timeStep)/1000
            updateTimeAction(String.format(":%02d", time))
            if(time <= Options.resetThreshold){
                updateOTPAction()
            }
            Thread.sleep(Options.counterSleepTime)
        }
    }

    @Synchronized
    fun stop() {
        run = false
    }
}

class Device(private val _id:String="",
             private val _initial:Long=0,
             private val _key:SecretKey=SecretKeyFactory.getInstance(Options.pbeKeyAlgorithm)
                 .generateSecret(PBEKeySpec("default".toCharArray()))){
    private val format = "%0"+ Options.otpLength +"d"
    private val power = intArrayOf(1,10,100,1000,10000,100000,1000000,10000000,100000000)
    val id : String
        get() = _id
    val initialCounter : Long
        get() = _initial

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
        val offset = (hashBytes[hashBytes.size - 1] and 0xf).toInt()
        var code = hashBytes[offset].toInt() and 0x7f shl 24 or (
                hashBytes[offset + 1].toInt() and 0xff shl 16) or (
                hashBytes[offset + 2].toInt() and 0xff shl 8) or (
                hashBytes[offset + 3].toInt() and 0xff)
        code %= power[Options.otpLength]
        return String.format(format, code)
    }

    private fun generatedHmacFrom(currentTimeBytes:ByteArray):ByteArray{
        val mac = Mac.getInstance(Options.macAlgorithm)
        mac.init(_key)
        mac.update(id.toByteArray(Charset.forName("UTF-8")))
        return mac.doFinal(currentTimeBytes)
    }

    private fun timeValueUsing(timesStamp:Long):ByteArray {
        val deltaCounter = timesStamp - initialCounter
        val timeCounter = deltaCounter / Options.timeStep
        return ByteBuffer.allocate(8).putLong(timeCounter).array()
    }

    fun signTransaction(request:String):String {
        val mac = Mac.getInstance(Options.macAlgorithm)
        mac.init(_key)
        mac.update(id.toByteArray(Charset.forName("UTF-8")))
        return HexTool.byteAsHex(mac.doFinal(request.toByteArray(Charset.forName("UTF-8"))))
    }
}

data class Payload(val id:Long, val message:HashMap<String, String>, val timestamp: Long)

class PayloadMessageAdapter : JsonAdapter<HashMap<String,String>>() {
    @FromJson
    override fun fromJson(reader: JsonReader): HashMap<String, String>? {
        val message = HashMap<String,String>()
        reader.beginObject()
        while (reader.hasNext()){
            message[reader.nextName()] = reader.nextString()
        }
        reader.endObject()
        return message
    }

    @ToJson
    override fun toJson(writer: JsonWriter, value: HashMap<String, String>?) {
        if(value!=null) {
            writer.beginObject()
            value.map { writer.name(it.key).value(it.value) }
            writer.endObject()
        }
    }
}

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
                { Request.Builder().url(Options.totpWsUrl+"/"+device.id+"/"+device.generateOtp()).build() },
                { ShutdownReason.GRACEFUL }
            )
        )

        val moshi = Moshi.Builder().add(PayloadMessageAdapter())
            .build()

        val websocketConfiguration = Scarlet.Configuration(
            messageAdapterFactories = listOf(MoshiMessageAdapter.Factory(moshi = moshi)),
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
                            val signingMessage = TreeMap<String, String>()
                            it.message.entries.filter { entry -> !"deviceId,iat,requestType".contains(entry.key) }
                                .forEach { entry -> signingMessage[entry.key] = entry.value }
                            val signingString = signingMessage.entries.joinToString("") { entry -> entry.key + entry.value }
                            it.message["signature"]=device.signTransaction(signingString)
                            totpWebsocketService.sendResponse(it)
                        },{
                            sendError("Request rejected", it, totpWebsocketService)
                        }, it.message)
                    }
                    else -> sendError("Invalid request type",it,totpWebsocketService)
                }
            } else {
                sendError("Unable to respond",it,totpWebsocketService)
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
            if ((buf[i].toInt() and 0xff) < 0x10)
                strbuf.append("0")

            strbuf.append((buf[i].toInt() and 0xff).toString(16))
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

        var device:Device? = null
        val provisioningRequest = Request.Builder()
            .url(Options.totpUrl+"/provisioning/$deviceId").post(
                RequestBody.create(null,"")
            ).build()
        var seed = ByteArray(0)
        totpServiceClient.newCall(provisioningRequest).execute().use {
            if(!it.isSuccessful)throw IOException("Provisioning for device ID $deviceId failed with code ${it.code()}")

            val provisioningResponse = provisioningResponseAdapter.fromJson(it.body()?.source())
            seed = HexTool.hexAsByte(provisioningResponse?.seed)
            val key = SecretKeySpec(seed, Options.macAlgorithm)

            device = Device(deviceId, provisioningResponse!!.initialCounter, key)
        }

        val activationRequest = Request.Builder()
            .url(Options.totpUrl+"/provisioning/$deviceId")
            .put(
                RequestBody.create(MediaType.get("application/json; charset=utf-8"),
                    activationRequestAdapter
                        .toJson(ActivationRequest(token = device!!.generateOtp(), alias = alias))))
            .build()

        totpServiceClient.newCall(activationRequest).execute().use {
            if(!it.isSuccessful)throw IOException("Activation for device ID $deviceId failed with code ${it.code()}")
        }

        DeviceStorage.saveDevice(device!!, seed, pin, context)

        return device!!
    }
}

object DeviceStorage {
    private val keystore =  KeyStore.getInstance("AndroidKeyStore")
    init {
        keystore.load(null)
    }

    fun hasDeviceFile(context: Context):Boolean {
        return File(context.filesDir,Options.deviceFileName).isFile
    }

    fun loadDeviceFromStorage(pin:String, context: Context):Device{
        val salt = decryptAndLoadSalt(context)
        val cipher = prepareCipher(pin, salt, Cipher.DECRYPT_MODE)
        val fileIn = CipherInputStream(FileInputStream(File(context.filesDir,Options.deviceFileName)), cipher)
        val deviceProperties = Properties()
        fileIn.use {
            deviceProperties.load(fileIn)
        }

        val seed = HexTool.hexAsByte(deviceProperties.getProperty("seed"))
        val key = SecretKeySpec(seed, Options.macAlgorithm)

        val initialCounter = deviceProperties.getProperty("initial.counter").toLong()
        val deviceId = deviceProperties.getProperty("device.id")

        return Device(deviceId,initialCounter,key)
    }

    private fun decryptAndLoadSalt(context: Context): ByteArray{
        val privateKeyEntry = keystore.getEntry(Options.keyPairAlias, null) as KeyStore.PrivateKeyEntry

        val cipher = Cipher.getInstance(Options.keyPairAlgorithm)
        cipher.init(Cipher.DECRYPT_MODE, privateKeyEntry.privateKey)

        val fileIn = CipherInputStream(FileInputStream(File(context.filesDir,Options.deviceFileSaltName)), cipher)
        val salt = ByteArray(32)
        fileIn.use {
            it.read(salt)
        }
        return salt
    }

    fun saveDevice(device: Device, seed:ByteArray, pin:String, context: Context){
        val deviceProperties = Properties()
        deviceProperties.setProperty("seed",HexTool.byteAsHex(seed))
        deviceProperties.setProperty("initial.counter", device.initialCounter.toString())
        deviceProperties.setProperty("device.id", device.id)

        val salt = createAndSaveSalt(context)
        val cipher = prepareCipher(pin, salt, Cipher.ENCRYPT_MODE)
        val fileOut = CipherOutputStream(FileOutputStream(File(context.filesDir,Options.deviceFileName)), cipher)
        fileOut.use {
            deviceProperties.store(it,"")
        }
    }

    private fun createAndSaveSalt(context: Context):ByteArray {
        val salt = ByteArray(32)
        val secureRandom = SecureRandom()
        secureRandom.nextBytes(salt)
        generateKeyPair(context)
        encryptSalt(salt,context)
        return salt
    }

    private fun generateKeyPair(context: Context) {
        val start = Calendar.getInstance()
        val end = Calendar.getInstance()
        end.add(Calendar.YEAR, Options.keyPairValidity)
        val spec = KeyPairGeneratorSpec.Builder(context)
            .setAlias(Options.keyPairAlias)
            .setSubject(X500Principal("CN=PBE Salt Key, O=Android Authority"))
            .setSerialNumber(BigInteger.ONE)
            .setStartDate(start.time)
            .setEndDate(end.time)
            .build()
        val generator = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore")
        generator.initialize(spec)

        generator.generateKeyPair()
    }

    private fun encryptSalt(salt:ByteArray, context: Context) {
        val privateKeyEntry = keystore.getEntry(Options.keyPairAlias, null) as KeyStore.PrivateKeyEntry

        val cipher = Cipher.getInstance(Options.keyPairAlgorithm, "AndroidOpenSSL")
        cipher.init(Cipher.ENCRYPT_MODE, privateKeyEntry.certificate.publicKey)

        val fileOut = CipherOutputStream(FileOutputStream(File(context.filesDir,Options.deviceFileSaltName)), cipher)
        fileOut.use {
            it.write(salt)
        }
    }

    private fun generatePBEKeySpec(pin:String, pbeSalt:ByteArray): KeySpec {
        return PBEKeySpec(pin.toCharArray(),pbeSalt,Options.pbeIterations, 128)
    }

    private fun prepareCipher(pin: String, pbeSalt:ByteArray, mode:Int): Cipher {
        val keySpec = generatePBEKeySpec(pin, pbeSalt)
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
    var initialDeviceId: String = ""

    fun started() : Boolean {
        return device != null && deviceConnection != null
    }

    fun loadDevice(alias: String, pin:String, application: Application){
        device = if(DeviceStorage.hasDeviceFile(application)){
            DeviceStorage.loadDeviceFromStorage(pin, application)
        } else {
            DeviceProvisioner.provision(initialDeviceId, alias, pin, application)
        }
        deviceConnection = DeviceConnection(device!!)
        deviceConnection!!.connect(application)
    }

    fun generateDeviceIdIfNecessary(application: Application):Unit {
        if(!DeviceStorage.hasDeviceFile(application)) {
            val manager =
                application.applicationContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

            val permission =
                ContextCompat.checkSelfPermission(application, Manifest.permission.READ_PHONE_STATE)

            val digest = MessageDigest.getInstance("SHA-256")
            val deviceId = (java.lang.Long.toHexString(Date().time) +
                    Integer.toHexString(Random().nextInt())).toUpperCase(Locale.ENGLISH)
            digest.update(deviceId.toByteArray())
            initialDeviceId = HexTool.byteAsHex(digest.digest())
        }
    }

    fun generateOtp() : String? {
        return device?.generateOtp()
    }

    fun getInitialCounter():Long? {
        return device?.initialCounter
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
            device = null
        }
        if(deviceConnection!=null){
            deviceConnection!!.dispose()
            deviceConnection = null
        }
    }
}