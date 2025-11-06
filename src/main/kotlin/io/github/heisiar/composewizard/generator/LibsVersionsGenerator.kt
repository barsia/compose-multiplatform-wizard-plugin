package io.github.heisiar.composewizard.generator

import io.github.heisiar.composewizard.shared.ComposeVersions

class LibsVersionsGenerator {
    
    fun generate(config: ProjectConfig): String {
        return buildString {
            appendLine("[versions]")
            
            if (config.targetAndroid) {
                appendLine("agp = \"8.11.2\"")
                appendLine("android-compileSdk = \"36\"")
                appendLine("android-minSdk = \"24\"")
                appendLine("android-targetSdk = \"36\"")
                appendLine("androidx-activity = \"1.11.0\"")
                appendLine("androidx-appcompat = \"1.7.1\"")
                appendLine("androidx-core = \"1.17.0\"")
            }
            
            if (!config.targetDesktop || config.targetAndroid || config.targetIOS || config.targetWeb) {
                appendLine("androidx-lifecycle = \"2.9.5\"")
            }
            
            if (config.needsHotReloadPlugin) {
                appendLine("composeHotReload = \"${ComposeVersions.HOT_RELOAD_VERSION}\"")
            }
            
            appendLine("composeMultiplatform = \"${config.composeVersion}\"")
            
            if (config.includeTests) {
                appendLine("junit = \"4.13.2\"")
            }
            
            appendLine("kotlin = \"${config.kotlinVersion}\"")
            
            if (config.targetDesktop) {
                appendLine("kotlinx-coroutines = \"1.10.2\"")
            }
            
            appendLine()
            
            appendLine("[libraries]")
            
            if (config.targetAndroid) {
                appendLine("androidx-core-ktx = { module = \"androidx.core:core-ktx\", version.ref = \"androidx-core\" }")
                appendLine("androidx-appcompat = { module = \"androidx.appcompat:appcompat\", version.ref = \"androidx-appcompat\" }")
                appendLine("androidx-activity-compose = { module = \"androidx.activity:activity-compose\", version.ref = \"androidx-activity\" }")
            }
            
            if (!config.targetDesktop || config.targetAndroid || config.targetIOS || config.targetWeb) {
                appendLine("androidx-lifecycle-viewmodelCompose = { module = \"org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose\", version.ref = \"androidx-lifecycle\" }")
                appendLine("androidx-lifecycle-runtimeCompose = { module = \"org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose\", version.ref = \"androidx-lifecycle\" }")
            }
            
            if (config.targetDesktop) {
                appendLine("kotlinx-coroutinesSwing = { module = \"org.jetbrains.kotlinx:kotlinx-coroutines-swing\", version.ref = \"kotlinx-coroutines\" }")
            }
            
            if (config.includeTests) {
                appendLine("kotlin-test = { module = \"org.jetbrains.kotlin:kotlin-test\", version.ref = \"kotlin\" }")
                appendLine("kotlin-testJunit = { module = \"org.jetbrains.kotlin:kotlin-test-junit\", version.ref = \"kotlin\" }")
                appendLine("junit = { module = \"junit:junit\", version.ref = \"junit\" }")
            }
            
            appendLine()
            appendLine("[plugins]")
            
            if (config.targetAndroid) {
                appendLine("androidApplication = { id = \"com.android.application\", version.ref = \"agp\" }")
                appendLine("androidLibrary = { id = \"com.android.library\", version.ref = \"agp\" }")
            }
            
            if (config.needsHotReloadPlugin) {
                appendLine("composeHotReload = { id = \"org.jetbrains.compose.hot-reload\", version.ref = \"composeHotReload\" }")
            }
            
            appendLine("composeMultiplatform = { id = \"org.jetbrains.compose\", version.ref = \"composeMultiplatform\" }")
            appendLine("composeCompiler = { id = \"org.jetbrains.kotlin.plugin.compose\", version.ref = \"kotlin\" }")
            append("kotlinMultiplatform = { id = \"org.jetbrains.kotlin.multiplatform\", version.ref = \"kotlin\" }")
        }
    }
}

