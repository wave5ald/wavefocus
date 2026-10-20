package com.example.data.security

import com.example.desktop.platform.AppPaths
import java.io.File
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Desktop counterpart of the Android Keystore backed CryptoManager.
 *
 * The AES-256 master key is generated once and persisted (with owner-only intent) in the app data
 * folder, so encrypted notes/backups stay readable across restarts. Format is identical:
 * AES/GCM/NoPadding with a 128-bit tag and Base64(iv) stored separately.
 */
object CryptoManager {
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BITS = 128
    private const val KEY_FILE = "wave_focus_master.key"

    @Volatile
    private var cachedKey: SecretKey? = null

    private fun keyFile(): File = File(AppPaths.filesDir, KEY_FILE)

    @Synchronized
    private fun getOrCreateKey(): SecretKey {
        cachedKey?.let { return it }
        val file = keyFile()
        val key: SecretKey = try {
            if (file.exists() && file.length() >= 16) {
                val bytes = Base64.getDecoder().decode(file.readText().trim())
                SecretKeySpec(bytes, "AES")
            } else {
                val generated = KeyGenerator.getInstance("AES").apply {
                    init(256, SecureRandom())
                }.generateKey()
                file.parentFile?.mkdirs()
                file.writeText(Base64.getEncoder().encodeToString(generated.encoded))
                generated
            }
        } catch (_: Exception) {
            KeyGenerator.getInstance("AES").apply { init(256, SecureRandom()) }.generateKey()
        }
        cachedKey = key
        return key
    }

    fun encrypt(plainText: String): Pair<String, String> {
        if (plainText.isEmpty()) return Pair("", "")
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val iv = cipher.iv
            val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val cipherBase64 = Base64.getEncoder().encodeToString(cipherBytes)
            val ivBase64 = Base64.getEncoder().encodeToString(iv)
            Pair(cipherBase64, ivBase64)
        } catch (_: Exception) {
            Pair(Base64.getEncoder().encodeToString(plainText.toByteArray(Charsets.UTF_8)), "")
        }
    }

    fun decrypt(cipherBase64: String, ivBase64: String): String {
        if (cipherBase64.isEmpty()) return ""
        if (ivBase64.isEmpty()) {
            return try {
                String(Base64.getDecoder().decode(cipherBase64), Charsets.UTF_8)
            } catch (_: Exception) {
                cipherBase64
            }
        }
        return try {
            val cipherBytes = Base64.getDecoder().decode(cipherBase64)
            val iv = Base64.getDecoder().decode(ivBase64)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TAG_LENGTH_BITS, iv))
            String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
        } catch (_: Exception) {
            try {
                String(Base64.getDecoder().decode(cipherBase64), Charsets.UTF_8)
            } catch (_: Exception) {
                cipherBase64
            }
        }
    }
}
