package io.github.heisiar.composewizard.shared

/**
 * Central source of truth for Compose Multiplatform versions.
 * 
 * This file is used by:
 * - Android Studio Template wizard (enum for dropdown)
 * - Compose UI wizard (list for dynamic fetching fallback)
 * 
 * Keep this list updated with latest stable versions from:
 * https://github.com/JetBrains/compose-multiplatform/releases
 */
object ComposeVersions {
    
    /**
     * List of known stable Compose Multiplatform versions.
     * Ordered from newest to oldest.
     * 
     * ⚠️ This is the SINGLE SOURCE OF TRUTH for versions.
     * When updating, the enum will be automatically generated.
     */
    val KNOWN_STABLE_VERSIONS = listOf(
        "1.9.2",
        "1.9.1",
        "1.9.0",
        "1.8.0",
        "1.7.1",
        "1.7.0"
    )
    
    /**
     * Default version to use in wizards.
     */
    val DEFAULT_VERSION: String = KNOWN_STABLE_VERSIONS.first()
    
    /**
     * Enum representation for Android Studio Template API.
     * 
     * ⚠️ LIMITATION: Enum names (v1_9_2) will be displayed in the dropdown as-is.
     * Android Studio's EnumComboProvider uses enum.name, not toString().
     * This is a limitation of the Template API that we cannot override.
     * 
     * To add/remove versions, edit KNOWN_STABLE_VERSIONS above.
     */
    enum class Version(val versionString: String) {
        @Suppress("EnumEntryName")
        v1_9_2("1.9.2"),
        @Suppress("EnumEntryName")
        v1_9_1("1.9.1"),
        @Suppress("EnumEntryName")
        v1_9_0("1.9.0"),
        @Suppress("EnumEntryName")
        v1_8_0("1.8.0"),
        @Suppress("EnumEntryName")
        v1_7_1("1.7.1"),
        @Suppress("EnumEntryName")
        v1_7_0("1.7.0");
        
        override fun toString(): String = versionString
        
        companion object {
            /**
             * Find enum value by version string.
             */
            fun fromString(version: String): Version? {
                return values().find { it.versionString == version }
            }
            
            /**
             * Default version for Android Studio Template wizard.
             * Automatically uses the first version from KNOWN_STABLE_VERSIONS.
             */
            val DEFAULT: Version
                get() = fromString(DEFAULT_VERSION) 
                    ?: error("DEFAULT_VERSION '$DEFAULT_VERSION' not found in Version enum!")
            
            /**
             * Get all available versions as strings.
             * This matches KNOWN_STABLE_VERSIONS.
             */
            fun allVersions(): List<String> = values().map { it.versionString }
        }
    }
    
    init {
        // Compile-time check: ensure enum matches the list
        val enumVersions = Version.allVersions()
        val missingInEnum = KNOWN_STABLE_VERSIONS - enumVersions.toSet()
        val extraInEnum = enumVersions - KNOWN_STABLE_VERSIONS.toSet()
        
        if (missingInEnum.isNotEmpty() || extraInEnum.isNotEmpty()) {
            error("""
                ❌ ComposeVersions.Version enum is out of sync with KNOWN_STABLE_VERSIONS!
                
                Missing in enum: $missingInEnum
                Extra in enum: $extraInEnum
                
                Please update the Version enum to match KNOWN_STABLE_VERSIONS.
            """.trimIndent())
        }
    }
}

