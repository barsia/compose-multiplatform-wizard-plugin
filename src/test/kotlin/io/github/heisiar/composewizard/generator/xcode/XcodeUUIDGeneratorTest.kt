package io.github.heisiar.composewizard.generator.xcode

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class XcodeUUIDGeneratorTest {

    @Test
    fun `generate returns 24 character string`() {
        val generator = SecureRandomUUIDGenerator()
        val uuid = generator.generate()
        
        assertEquals(24, uuid.length, "UUID should be 24 characters long")
    }

    @Test
    fun `generate returns uppercase hexadecimal string`() {
        val generator = SecureRandomUUIDGenerator()
        val uuid = generator.generate()
        
        val hexPattern = Regex("^[0-9A-F]{24}$")
        assertTrue(hexPattern.matches(uuid), "UUID should contain only uppercase hexadecimal characters")
    }

    @Test
    fun `generate produces unique UUIDs`() {
        val generator = SecureRandomUUIDGenerator()
        val generatedUuids = (1..1000).map { generator.generate() }.toSet()
        
        assertEquals(1000, generatedUuids.size, "All 1000 generated UUIDs should be unique")
    }

    @Test
    fun `generate does not contain lowercase characters`() {
        val generator = SecureRandomUUIDGenerator()
        val uuid = generator.generate()
        
        assertTrue(uuid.none { it.isLowerCase() }, "UUID should not contain lowercase characters")
    }

    @Test
    fun `generate contains only valid hex characters`() {
        val generator = SecureRandomUUIDGenerator()
        val uuid = generator.generate()
        val validChars = "0123456789ABCDEF".toSet()
        
        assertTrue(uuid.all { it in validChars }, "UUID should contain only 0-9 and A-F")
    }

    @Test
    fun `multiple generators produce different UUIDs`() {
        val generator1 = SecureRandomUUIDGenerator()
        val generator2 = SecureRandomUUIDGenerator()
        
        val uuid1 = generator1.generate()
        val uuid2 = generator2.generate()
        
        assertTrue(uuid1 != uuid2, "Different generator instances should produce different UUIDs")
    }

    @Test
    fun `generated UUIDs are suitable for Xcode format`() {
        val generator = SecureRandomUUIDGenerator()
        val uuid = generator.generate()
        
        assertTrue(uuid.matches(Regex("[0-9A-F]{24}")), "UUID should match Xcode format")
        assertEquals(24, uuid.length)
        assertTrue(uuid.all { it.isUpperCase() || it.isDigit() })
    }
}

