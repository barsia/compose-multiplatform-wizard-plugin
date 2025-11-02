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
     * Default version to use in wizards.
     */
    val DEFAULT_VERSION: String = STABLE_VERSIONS.first()
}

