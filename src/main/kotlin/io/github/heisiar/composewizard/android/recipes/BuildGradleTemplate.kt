package io.github.heisiar.composewizard.android.recipes

fun buildComposeMultiplatformGradle(
  packageName: String,
  moduleName: String,
  enableAndroid: Boolean,
  enableIos: Boolean,
  enableDesktop: Boolean,
  enableWeb: Boolean
): String {
  val plugins = buildString {
    appendLine("plugins {")
    appendLine("    alias(libs.plugins.kotlin.multiplatform)")
    if (enableAndroid) {
      appendLine("    alias(libs.plugins.android.application)")
    }
    appendLine("    alias(libs.plugins.compose.compiler)")
    appendLine("    alias(libs.plugins.compose)")
    appendLine("}")
  }
  
  val kotlinBlock = buildString {
    appendLine("kotlin {")
    
    if (enableAndroid) {
      appendLine("    androidTarget {")
      appendLine("        compilations.all {")
      appendLine("            kotlinOptions {")
      appendLine("                jvmTarget = \"11\"")
      appendLine("            }")
      appendLine("        }")
      appendLine("    }")
      appendLine()
    }
    
    if (enableIos) {
      appendLine("    listOf(")
      appendLine("        iosX64(),")
      appendLine("        iosArm64(),")
      appendLine("        iosSimulatorArm64()")
      appendLine("    ).forEach { iosTarget ->")
      appendLine("        iosTarget.binaries.framework {")
      appendLine("            baseName = \"$moduleName\"")
      appendLine("            isStatic = true")
      appendLine("        }")
      appendLine("    }")
      appendLine()
    }
    
    if (enableDesktop) {
      appendLine("    jvm(\"desktop\")")
      appendLine()
    }
    
    if (enableWeb) {
      appendLine("    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)")
      appendLine("    wasmJs {")
      appendLine("        moduleName = \"$moduleName\"")
      appendLine("        browser {")
      appendLine("            commonWebpackConfig {")
      appendLine("                outputFileName = \"$moduleName.js\"")
      appendLine("            }")
      appendLine("        }")
      appendLine("        binaries.executable()")
      appendLine("    }")
      appendLine()
    }
    
    appendLine("    sourceSets {")
    appendLine("        commonMain.dependencies {")
    appendLine("            implementation(compose.runtime)")
    appendLine("            implementation(compose.foundation)")
    appendLine("            implementation(compose.material3)")
    appendLine("            implementation(compose.ui)")
    appendLine("            implementation(compose.components.resources)")
    appendLine("        }")
    
    if (enableAndroid) {
      appendLine()
      appendLine("        androidMain.dependencies {")
      appendLine("            implementation(compose.preview)")
      appendLine("            implementation(libs.androidx.activity.compose)")
      appendLine("        }")
    }
    
    if (enableDesktop) {
      appendLine()
      appendLine("        val desktopMain by getting {")
      appendLine("            dependencies {")
      appendLine("                implementation(compose.desktop.currentOs)")
      appendLine("            }")
      appendLine("        }")
    }
    
    appendLine("    }")
    appendLine("}")
  }
  
  val androidBlock = if (enableAndroid) {
    """
android {
    namespace = "$packageName"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "$packageName"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    
    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
"""
  } else ""
  
  val desktopBlock = if (enableDesktop) {
    """
compose.desktop {
    application {
        mainClass = "$packageName.MainKt"
    }
}
"""
  } else ""
  
  return """
$plugins

$kotlinBlock

$androidBlock

$desktopBlock
""".trimIndent()
}

