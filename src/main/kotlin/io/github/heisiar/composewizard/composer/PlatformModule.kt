package io.github.heisiar.composewizard.composer

import io.github.heisiar.composewizard.generator.ProjectConfig

/**
 * Represents a platform module (Android, Desktop, iOS, Web).
 * Each module is independent and knows how to configure itself.
 */
interface PlatformModule {
    /**
     * Unique platform identifier (android, desktop, ios, web)
     */
    val platformId: String
    
    /**
     * Display name for UI
     */
    val displayName: String
    
    /**
     * Import statements for build.gradle.kts
     */
    fun getImports(config: ProjectConfig): List<String>
    
    /**
     * Plugin declarations for build.gradle.kts
     */
    fun getPlugins(config: ProjectConfig): List<String>
    
    /**
     * Root-level plugins for root build.gradle.kts
     */
    fun getRootPlugins(config: ProjectConfig): List<String>
    
    /**
     * Target configuration (e.g., androidTarget {}, jvm())
     */
    fun getTargetFragment(config: ProjectConfig): String
    
    /**
     * SourceSet configuration with dependencies
     */
    fun getSourceSetFragment(config: ProjectConfig): String
    
    /**
     * Additional configuration (e.g., android { ... }, compose.desktop { ... })
     */
    fun getConfigFragment(config: ProjectConfig): String
    
    /**
     * Dependencies that go outside sourceSets
     */
    fun getDependencies(config: ProjectConfig): List<String>
    
    /**
     * Copy platform-specific files to target directory
     * @param targetPath Root directory of the project
     * @param config Project configuration
     * @param isMultiplatform True if this is a multiplatform project (>1 platform)
     */
    fun copyFiles(targetPath: String, config: ProjectConfig, isMultiplatform: Boolean)
    
    /**
     * Get README section for this platform
     */
    fun getReadmeSection(config: ProjectConfig): String
    
    /**
     * Get library versions this platform needs in libs.versions.toml
     */
    fun getLibraryVersions(config: ProjectConfig): Map<String, String>
    
    /**
     * Get library declarations for libs.versions.toml
     */
    fun getLibraryDeclarations(config: ProjectConfig): Map<String, String>
}

