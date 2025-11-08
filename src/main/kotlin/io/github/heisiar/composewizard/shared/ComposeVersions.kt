package io.github.heisiar.composewizard.shared

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
     * List of stable Compose Multiplatform versions (fallback).
     * Ordered from newest to oldest.
     * Top 10 most recent stable versions.
     * 
     * ⚠️ THIS IS THE SINGLE SOURCE OF TRUTH FOR FALLBACK VERSIONS ⚠️
     * 
     * Used by:
     * - ComposeVersionService (fallback when Maven is unavailable)
     * - ComposeVersionCache (fallback when Maven is unavailable)
     * - Template API wizard (fallback in tooltip)
     * - Compose UI wizard (fallback when Maven is unavailable)
     * 
     * To update versions, simply edit this list.
     * Keep it updated with latest stable versions from:
     * https://github.com/JetBrains/compose-multiplatform/releases
     */
    val STABLE_VERSIONS = listOf(
        "1.9.2",
        "1.9.1",
        "1.9.0",
        "1.8.0",
        "1.7.1",
        "1.7.0",
        "1.6.11",
        "1.6.10",
        "1.6.2",
        "1.6.1"
    )
    
    /**
     * Default Compose Multiplatform version to use in wizards.
     */
    val DEFAULT_VERSION: String = STABLE_VERSIONS.first()
    
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
}

