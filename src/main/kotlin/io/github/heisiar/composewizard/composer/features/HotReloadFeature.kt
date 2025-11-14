package io.github.heisiar.composewizard.composer.features

import io.github.heisiar.composewizard.composer.PlatformModule
import io.github.heisiar.composewizard.composer.ProjectFeature
import io.github.heisiar.composewizard.generator.ProjectConfig
import io.github.heisiar.composewizard.shared.ComposeVersions

class HotReloadFeature : ProjectFeature {
    
    override val featureId: String = "hot-reload"
    override val displayName: String = "Hot Reload"
    override val defaultEnabled: Boolean = false
    
    override fun apply(targetPath: String, config: ProjectConfig, modules: List<PlatformModule>) {
    }
    
    override fun getRootPlugins(config: ProjectConfig): List<String> {
        return listOf("alias(libs.plugins.composeHotReload)")
    }
    
    override fun getPlugins(config: ProjectConfig): List<String> {
        return listOf("alias(libs.plugins.composeHotReload)")
    }
    
    override fun getLibraryVersions(config: ProjectConfig): Map<String, String> {
        val version = config.hotReloadVersion ?: ComposeVersions.COMPOSE_HOT_RELOAD_VERSION
        return mapOf(
            "composeHotReload" to version
        )
    }
    
    override fun getLibraryDeclarations(config: ProjectConfig): Map<String, String> {
        return emptyMap()
    }
}

