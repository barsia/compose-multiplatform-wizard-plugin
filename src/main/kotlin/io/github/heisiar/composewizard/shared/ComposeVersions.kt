package io.github.heisiar.composewizard.shared

/**
 * Type of library for version resolution and display.
 */
enum class LibraryType(val displayName: String) {
    LIFECYCLE("Lifecycle"),
    MATERIAL3("Material3"),
    MATERIAL3_ADAPTIVE("M3 Adaptive"),
    NAVIGATION("Navigation"),
    NAVIGATION3("Navigation3"),
    NAVIGATION_EVENT("Navigation Event"),
    SAVED_STATE("SavedState"),
    WINDOW("Window"),
    HOT_RELOAD("Hot Reload")
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
    val navigationVersion: String? = null,
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
        LibraryType.NAVIGATION -> navigationVersion
        LibraryType.NAVIGATION3 -> navigation3Version
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
     * Used as the ONLY source when no internet connection is available (offline fallback).
     * When internet is available, versions are fetched from Maven Central instead.
     * 
     * Based on official JetBrains releases:
     * https://github.com/JetBrains/compose-multiplatform/releases
     * 
     * NOTE: All library versions in this map were originally fetched from GitHub tag pages
     * and are hardcoded here to guarantee offline functionality.
     * 
     * To add new versions: add entry here, everything else updates automatically.
     */
    val LIBRARY_BUNDLES = linkedMapOf(
        "1.10.0-beta02" to ComposeLibraryVersions(
            composeVersion = "1.10.0-beta02",
            kotlinVersion = "2.2.21",
            lifecycleVersion = "2.10.0-alpha05",
            material3Version = "1.10.0-alpha05",
            material3AdaptiveVersion = "1.3.0-alpha02",
            navigation3Version = "1.0.0-alpha05",
            navigationEventVersion = "1.0.0-beta02",
            savedStateVersion = "1.4.0-rc01",
            windowVersion = "1.5.0"
        ),
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
            kotlinVersion = "2.2.21",
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
            kotlinVersion = "2.2.21",
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
            kotlinVersion = "2.2.21",
            lifecycleVersion = "2.10.0-alpha01",
            material3Version = "1.10.0-alpha01",
            material3AdaptiveVersion = "1.2.0-alpha06",
            navigation3Version = "1.0.0-alpha01",
            navigationVersion = "1.10.0-alpha01",
            savedStateVersion = "1.4.0-alpha01",
            windowVersion = "1.5.0-alpha01"
        ),
        "1.9.3" to ComposeLibraryVersions(
            composeVersion = "1.9.3",
            kotlinVersion = "2.2.21",
            lifecycleVersion = "2.9.6",
            savedStateVersion = "1.3.6"
        )
    )
    
    /**
     * Hardcoded baseline of stable Compose Multiplatform versions.
     * 
     * ⚠️ AUTOMATICALLY GENERATED from LIBRARY_BUNDLES ⚠️
     * 
     * This list is used as the ONLY source when no internet connection is available.
     * When internet is available, versions are fetched from Maven Central instead.
     * 
     * Usage:
     * - Offline fallback: displayed when Maven Central is unreachable (all versions from LIBRARY_BUNDLES)
     * - Version baseline: LAST_STABLE_VERSION is derived from this list for Maven filtering
     * 
     * The plugin checks Maven once per 24 hours. No version limit or merging with hardcoded.
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
     * Last stable Compose Multiplatform version in LIBRARY_BUNDLES.
     * 
     * This is the STARTING POINT for fetching versions from Maven Central in Release mode.
     * All versions >= this value are fetched and displayed (including alpha/beta/rc after it).
     * 
     * Example:
     * - LIBRARY_BUNDLES = [1.10.0-beta01, 1.10.0-alpha01, 1.9.3, 1.9.2]
     * - LAST_STABLE_VERSION = 1.9.3 (first without alpha/beta/rc)
     * - Maven will fetch: 1.9.3, 1.10.0-alpha01, 1.10.0-beta01, and any newer versions
     * 
     * Filters out dev versions (alpha/beta/rc) to get the true stable baseline.
     */
    val LAST_STABLE_VERSION: String by lazy {
        LIBRARY_BUNDLES.keys.firstOrNull { version ->
            !version.contains("-alpha") && !version.contains("-beta") && !version.contains("-rc")
        } ?: LIBRARY_BUNDLES.keys.first()
    }
    
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
    const val KOTLIN_VERSION_WIZARD = "2.2.21"
    
    /**
     * Hot Reload version.
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

