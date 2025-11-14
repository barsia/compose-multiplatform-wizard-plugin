package io.github.heisiar.composewizard.generator.xcode

import io.github.heisiar.composewizard.testutils.ProjectFixtures
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class XcodeProjectGeneratorTest {

    private val testConfig = ProjectFixtures.IOS_ONLY_CONFIG

    @Test
    fun `generate replaces all UUIDs in template`() {
        val generator = XcodeProjectGenerator(SecureRandomUUIDGenerator())
        val template = """
            /* Begin PBXFileReference section */
            		ABC123DEF456789ABCDEF012 /* ProjectIos.app */ = {isa = PBXFileReference; explicitFileType = wrapper.application;};
            /* End PBXFileReference section */
            
            /* Begin PBXProject section */
            		DEF456789ABCDEF012345678 /* Project object */ = {
            			isa = PBXProject;
            			mainGroup = ABC123DEF456789ABCDEF012;
            		};
            /* End PBXProject section */
        """.trimIndent()

        val result = generator.generate(ProjectFixtures.toProjectConfig(testConfig), template)

        assertFalse(result.contains("ABC123DEF456789ABCDEF012"), "Original UUID should be replaced")
        assertFalse(result.contains("DEF456789ABCDEF012345678"), "Original UUID should be replaced")
        
        val newUuids = Regex("[0-9A-F]{24}").findAll(result).map { it.value }.toList()
        assertTrue(newUuids.size >= 2, "Should contain at least 2 new UUIDs")
        assertTrue(newUuids.all { it != "ABC123DEF456789ABCDEF012" && it != "DEF456789ABCDEF012345678" })
    }

    @Test
    fun `generate replaces PROJECT_NAME placeholder`() {
        val generator = XcodeProjectGenerator(SecureRandomUUIDGenerator())
        val template = """
            D0C6196ECE92D680CDA86A0C /* ${'$'}PROJECT_NAME$.app */ = {isa = PBXFileReference;};
        """.trimIndent()

        val result = generator.generate(ProjectFixtures.toProjectConfig(testConfig), template)

        assertTrue(result.contains("${testConfig.projectName}.app"), "Should replace PROJECT_NAME with actual name")
        assertFalse(result.contains("\$PROJECT_NAME\$"), "Placeholder should be removed")
    }

    @Test
    fun `generate maintains template structure`() {
        val generator = XcodeProjectGenerator(SecureRandomUUIDGenerator())
        val template = """
            // !${'$'}*UTF8*${'$'}!
            {
            	archiveVersion = 1;
            	classes = {
            	};
            	objectVersion = 77;
            	objects = {
            		ABC123DEF456789ABCDEF012 /* test */ = {isa = Test;};
            	};
            	rootObject = ABC123DEF456789ABCDEF012;
            }
        """.trimIndent()

        val result = generator.generate(ProjectFixtures.toProjectConfig(testConfig), template)

        assertTrue(result.contains("// !\$*UTF8*\$!"), "Should preserve file header")
        assertTrue(result.contains("archiveVersion = 1;"), "Should preserve structure")
        assertTrue(result.contains("objectVersion = 77;"), "Should preserve version")
        assertTrue(result.contains("{isa = Test;}"), "Should preserve object properties")
    }

    @Test
    fun `generate creates unique UUIDs for each occurrence`() {
        val generator = XcodeProjectGenerator(SecureRandomUUIDGenerator())
        val template = """
            UUID1: ABC123DEF456789ABCDEF012
            UUID2: DEF456789ABCDEF012345678
            UUID1 again: ABC123DEF456789ABCDEF012
        """.trimIndent()

        val result = generator.generate(ProjectFixtures.toProjectConfig(testConfig), template)

        val lines = result.lines()
        val uuid1Line1 = lines[0].substringAfter("UUID1: ")
        val uuid1Line3 = lines[2].substringAfter("UUID1 again: ")
        
        assertEquals(uuid1Line1, uuid1Line3, "Same original UUID should map to same new UUID")
        
        val uuid2Line2 = lines[1].substringAfter("UUID2: ")
        assertTrue(uuid1Line1 != uuid2Line2, "Different original UUIDs should map to different new UUIDs")
    }

    @Test
    fun `generate handles template without UUIDs`() {
        val generator = XcodeProjectGenerator(SecureRandomUUIDGenerator())
        val template = """
            Some text without UUIDs
            Just regular content
        """.trimIndent()

        val result = generator.generate(ProjectFixtures.toProjectConfig(testConfig), template)

        assertEquals(template, result, "Should return template unchanged if no UUIDs or placeholders")
    }

    @Test
    fun `generate handles template with multiple PROJECT_NAME occurrences`() {
        val generator = XcodeProjectGenerator(SecureRandomUUIDGenerator())
        val template = """
            Name: ${'$'}PROJECT_NAME${'$'}
            Path: /path/to/${'$'}PROJECT_NAME${'$'}
            Bundle: com.example.${'$'}PROJECT_NAME${'$'}
        """.trimIndent()

        val result = generator.generate(ProjectFixtures.toProjectConfig(testConfig), template)

        val expectedName = testConfig.projectName
        assertTrue(result.contains("Name: $expectedName"))
        assertTrue(result.contains("Path: /path/to/$expectedName"))
        assertTrue(result.contains("Bundle: com.example.$expectedName"))
        assertFalse(result.contains("\$PROJECT_NAME\$"))
    }

    @Test
    fun `generate preserves comments and formatting`() {
        val generator = XcodeProjectGenerator(SecureRandomUUIDGenerator())
        val template = """
            /* Begin PBXFileReference section */
            		ABC123DEF456789ABCDEF012 /* ${'$'}PROJECT_NAME$.app */ = {
            			isa = PBXFileReference;
            			path = ${'$'}PROJECT_NAME$.app;
            		};
            /* End PBXFileReference section */
        """.trimIndent()

        val result = generator.generate(ProjectFixtures.toProjectConfig(testConfig), template)

        assertTrue(result.contains("/* Begin PBXFileReference section */"))
        assertTrue(result.contains("/* End PBXFileReference section */"))
        assertTrue(result.contains("isa = PBXFileReference;"))
        assertTrue(result.lines().any { it.startsWith("\t\t") }, "Should preserve indentation")
    }

    @Test
    fun `generate produces valid Xcode UUIDs`() {
        val generator = XcodeProjectGenerator(SecureRandomUUIDGenerator())
        val template = "UUID: ABC123DEF456789ABCDEF012"

        val result = generator.generate(ProjectFixtures.toProjectConfig(testConfig), template)

        val uuidPattern = Regex("[0-9A-F]{24}")
        val foundUuids = uuidPattern.findAll(result).map { it.value }.toList()
        
        assertTrue(foundUuids.isNotEmpty(), "Should contain at least one UUID")
        foundUuids.forEach { uuid ->
            assertEquals(24, uuid.length, "Each UUID should be 24 characters")
            assertTrue(uuid.all { it in "0123456789ABCDEF" }, "Each UUID should be valid hex")
        }
    }

    @Test
    fun `generate works with real project fixture data`() {
        val generator = XcodeProjectGenerator(SecureRandomUUIDGenerator())
        val template = """
            D0C6196ECE92D680CDA86A0C /* ${'$'}PROJECT_NAME$.app */ = {isa = PBXFileReference; explicitFileType = wrapper.application; includeInIndex = 0; path = ${'$'}PROJECT_NAME$.app; sourceTree = BUILT_PRODUCTS_DIR; };
            1CB7666178EA02572D3F748D /* Exceptions for "iosApp" folder in "iosApp" target */ = {
                isa = PBXFileSystemSynchronizedBuildFileExceptionSet;
                membershipExceptions = (
                    Info.plist,
                );
                target = A2EC10521F904097180A4EF9 /* iosApp */;
            };
        """.trimIndent()

        val result = generator.generate(ProjectFixtures.toProjectConfig(testConfig), template)

        assertFalse(result.contains("D0C6196ECE92D680CDA86A0C"))
        assertFalse(result.contains("1CB7666178EA02572D3F748D"))
        assertFalse(result.contains("A2EC10521F904097180A4EF9"))
        assertTrue(result.contains(testConfig.projectName))
        assertTrue(result.contains("isa = PBXFileReference"))
        assertTrue(result.contains("isa = PBXFileSystemSynchronizedBuildFileExceptionSet"))
    }
}

