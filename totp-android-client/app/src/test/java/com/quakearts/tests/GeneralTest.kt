package com.quakearts.tests

import com.quakearts.symbolusclient.utils.*
import com.squareup.moshi.*
import org.junit.Assert.assertThat
import org.hamcrest.core.Is.`is`
import org.junit.Test
import javax.crypto.spec.SecretKeySpec
import kotlin.collections.HashMap

class GeneralTest {
    @Test
    fun hexToByte(){
        val array = HexTool.hexAsByte("ff800001")
        assertThat(array.size,`is`(4))
        assertThat(array[0].toInt() and 0xFF,`is`(255))
        assertThat(array[1].toInt() and 0xFF,`is`(128))
        assertThat(array[2].toInt() and 0xFF,`is`(0))
        assertThat(array[3].toInt() and 0xFF,`is`(1))
    }

    @Test
    fun byteToHex(){
        val array = ByteArray(4)
        array[0] = 0xFF.toByte()
        array[1] = 0x80.toByte()
        array[2] = 0x00.toByte()
        array[3] = 0x01.toByte()

        assertThat(HexTool.byteAsHex(array),`is`("ff800001"))
    }

    @Test
    fun generateOtp(){
        val device = Device("",0, SecretKeySpec("12345678901234567890123456789012".toByteArray(),
            Options.macAlgorithm))
        assertThat(device.generateOtpFromTimestamp(59000),`is`("119246"))
    }

    @Test
    fun payloadMessageAdapter(){
        val payloadAdapter = Moshi.Builder().add(PayloadMessageAdapter()).build().adapter<Payload>(Payload::class.java)
        val payload = Payload(1,HashMap(),0)
        payload.message["otp"] = "123456"
        assertThat(payloadAdapter.toJson(payload), `is`("{\"id\":1,\"message\":{\"otp\":\"123456\"},\"timestamp\":0}"))

        val parsedPayload = payloadAdapter.fromJson("{\"id\":5,\"message\":{" +
                "\"otp\":\"567890\"," +
                "\"totp-timestamp\":\"98992390\"," +
                "\"Amount\":\"GHS 50.00\"" +
                "},\"timestamp\":6}")

        assertThat(parsedPayload?.id, `is`(5L))
        assertThat(parsedPayload?.timestamp, `is`(6L))
        assertThat(parsedPayload!!.message["otp"], `is`("567890"))
        assertThat(parsedPayload!!.message["totp-timestamp"], `is`("98992390"))
        assertThat(parsedPayload!!.message["Amount"], `is`("GHS 50.00"))
    }
}