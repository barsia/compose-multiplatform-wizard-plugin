plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.10.2"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0"
}

// Configure Java toolchain for the entire project (required for Jewel)
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

group = "io.github.heisiar"
version = "1.0.0"

// Platform configuration: Build against AS 251.x for maximum compatibility
// Compose Compiler generates code for specific runtime version - use oldest supported
val runIntellijIdea = project.findProperty("runIntellijIdea")?.toString()?.toBoolean() ?: false
val platformType = if (runIntellijIdea) "IC" else "AI"
val platformVersion = if (runIntellijIdea) "2025.2.4" else "2025.2.2.4"  // AS Otter 2025.2.2 Canary 4, IDEA for testing

repositories {
    mavenCentral()
    google()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        // Base platform - Android Studio (for wizard API) or IntelliJ IDEA (for testing)
        create(platformType, platformVersion)
        
        // Gradle support
        bundledPlugin("com.intellij.gradle")
        bundledPlugin("org.jetbrains.kotlin")
        
        // Android plugin - only when building against AS
        if (!runIntellijIdea) {
            bundledPlugin("org.jetbrains.android")
        }
        
        // Test framework
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
        
        // Compose UI - provides compile-time APIs
        composeUI()
        
        // Jewel UI library for SwingBridgeTheme
        bundledModule("intellij.platform.jewel.foundation")
        bundledModule("intellij.platform.jewel.ui")
        bundledModule("intellij.platform.jewel.ideLafBridge")
        bundledModule("intellij.platform.jewel.markdown.core")
        bundledModule("intellij.platform.jewel.markdown.ideLafBridgeStyling")
        bundledModule("intellij.libraries.compose.foundation.desktop")
        bundledModule("intellij.libraries.skiko")
    }
    
    // All Compose and Skiko dependencies are provided by platform via bundledModule() above
    // No need to include them explicitly to avoid ClassLoader conflicts
    
    // Android Studio API - only for compilation when building against AS
    if (!runIntellijIdea) {
        compileOnly("com.android.tools:sdk-common:31.7.2")
        compileOnly("com.android.tools.build:gradle-api:8.7.3")
    }
    
    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.1.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:2.1.0")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    // kotlinx-coroutines-test excluded to use platform-provided version
}

intellijPlatform {
    buildSearchableOptions = false
    
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "252"  // Support AS 2025.2+ (Otter) and IDEA 2025.2+
            untilBuild = "262.*"
        }
        
        name = "Compose Multiplatform Wizard"
        
        changeNotes = """
            <h3>1.0.0</h3>
            <ul>
              <li>Initial release</li>
              <li>Support for IntelliJ IDEA and Android Studio</li>
              <li>Multi-platform project templates (Desktop, Android, iOS, Web)</li>
            </ul>
        """.trimIndent()
    }
}

// Exclude Android Studio-specific code when building for IDEA
sourceSets {
    main {
        java {
            if (runIntellijIdea) {
                exclude("**/androidstudio/**")
            }
        }
        kotlin {
            if (runIntellijIdea) {
                exclude("**/androidstudio/**")
            }
        }
    }
}

tasks {
    // Java toolchain is configured globally above, no need to set compatibility here
    
    test {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
            showStandardStreams = false
        }
    }
    
    runIde {
        // Enable FUS (Feature Usage Statistics) for local testing
        jvmArgs(
            "-Xmx2048m",
            "-Dfus.internal.test.mode=true",  // Enable local FUS event logging (no data sent to JetBrains)
            "-Didea.is.internal=true"          // Enable internal mode (access to FUS Event Log viewer)
        )
        autoReload = true
        
        doFirst {
            val platform = if (runIntellijIdea) "IntelliJ IDEA" else "Android Studio"
            println("==============================================")
            println("  Running plugin in: $platform ($platformType $platformVersion)")
            println("  FUS Test Mode: ENABLED (events logged locally)")
            println("==============================================")
        }
    }
    
    buildPlugin {
        archiveFileName = "compose-multiplatform-wizard-$version.zip"
    }
}

kotlin {
    jvmToolchain(21)
}

