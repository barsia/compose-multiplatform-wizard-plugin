package io.github.heisiar.composewizard.shared

/**
 * Central source of truth for Compose Multiplatform versions.
 * 
 * This file is used by:
 * - Android Studio Template wizard (StringParameter in dropdown)
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
     * This is the SINGLE SOURCE OF TRUTH for versions.
     * To add/remove versions, simply edit this list.
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
}

