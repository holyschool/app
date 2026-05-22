package com.enderplusbayzuiship.edupage2.network

import android.util.Base64
import com.enderplusbayzuiship.edupage2.data.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackendSecurityManager @Inject constructor(
    private val appPreferences: AppPreferences,
    private val backendApi: BackendApi
) {
    private var cachedPublicKey: PublicKey? = null

    suspend fun getPublicKey(): PublicKey? = withContext(Dispatchers.IO) {
        cachedPublicKey?.let { return@withContext it }

        val baseUrl = appPreferences.backendEffectiveUrl
        val apiKey = appPreferences.backendEffectiveKey
        if (baseUrl.isBlank() || apiKey.isBlank()) return@withContext null

        val result = runCatching { backendApi.getPublicKey(baseUrl, apiKey) }.getOrNull()
        if (result == null || !result.ok) return@withContext null

        try {
            val json = JSONObject(result.body)
            val pem = json.getString("publicKey")
            val cleanPem = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("\n", "")
                .trim()
            
            val keyBytes = Base64.decode(cleanPem, Base64.DEFAULT)
            val spec = X509EncodedKeySpec(keyBytes)
            val keyFactory = KeyFactory.getInstance("RSA")
            val key = keyFactory.generatePublic(spec)
            cachedPublicKey = key
            key
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun encryptPassword(password: String): String? = withContext(Dispatchers.IO) {
        val publicKey = getPublicKey() ?: return@withContext null
        try {
            val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
            cipher.init(Cipher.ENCRYPT_MODE, publicKey)
            val encryptedBytes = cipher.doFinal(password.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
