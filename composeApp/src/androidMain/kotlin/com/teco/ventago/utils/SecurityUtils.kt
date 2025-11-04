package com.teco.ventago.utils

import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Throws(NoSuchAlgorithmException::class, InvalidKeyException::class)
fun hmac(algorithm: String?, key: ByteArray?, message: ByteArray?): ByteArray? {
    val mac = Mac.getInstance(algorithm)
    mac.init(SecretKeySpec(key, algorithm))
    return mac.doFinal(message)
}

fun bytesToHex(bytes: ByteArray): String? {
    val hexArray = "0123456789abcdef".toCharArray()
    val hexChars = CharArray(bytes.size * 2)
    var j = 0
    var v: Int
    while (j < bytes.size) {
        v = bytes[j].toInt() and 0xFF
        hexChars[j * 2] = hexArray[v ushr 4]
        hexChars[j * 2 + 1] = hexArray[v and 0x0F]
        j++
    }
    return String(hexChars)
}

actual fun generateHashWithHmac256(message: String): String? {
    return try {
        val hashingAlgorithm = "HmacSHA256" //or "HmacSHA1", "HmacSHA512"
        val bytes: ByteArray? = hmac(
            hashingAlgorithm,
            "fdfda95f–3eed–463e–be85–326072e6d1e0".toByteArray(),
            message.toByteArray()
        )
        bytesToHex(bytes!!)
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    }
}