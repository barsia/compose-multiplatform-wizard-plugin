package io.github.heisiar.composewizard.shared

/**
 * Type of library for version resolution and display.
 */
enum class LibraryType(val displayName: String) {
    LIFECYCLE("Lifecycle"),
    MATERIAL3("Material3"),
    MATERIAL3_ADAPTIVE("Material3 Adaptive"),
    NAVIGATION("Navigation3"),
    NAVIGATION_EVENT("Navigation Event"),
    SAVED_STATE("SavedState"),
    WINDOW("Window"),
    HOT_RELOAD("Compose Hot Reload")
}

/**
 * Library versions bundle for specific Compose version.
 * Contains all related library versions that should be used together.
 * 
 * Example mappings:
 * - compose = 1.10.0-beta01
 *   - org.jetbrains.compose.*:*:1.10.0-beta01
 *   - org.jetbrains.compose.material3:material3*:1.10.0-alpha04
 *   - org.jetbrains.compose.material3.adaptive:adaptive-*:1.3.0-alpha01
 *   - org.jetbrains.androidx.lifecycle:lifecycle-*:2.10.0-alpha04
 *   - org.jetbrains.androidx.navigation3:navigation3-*:1.0.0-alpha04
 *   - org.jetbrains.androidx.navigationevent:navigationevent-*:1.0.0-beta01
 *   - org.jetbrains.androidx.savedstate:savedstate*:1.4.0-beta01
 *   - org.jetbrains.androidx.window:window-core:1.5.0-rc01
 */
data class ComposeLibraryVersions(
    val composeVersion: String,
    val kotlinVersion: String,
    
    // AndroidX Libraries
    val lifecycleVersion: String,
    val navigation3Version: String? = null,
    val navigationEventVersion: String? = null,
    val savedStateVersion: String? = null,
    val windowVersion: String? = null,
    
    // Compose Material
    val material3Version: String? = null,
    val material3AdaptiveVersion: String? = null
) {
    /**
     * Get library version by type.
     */
    fun getVersion(type: LibraryType): String? = when (type) {
        LibraryType.LIFECYCLE -> lifecycleVersion
        LibraryType.MATERIAL3 -> material3Version
        LibraryType.MATERIAL3_ADAPTIVE -> material3AdaptiveVersion
        LibraryType.NAVIGATION -> navigation3Version
        LibraryType.NAVIGATION_EVENT -> navigationEventVersion
        LibraryType.SAVED_STATE -> savedStateVersion
        LibraryType.WINDOW -> windowVersion
        LibraryType.HOT_RELOAD -> null
    }
}

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
        "1.10.0-beta01" to ComposeLibraryVersions(
            composeVersion = "1.10.0-beta01",
            kotlinVersion = "2.2.21",
            lifecycleVersion = "2.10.0-alpha04",
            material3Version = "1.10.0-alpha04",
            material3AdaptiveVersion = "1.3.0-alpha01",
            navigation3Version = "1.0.0-alpha04",
            navigationEventVersion = "1.0.0-beta01",
            savedStateVersion = "1.4.0-beta01",
            windowVersion = "1.5.0-rc01"
        ),
        "1.10.0-alpha03" to ComposeLibraryVersions(
            composeVersion = "1.10.0-alpha03",
            kotlinVersion = "2.1.0",
            lifecycleVersion = "2.10.0-alpha03",
            material3Version = "1.10.0-alpha03",
            material3AdaptiveVersion = "1.2.0-beta01",
            navigation3Version = "1.0.0-alpha03",
            navigationEventVersion = "1.0.0-alpha02",
            savedStateVersion = "1.4.0-alpha03",
            windowVersion = "1.5.0-beta01"
        ),
        "1.10.0-alpha02" to ComposeLibraryVersions(
            composeVersion = "1.10.0-alpha02",
            kotlinVersion = "2.1.0",
            lifecycleVersion = "2.10.0-alpha02",
            material3Version = "1.10.0-alpha02",
            material3AdaptiveVersion = "1.2.0-alpha07",
            navigation3Version = "1.0.0-alpha02",
            navigationEventVersion = "1.0.0-alpha01",
            savedStateVersion = "1.4.0-alpha02",
            windowVersion = "1.5.0-alpha02"
        ),
        "1.10.0-alpha01" to ComposeLibraryVersions(
            composeVersion = "1.10.0-alpha01",
            kotlinVersion = "2.1.0",
            lifecycleVersion = "2.10.0-alpha01",
            material3Version = "1.10.0-alpha01",
            material3AdaptiveVersion = "1.2.0-alpha06",
            navigation3Version = "1.0.0-alpha01",
            savedStateVersion = "1.4.0-alpha01",
            windowVersion = "1.5.0-alpha01"
        ),
        "1.9.3" to ComposeLibraryVersions(
            composeVersion = "1.9.3",
            kotlinVersion = "2.1.0",
            lifecycleVersion = "2.9.6",
            savedStateVersion = "1.3.6"
        ),
        "1.9.2" to ComposeLibraryVersions(
            composeVersion = "1.9.2",
            kotlinVersion = "2.1.0",
            lifecycleVersion = "2.9.5",
            material3AdaptiveVersion = "1.2.0"
        ),
        "1.9.1" to ComposeLibraryVersions(
            composeVersion = "1.9.1",
            kotlinVersion = "2.0.21",
            lifecycleVersion = "2.9.5",
            material3Version = "1.9.0",
            navigation3Version = "2.9.1",
            savedStateVersion = "1.3.5"
        ),
        "1.9.0" to ComposeLibraryVersions(
            composeVersion = "1.9.0",
            kotlinVersion = "2.0.21",
            lifecycleVersion = "2.9.4",
            material3Version = "1.9.0-beta06",
            savedStateVersion = "1.3.4",
            windowVersion = "1.4.0"
        ),
        "1.9.0-rc02" to ComposeLibraryVersions(
            composeVersion = "1.9.0-rc02",
            kotlinVersion = "2.0.21",
            lifecycleVersion = "2.9.4-rc01",
            material3Version = "1.9.0-beta05",
            savedStateVersion = "1.3.4-rc01",
            windowVersion = "1.4.0-rc02"
        ),
        "1.9.0-beta03" to ComposeLibraryVersions(
            composeVersion = "1.9.0-beta03",
            kotlinVersion = "2.0.20",
            lifecycleVersion = "2.9.2",
            material3Version = "1.9.0-beta03",
            material3AdaptiveVersion = "1.2.0-alpha05",
            navigation3Version = "2.9.0-beta05",
            savedStateVersion = "1.3.2",
            windowVersion = "1.4.0-beta01"
        ),
        "1.9.0-beta01" to ComposeLibraryVersions(
            composeVersion = "1.9.0-beta01",
            kotlinVersion = "2.0.20",
            lifecycleVersion = "2.9.0",
            material3Version = "1.9.0-alpha04",
            material3AdaptiveVersion = "1.2.0-alpha04",
            navigation3Version = "2.9.0-beta04",
            windowVersion = "1.4.0-alpha09"
        ),
        "1.9.0-alpha03" to ComposeLibraryVersions(
            composeVersion = "1.9.0-alpha03",
            kotlinVersion = "2.0.20",
            lifecycleVersion = "2.9.0",
            material3AdaptiveVersion = "1.2.0-alpha03",
            windowVersion = "1.4.0-alpha08"
        ),
        "1.9.0-alpha02" to ComposeLibraryVersions(
            composeVersion = "1.9.0-alpha02",
            kotlinVersion = "2.0.20",
            lifecycleVersion = "2.9.0",
            material3AdaptiveVersion = "1.2.0-alpha02",
            windowVersion = "1.4.0-alpha07"
        ),
        "1.8.2" to ComposeLibraryVersions(
            composeVersion = "1.8.2",
            kotlinVersion = "2.0.20",
            lifecycleVersion = "2.9.1",
            material3AdaptiveVersion = "1.1.2",
            navigation3Version = "2.9.0-beta03",
            savedStateVersion = "1.3.1"
        )
    )
    
    /**
     * Hardcoded baseline of stable Compose Multiplatform versions.
     * 
     * ⚠️ AUTOMATICALLY GENERATED from LIBRARY_BUNDLES ⚠️
     * 
     * These versions are embedded in the plugin code and serve as:
     * 1. Instant availability (no internet required)
     * 2. Baseline for version filtering (Maven versions newer than first version in this list)
     * 3. Fallback when Maven is unavailable
     * 
     * The plugin will check Maven once per 24 hours for newer versions than DEFAULT_VERSION.
     * Total cached versions limited to 20 (hardcoded + new from Maven).
     * 
     * This list is automatically derived from LIBRARY_BUNDLES keys.
     */
    val STABLE_VERSIONS_HARDCODED: List<String> by lazy {
        LIBRARY_BUNDLES.keys.toList()
        // LIBRARY_BUNDLES already has correct manual semantic order, don't re-sort
    }
    
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
    const val DEFAULT_KOTLIN_VERSION = "2.2.21"
    
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

