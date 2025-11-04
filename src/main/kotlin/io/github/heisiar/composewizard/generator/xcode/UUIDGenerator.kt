package io.github.heisiar.composewizard.generator.xcode

import java.security.SecureRandom

interface UUIDGenerator {
    /**
     * Generates a unique 24-character identifier in Xcode format
     * @return String of 24 hexadecimal characters in uppercase
     * Example: "A1B2C3D4E5F6A7B8C9D0E1F2"
     */
    fun generate(): String
}

class SecureRandomUUIDGenerator : UUIDGenerator {
    private val secureRandom = SecureRandom()
    
    override fun generate(): String {
        val bytes = ByteArray(12)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString("") { "%02X".format(it) }
    }
}

