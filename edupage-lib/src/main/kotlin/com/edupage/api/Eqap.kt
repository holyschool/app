package com.edupage.api

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import java.util.zip.Deflater

internal object Eqap {

    private val HEX = "0123456789ABCDEF".toCharArray()
    private const val SAFE = "-_.~/"

    fun quote(input: String): String {
        val sb = StringBuilder(input.length * 2)
        for (c in input) {
            if (c in 'A'..'Z' || c in 'a'..'z' || c in '0'..'9' || c in SAFE) {
                sb.append(c)
            } else {
                for (b in c.toString().toByteArray(StandardCharsets.UTF_8)) {
                    val v = b.toInt() and 0xFF
                    sb.append('%').append(HEX[v ushr 4]).append(HEX[v and 0x0F])
                }
            }
        }
        return sb.toString()
    }

    private fun encodeForm(data: Map<String, String>): String =
        data.entries.joinToString("&") { (k, v) -> "${quote(k)}=${quote(v)}" }

    private fun deflateRaw(data: ByteArray): ByteArray {
        val deflater = Deflater(Deflater.DEFAULT_COMPRESSION, true)
        return try {
            deflater.setInput(data)
            deflater.finish()
            val out = ByteArrayOutputStream()
            val buf = ByteArray(4096)
            while (!deflater.finished()) {
                val n = deflater.deflate(buf)
                if (n > 0) out.write(buf, 0, n)
            }
            out.toByteArray()
        } finally {
            deflater.end()
        }
    }

    private fun sha1Hex(value: String): String {
        val digest = MessageDigest.getInstance("SHA-1")
        val bytes = digest.digest(value.toByteArray(StandardCharsets.UTF_8))
        return bytes.joinToString("") { String.format("%02x", it) }
    }

    fun encodeRequestBody(form: Map<String, String>): String {
        val urlEncoded = encodeForm(form)
        val compressed = deflateRaw(urlEncoded.toByteArray(StandardCharsets.UTF_8))
        val b64 = Base64.getEncoder().encodeToString(compressed)
        val eqap = "dz:$b64"
        val eqacs = sha1Hex(eqap)
        return encodeForm(mapOf("eqap" to eqap, "eqacs" to eqacs, "eqaz" to "1"))
    }

    fun decodeResponse(response: String): String {
        val cleaned = response.filter { it != '\t' && it != '\n' && it != '\r' && it != ' ' }
        return when {
            cleaned.startsWith("eqwd:") -> base64ToString(cleaned.substring(5))
            cleaned.startsWith("eqz:") -> base64ToString(cleaned.substring(4))
            else -> cleaned
        }
    }

    private fun base64ToString(data: String): String {
        val cleaned = data.filter { it != '\t' && it != '\n' && it != '\r' && it != ' ' }
        val padded = when (cleaned.length % 4) {
            2 -> "$cleaned=="
            3 -> "$cleaned="
            else -> cleaned
        }
        return String(Base64.getDecoder().decode(padded), StandardCharsets.UTF_8)
    }
}
