package io.github.barsia.composewizard.generator

class BuildGradleGenerator {
    
    fun generateRootBuildGradle(config: ProjectConfig): String {
        return buildString {
            appendLine("plugins {")
            appendLine("    // this is necessary to avoid the plugins to be loaded multiple times")
            appendLine("    // in each subproject's classloader")
            if (config.targetAndroid) {
                appendLine("    alias(libs.plugins.androidApplication) apply false")
                appendLine("    alias(libs.plugins.androidLibrary) apply false")
            }
            if (config.targetDesktop) {
                appendLine("    alias(libs.plugins.composeHotReload) apply false")
            }
            appendLine("    alias(libs.plugins.composeMultiplatform) apply false")
            appendLine("    alias(libs.plugins.composeCompiler) apply false")
            appendLine("    alias(libs.plugins.kotlinMultiplatform) apply false")
            append("}")
        }
    }
    
    fun generateComposeAppBuildGradle(config: ProjectConfig): String {
        return buildString {
            val imports = mutableListOf<String>()
            
            // Add imports based on actual targets (in specific order for consistency)
            if (config.targetDesktop) {
                imports.add("import org.jetbrains.compose.desktop.application.dsl.TargetFormat")
            }
            
            if (config.targetWeb) {
                imports.add("import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl")
            }
            
            if (config.targetAndroid) {
                imports.add("import org.jetbrains.kotlin.gradle.dsl.JvmTarget")
            }
            
            if (imports.isNotEmpty()) {
                imports.forEach { appendLine(it) }
                appendLine()
            }
            
            appendLine("plugins {")
            appendLine("    alias(libs.plugins.kotlinMultiplatform)")
            if (config.targetAndroid) {
                appendLine("    alias(libs.plugins.androidApplication)")
            }
            appendLine("    alias(libs.plugins.composeMultiplatform)")
            appendLine("    alias(libs.plugins.composeCompiler)")
            if (config.targetDesktop) {
                appendLine("    alias(libs.plugins.composeHotReload)")
            }
            appendLine("}")
            appendLine()
            
            appendLine("kotlin {")
            
            if (config.targetAndroid) {
                appendLine("    androidTarget {")
                appendLine("        compilerOptions {")
                appendLine("            jvmTarget.set(JvmTarget.JVM_11)")
                appendLine("        }")
                appendLine("    }")
            }

            if (config.targetIOS) {
                // Add empty line before iOS targets for multiplatform projects
                if (config.targetAndroid) {
                    appendLine()
                }
                appendLine("    listOf(")
                appendLine("        iosArm64(),")
                appendLine("        iosSimulatorArm64()")
                appendLine("    ).forEach { iosTarget ->")
                appendLine("        iosTarget.binaries.framework {")
                appendLine("            baseName = \"ComposeApp\"")
                appendLine("            isStatic = true")
                appendLine("        }")
                appendLine("    }")
            }
            
            if (config.targetDesktop) {
                // Add empty line before Desktop for multiplatform projects
                if (config.targetAndroid || config.targetIOS) {
                    appendLine()
                }
                appendLine("    jvm()")
            }
            
            if (config.targetWeb) {
                // Add empty line before Web targets for multiplatform projects
                if (config.targetAndroid || config.targetDesktop || config.targetIOS) {
                    appendLine()
                }
                appendLine("    js {")
                appendLine("        browser()")
                appendLine("        binaries.executable()")
                appendLine("    }")
                appendLine()
                appendLine("    @OptIn(ExperimentalWasmDsl::class)")
                appendLine("    wasmJs {")
                appendLine("        browser()")
                appendLine("        binaries.executable()")
                appendLine("    }")
            }
            
            appendLine()  // Always use empty line before sourceSets
            appendLine("    sourceSets {")
            
            // For any project with Android: androidMain comes first (both Android-only and multiplatform)
            if (config.targetAndroid) {
                appendLine("        androidMain.dependencies {")
                appendLine("            implementation(compose.preview)")
                appendLine("            implementation(libs.androidx.activity.compose)")
                appendLine("        }")
            }
            
            appendLine("        commonMain.dependencies {")
            appendLine("            implementation(compose.runtime)")
            appendLine("            implementation(compose.foundation)")
            appendLine("            implementation(compose.material3)")
            appendLine("            implementation(compose.ui)")
            appendLine("            implementation(compose.components.resources)")
            appendLine("            implementation(compose.components.uiToolingPreview)")
            appendLine("            implementation(libs.androidx.lifecycle.viewmodelCompose)")
            appendLine("            implementation(libs.androidx.lifecycle.runtimeCompose)")
            appendLine("        }")
            
            if (config.includeTests) {
                appendLine("        commonTest.dependencies {")
                appendLine("            implementation(libs.kotlin.test)")
                appendLine("        }")
            }
            
            if (config.targetDesktop) {
                appendLine("        jvmMain.dependencies {")
                appendLine("            implementation(compose.desktop.currentOs)")
                appendLine("            implementation(libs.kotlinx.coroutinesSwing)")
                appendLine("        }")
            }
            
            appendLine("    }")
            appendLine("}")
            
            
            if (config.targetAndroid) {
                appendLine()
                appendLine("android {")
                appendLine("    namespace = \"${config.projectId}\"")
                appendLine("    compileSdk = libs.versions.android.compileSdk.get().toInt()")
                appendLine()
                appendLine("    defaultConfig {")
                appendLine("        applicationId = \"${config.projectId}\"")
                appendLine("        minSdk = libs.versions.android.minSdk.get().toInt()")
                appendLine("        targetSdk = libs.versions.android.targetSdk.get().toInt()")
                appendLine("        versionCode = 1")
                appendLine("        versionName = \"1.0\"")
                appendLine("    }")
                appendLine("    packaging {")
                appendLine("        resources {")
                appendLine("            excludes += \"/META-INF/{AL2.0,LGPL2.1}\"")
                appendLine("        }")
                appendLine("    }")
                appendLine("    buildTypes {")
                appendLine("        getByName(\"release\") {")
                appendLine("            isMinifyEnabled = false")
                appendLine("        }")
                appendLine("    }")
                appendLine("    compileOptions {")
                appendLine("        sourceCompatibility = JavaVersion.VERSION_11")
                appendLine("        targetCompatibility = JavaVersion.VERSION_11")
                appendLine("    }")
                appendLine("}")
                appendLine()
                appendLine("dependencies {")
                appendLine("    debugImplementation(compose.uiTooling)")
                appendLine("}")
            }
            
            if (config.targetDesktop) {
                appendLine()
                appendLine("compose.desktop {")
                appendLine("    application {")
                appendLine("        mainClass = \"${config.projectId}.MainKt\"")
                appendLine()
                appendLine("        nativeDistributions {")
                appendLine("            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)")
                appendLine("            packageName = \"${config.projectId}\"")
                appendLine("            packageVersion = \"1.0.0\"")
                appendLine("        }")
                appendLine("    }")
                appendLine("}")
            }
        }
    }
}

