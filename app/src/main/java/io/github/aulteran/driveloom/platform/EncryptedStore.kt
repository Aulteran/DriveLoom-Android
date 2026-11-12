package io.github.aulteran.driveloom.platform

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import io.github.aulteran.driveloom.core.session.KeyValueStore
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * The app's only persistent storage: the server address, the session cookie
 * and the theme choice.
 *
 * Values are encrypted with AES-256-GCM under a key that lives in the Android
 * Keystore and cannot be exported, then kept in private SharedPreferences.
 * Each value is bound to its name, so one stored value cannot be swapped for
 * another. If a value cannot be decrypted (for example the key was lost when
 * the device lock changed), it is dropped and the person signs in again.
 * The file is excluded from cloud backup and device transfer.
 */
class EncryptedStore(context: Context) : KeyValueStore {
    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    private val cache = HashMap<String, String?>()

    @Synchronized
    override fun get(key: String): String? {
        if (cache.containsKey(key)) return cache[key]
        val value = prefs.getString(key, null)?.let { decrypt(key, it) }
        if (value == null && prefs.contains(key)) prefs.edit().remove(key).apply()
        cache[key] = value
        return value
    }

    @Synchronized
    override fun put(key: String, value: String?) {
        cache[key] = value
        val stored = value?.let { encrypt(key, it) }
        prefs.edit().apply { if (stored == null) remove(key) else putString(key, stored) }.apply()
    }

    private fun encrypt(name: String, value: String): String? = try {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        cipher.updateAAD(name.toByteArray(Charsets.UTF_8))
        val sealed = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        Base64.encodeToString(cipher.iv + sealed, Base64.NO_WRAP)
    } catch (_: GeneralSecurityException) {
        // Without a working keystore nothing is written: the value lives for this run only.
        null
    }

    private fun decrypt(name: String, stored: String): String? = try {
        val bytes = Base64.decode(stored, Base64.NO_WRAP)
        if (bytes.size <= IV_BYTES) {
            null
        } else {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, bytes, 0, IV_BYTES))
            cipher.updateAAD(name.toByteArray(Charsets.UTF_8))
            cipher.doFinal(bytes, IV_BYTES, bytes.size - IV_BYTES).toString(Charsets.UTF_8)
        }
    } catch (_: GeneralSecurityException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val FILE = "session"
        const val KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "session-store-v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
    }
}
