package com.flozaxx.applock.storage

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Turns a PIN or recovery code into a stored [Credential] and checks entries against it.
 *
 * The secret is stretched with PBKDF2 and the result is keyed with an HMAC key that never leaves
 * the Android Keystore, so a copy of the settings file cannot be brute-forced off the device.
 */
class PinVault(private val seal: (ByteArray) -> ByteArray = ::sealWithKeystore) {

    fun create(secret: String): Credential {
        val salt = ByteArray(SALT_BYTES).also(SecureRandom()::nextBytes)
        return Credential(
            salt = Base64.getEncoder().encodeToString(salt),
            hash = Base64.getEncoder().encodeToString(hash(secret, salt)),
            length = secret.length,
        )
    }

    fun matches(secret: String, credential: Credential): Boolean {
        val salt = Base64.getDecoder().decode(credential.salt)
        val expected = Base64.getDecoder().decode(credential.hash)
        return MessageDigest.isEqual(expected, hash(secret, salt))
    }

    private fun hash(secret: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(secret.toCharArray(), salt, PBKDF2_ITERATIONS, HASH_BITS)
        val stretched = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return seal(stretched)
    }

    companion object {
        private const val SALT_BYTES = 16
        private const val HASH_BITS = 256
        private const val PBKDF2_ITERATIONS = 120_000
        private const val KEY_ALIAS = "pin_seal"
        private const val KEYSTORE = "AndroidKeyStore"

        // Without look-alike characters (0/O, 1/I/L) so the code survives being written on paper.
        private const val RECOVERY_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
        private const val RECOVERY_LENGTH = 12

        fun newRecoveryCode(): String {
            val random = SecureRandom()
            return (1..RECOVERY_LENGTH)
                .map { RECOVERY_ALPHABET[random.nextInt(RECOVERY_ALPHABET.length)] }
                .chunked(4)
                .joinToString("-") { it.joinToString("") }
        }

        fun normalizeRecoveryCode(input: String): String =
            input.uppercase().filter(Char::isLetterOrDigit)

        private fun sealWithKeystore(data: ByteArray): ByteArray {
            val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
            val key = keyStore.getKey(KEY_ALIAS, null) as SecretKey?
                ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, KEYSTORE).run {
                    init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_SIGN).build())
                    generateKey()
                }
            return Mac.getInstance("HmacSHA256").run {
                init(key)
                doFinal(data)
            }
        }
    }
}
