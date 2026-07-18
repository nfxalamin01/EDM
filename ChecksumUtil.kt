package com.edm.downloadmanager.util

import java.io.File
import java.security.MessageDigest

object ChecksumUtil {

    fun sha256(file: File): String = digest(file, "SHA-256")
    fun md5(file: File): String = digest(file, "MD5")

    private fun digest(file: File, algorithm: String): String {
        val md = MessageDigest.getInstance(algorithm)
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                md.update(buffer, 0, read)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    /** Verifies a downloaded file against an expected checksum, if the server/user provided one. */
    fun verify(file: File, expected: String?): Boolean {
        if (expected.isNullOrBlank()) return true // nothing to verify against
        val actual = when (expected.length) {
            32 -> md5(file)
            64 -> sha256(file)
            else -> return true
        }
        return actual.equals(expected, ignoreCase = true)
    }
}
