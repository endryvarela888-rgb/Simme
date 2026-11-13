package com.simmer.app

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Keeps session tokens encrypted with a non-exportable Android Keystore key. */
object SessionStore {
    private const val alias = "simmer.session.aes"
    private const val prefs = "simmer_secure_session"

    fun loadAccess(context: Context): String? = decrypt(context.getSharedPreferences(prefs, Context.MODE_PRIVATE).getString("access", null))
    fun loadRefresh(context: Context): String? = decrypt(context.getSharedPreferences(prefs, Context.MODE_PRIVATE).getString("refresh", null))

    fun save(context: Context, access: String, refresh: String?) {
        val editor = context.getSharedPreferences(prefs, Context.MODE_PRIVATE).edit().putString("access", encrypt(access))
        if (!refresh.isNullOrBlank()) editor.putString("refresh", encrypt(refresh)) else editor.remove("refresh")
        editor.apply()
    }

    fun clear(context: Context) = context.getSharedPreferences(prefs, Context.MODE_PRIVATE).edit().clear().apply()

    private fun secretKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true).build())
            generateKey()
        }
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
    }

    private fun decrypt(value: String?): String? = runCatching {
        if (value.isNullOrBlank()) return null
        val bytes = Base64.decode(value, Base64.NO_WRAP)
        val iv = bytes.copyOfRange(0, 12)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
        String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8)
    }.getOrNull()
}
