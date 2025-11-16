package io.github.heisiar.composewizard.composer

import io.github.heisiar.composewizard.generator.ProjectConfig

/**
 * Represents an optional project feature (tests, git, hot-reload).
 * Features are applied after the base project is generated.
 */
interface ProjectFeature {
    /**
     * Unique feature identifier
     */
    val featureId: String
    
    /**
     * Display name for UI
     */
    val displayName: String
    
    /**
     * Whether this feature is enabled by default
     */
    val defaultEnabled: Boolean
        get() = false
    
    /**
     * Apply this feature to the project
     * @param targetPath Root directory of the project
     * @param config Project configuration
     * @param modules List of platform modules in the project
     */
    fun apply(targetPath: String, config: ProjectConfig, modules: List<PlatformModule>)
    
    /**
     * Additional plugins needed for root build.gradle.kts
     */
    fun getRootPlugins(config: ProjectConfig): List<String> = emptyList()
    
    /**
     * Additional plugins for composeApp build.gradle.kts
     */
    fun getPlugins(config: ProjectConfig): List<String> = emptyList()
    
    /**
     * Additional sourceSets fragments
     */
    fun getSourceSetFragments(config: ProjectConfig): List<String> = emptyList()
    
    /**
     * Library versions for libs.versions.toml
     */
    fun getLibraryVersions(config: ProjectConfig): Map<String, String> = emptyMap()
    
    /**
     * Library declarations for libs.versions.toml
     */
    fun getLibraryDeclarations(config: ProjectConfig): Map<String, String> = emptyMap()
}






