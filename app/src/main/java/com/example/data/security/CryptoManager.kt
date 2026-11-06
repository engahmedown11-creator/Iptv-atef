package com.example.data.security

import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoManager {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "atef_iptv_aes256_key"
    private const val GCM_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val CBC_TRANSFORMATION = "AES/CBC/PKCS5Padding"
    private const val GCM_TAG_LENGTH = 128
    private const val GCM_IV_LENGTH = 12

    // 256-bit static secret key for local persistence
    private val MASTER_KEY_256 = byteArrayOf(
        0x41, 0x54, 0x45, 0x46, 0x5F, 0x49, 0x50, 0x54,
        0x56, 0x5F, 0x53, 0x45, 0x43, 0x55, 0x52, 0x45,
        0x32, 0x35, 0x36, 0x42, 0x49, 0x54, 0x4B, 0x45,
        0x59, 0x5F, 0x32, 0x30, 0x32, 0x36, 0x21, 0x23
    )

    private val isAndroidKeyStoreAvailable: Boolean by lazy {
        try {
            val ks = KeyStore.getInstance(ANDROID_KEYSTORE)
            ks.load(null)
            true
        } catch (_: Throwable) {
            false
        }
    }

    private fun getSecretKey(): SecretKey {
        if (isAndroidKeyStoreAvailable) {
            try {
                val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
                val entry = ks.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
                if (entry != null) return entry.secretKey

                val kg = KeyGenerator.getInstance("AES", ANDROID_KEYSTORE)
                val spec = android.security.keystore.KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setRandomizedEncryptionRequired(true)
                    .build()
                kg.init(spec)
                return kg.generateKey()
            } catch (_: Throwable) {
                // Fallback to 256-bit SecretKeySpec
            }
        }
        return SecretKeySpec(MASTER_KEY_256, "AES")
    }

    fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        return try {
            val key = getSecretKey()
            val cipher = Cipher.getInstance(GCM_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            val combined = ByteArray(iv.size + cipherText.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)

            encodeBase64(combined)
        } catch (_: Throwable) {
            // CBC fallback
            fallbackEncrypt(plainText)
        }
    }

    fun decrypt(encryptedText: String): String {
        if (encryptedText.isEmpty()) return ""
        return try {
            val combined = decodeBase64(encryptedText)
            if (combined.size < GCM_IV_LENGTH) return encryptedText

            val iv = ByteArray(GCM_IV_LENGTH)
            val cipherText = ByteArray(combined.size - GCM_IV_LENGTH)
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)
            System.arraycopy(combined, GCM_IV_LENGTH, cipherText, 0, cipherText.size)

            val key = getSecretKey()
            val cipher = Cipher.getInstance(GCM_TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)

            val decrypted = cipher.doFinal(cipherText)
            String(decrypted, Charsets.UTF_8)
        } catch (_: Throwable) {
            fallbackDecrypt(encryptedText)
        }
    }

    private fun fallbackEncrypt(text: String): String {
        return try {
            val spec = SecretKeySpec(MASTER_KEY_256, "AES")
            val cipher = Cipher.getInstance(CBC_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, spec)
            val iv = cipher.iv
            val encrypted = cipher.doFinal(text.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(iv.size + encrypted.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)
            "ENC:" + encodeBase64(combined)
        } catch (_: Throwable) {
            text
        }
    }

    private fun fallbackDecrypt(text: String): String {
        val target = if (text.startsWith("ENC:")) text.removePrefix("ENC:") else text
        return try {
            val combined = decodeBase64(target)
            val iv = ByteArray(16)
            val cipherText = ByteArray(combined.size - 16)
            System.arraycopy(combined, 0, iv, 0, 16)
            System.arraycopy(combined, 16, cipherText, 0, cipherText.size)

            val spec = SecretKeySpec(MASTER_KEY_256, "AES")
            val cipher = Cipher.getInstance(CBC_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, spec, IvParameterSpec(iv))
            String(cipher.doFinal(cipherText), Charsets.UTF_8)
        } catch (_: Throwable) {
            text
        }
    }

    private fun encodeBase64(data: ByteArray): String {
        return try {
            java.util.Base64.getEncoder().encodeToString(data)
        } catch (_: Throwable) {
            android.util.Base64.encodeToString(data, android.util.Base64.NO_WRAP)
        }
    }

    private fun decodeBase64(str: String): ByteArray {
        return try {
            java.util.Base64.getDecoder().decode(str)
        } catch (_: Throwable) {
            android.util.Base64.decode(str, android.util.Base64.NO_WRAP)
        }
    }
}
