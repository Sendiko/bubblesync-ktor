package dev.sendiko.bubblesync.features.auth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

internal object PasswordHasher {
    private const val ITERATIONS = 600_000
    private const val SALT_BYTES = 16
    private const val KEY_BITS = 256
    private val random = SecureRandom()

    fun hash(password: String): String {
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val derivedKey = derive(password, salt, ITERATIONS)
        return listOf(
            "pbkdf2-sha256",
            ITERATIONS.toString(),
            Base64.getEncoder().withoutPadding().encodeToString(salt),
            Base64.getEncoder().withoutPadding().encodeToString(derivedKey),
        ).joinToString(":")
    }

    fun verify(password: String, encodedHash: String): Boolean {
        val parts = encodedHash.split(':')
        if (parts.size != 4 || parts[0] != "pbkdf2-sha256") return false
        val iterations = parts[1].toIntOrNull()?.takeIf { it in 100_000..1_000_000 } ?: return false
        val salt = runCatching { Base64.getDecoder().decode(parts[2]) }.getOrNull() ?: return false
        val expected = runCatching { Base64.getDecoder().decode(parts[3]) }.getOrNull() ?: return false
        if (salt.size != SALT_BYTES || expected.size != KEY_BITS / 8) return false
        return MessageDigest.isEqual(expected, derive(password, salt, iterations))
    }

    private fun derive(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}