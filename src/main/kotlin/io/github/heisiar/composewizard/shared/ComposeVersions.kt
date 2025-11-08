package io.github.heisiar.composewizard.shared

/**
 * Library versions bundle for specific Compose version.
 * Contains all related library versions that should be used together.
 */
data class ComposeLibraryVersions(
    val composeVersion: String,
    val kotlinVersion: String = "2.2.20",
    val lifecycleVersion: String? = null,
    val material3Version: String? = null,
    val navigationVersion: String? = null
)

/**
 * Central source of truth for Compose Multiplatform and Kotlin versions.
 * 
 * This file is used by:
 * - Android Studio Template wizard (StringParameter in dropdown)
 * - Compose UI wizard (list for dynamic fetching fallback)
 * - Project generation (versions in libs.versions.toml)
 * 
 * Keep this list updated with latest stable versions from:
 * https://github.com/JetBrains/compose-multiplatform/releases
 * https://kotlinlang.org/docs/releases.html
 */
object ComposeVersions {
    
    /**
     * Predefined library version bundles for known Compose versions.
     * 
     * ⚠️ SINGLE SOURCE OF TRUTH ⚠️
     * 
     * This is the ONLY place where versions need to be defined.
     * All other lists (STABLE_VERSIONS_HARDCODED, etc.) are automatically derived.
     * 
     * Used as fallback when GitHub API is unavailable or version resolution fails.
     * Based on official JetBrains releases:
     * https://github.com/JetBrains/compose-multiplatform/releases
     * 
     * To add new versions: add entry here, everything else updates automatically.
     */
    val LIBRARY_BUNDLES = linkedMapOf(
        "1.10.0-beta02" to ComposeLibraryVersions(
            composeVersion = "1.10.0-beta02",
            kotlinVersion = "2.2.21",
            lifecycleVersion = "2.10.0-alpha04"
        ),
        "1.10.0-beta01" to ComposeLibraryVersions(
            composeVersion = "1.10.0-beta01",
            kotlinVersion = "2.2.21",
            lifecycleVersion = "2.10.0-alpha04"
        ),
        "1.10.0-alpha03" to ComposeLibraryVersions(
            composeVersion = "1.10.0-alpha03",
            kotlinVersion = "2.1.0",
            lifecycleVersion = "2.10.0-alpha03"
        ),
        "1.10.0-alpha02" to ComposeLibraryVersions(
            composeVersion = "1.10.0-alpha02",
            kotlinVersion = "2.1.0",
            lifecycleVersion = "2.10.0-alpha02"
        ),
        "1.10.0-alpha01" to ComposeLibraryVersions(
            composeVersion = "1.10.0-alpha01",
            kotlinVersion = "2.1.0",
            lifecycleVersion = "2.10.0-alpha01"
        ),
        "1.9.3" to ComposeLibraryVersions(
            composeVersion = "1.9.3",
            kotlinVersion = "2.1.0",
            lifecycleVersion = "2.9.6"
        ),
        "1.9.2" to ComposeLibraryVersions(
            composeVersion = "1.9.2",
            kotlinVersion = "2.1.0",
            lifecycleVersion = "2.9.5"
        ),
        "1.9.1" to ComposeLibraryVersions(
            composeVersion = "1.9.1",
            kotlinVersion = "2.0.21",
            lifecycleVersion = "2.9.4"
        ),
        "1.9.0" to ComposeLibraryVersions(
            composeVersion = "1.9.0",
            kotlinVersion = "2.0.21",
            lifecycleVersion = "2.9.3"
        ),
        "1.8.0" to ComposeLibraryVersions(
            composeVersion = "1.8.0",
            kotlinVersion = "2.0.20",
            lifecycleVersion = "2.9.0"
        ),
        "1.7.1" to ComposeLibraryVersions(
            composeVersion = "1.7.1",
            kotlinVersion = "2.0.20",
            lifecycleVersion = "2.8.7"
        ),
        "1.7.0" to ComposeLibraryVersions(
            composeVersion = "1.7.0",
            kotlinVersion = "2.0.20",
            lifecycleVersion = "2.8.5"
        ),
        "1.6.11" to ComposeLibraryVersions(
            composeVersion = "1.6.11",
            kotlinVersion = "1.9.24",
            lifecycleVersion = "2.8.2"
        ),
        "1.6.10" to ComposeLibraryVersions(
            composeVersion = "1.6.10",
            kotlinVersion = "1.9.23",
            lifecycleVersion = "2.8.0"
        ),
        "1.6.2" to ComposeLibraryVersions(
            composeVersion = "1.6.2",
            kotlinVersion = "1.9.23",
            lifecycleVersion = "2.7.4"
        ),
        "1.6.1" to ComposeLibraryVersions(
            composeVersion = "1.6.1",
            kotlinVersion = "1.9.22",
            lifecycleVersion = "2.7.3"
        ),
        "1.6.0" to ComposeLibraryVersions(
            composeVersion = "1.6.0",
            kotlinVersion = "1.9.21",
            lifecycleVersion = "2.7.0"
        ),
        "1.5.12" to ComposeLibraryVersions(
            composeVersion = "1.5.12",
            kotlinVersion = "1.9.22",
            lifecycleVersion = "2.6.2"
        ),
        "1.5.11" to ComposeLibraryVersions(
            composeVersion = "1.5.11",
            kotlinVersion = "1.9.21",
            lifecycleVersion = "2.6.2"
        ),
        "1.5.10" to ComposeLibraryVersions(
            composeVersion = "1.5.10",
            kotlinVersion = "1.9.20",
            lifecycleVersion = "2.6.1"
        )
    )
    
    /**
     * Hardcoded baseline of stable Compose Multiplatform versions.
     * 
     * ⚠️ AUTOMATICALLY GENERATED from LIBRARY_BUNDLES ⚠️
     * 
     * These versions are embedded in the plugin code and serve as:
     * 1. Instant availability (no internet required)
     * 2. Baseline for incremental updates
     * 3. Fallback when Maven is unavailable
     * 
     * The plugin will check Maven once per 24 hours for newer versions and add them incrementally.
     * Total cached versions limited to 20 (hardcoded + new).
     * 
     * This list is automatically derived from LIBRARY_BUNDLES keys.
     */
    val STABLE_VERSIONS_HARDCODED: List<String> by lazy {
        LIBRARY_BUNDLES.keys.toList()
        // LIBRARY_BUNDLES already has correct manual semantic order, don't re-sort
    }
    
    /**
     * Fallback list for compatibility with existing code.
     * Points to hardcoded versions.
     */
    val STABLE_VERSIONS = STABLE_VERSIONS_HARDCODED
    
    /**
     * Default Compose Multiplatform version to use in wizards.
     * Always returns the first (newest) version from LIBRARY_BUNDLES.
     */
    val DEFAULT_VERSION: String
        get() = LIBRARY_BUNDLES.keys.firstOrNull() ?: error("LIBRARY_BUNDLES must not be empty")
    
    /**
     * Default Kotlin version compatible with Compose Multiplatform.
     * 
     * Version compatibility:
     * - Compose 1.9.x requires Kotlin 2.0+
     * - Kotlin 2.2.x is the latest stable version
     * 
     * Update this when updating DEFAULT_VERSION to ensure compatibility.
     * See: https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-compatibility-and-versioning.html
     */
    const val DEFAULT_KOTLIN_VERSION = "2.2.20"
    
    /**
     * Default AndroidX Lifecycle version for Compose Multiplatform.
     * See: https://maven.pkg.jetbrains.space/public/p/compose/dev/org/jetbrains/androidx/lifecycle/
     */
    const val DEFAULT_ANDROIDX_LIFECYCLE_VERSION = "2.9.5"
    
    /**
     * Compose Hot Reload version.
     * Used only for Compose versions < 1.10.0-beta01 and Desktop projects.
     * Starting from 1.10.0-beta01, Hot Reload is built into Compose.
     */
    const val COMPOSE_HOT_RELOAD_VERSION = "1.0.0-rc02"
    
    /**
     * Get library versions bundle for given Compose version.
     * Returns null if no predefined bundle exists.
     */
    fun getLibraryBundle(composeVersion: String): ComposeLibraryVersions? {
        return LIBRARY_BUNDLES[composeVersion]
    }
}

