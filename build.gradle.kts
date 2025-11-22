plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.2.21"
    id("org.jetbrains.intellij.platform") version "2.10.4"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21"
    id("org.jetbrains.kotlinx.kover") version "0.8.3"
}

// Configure Java toolchain for the entire project (required for Jewel)
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

group = "io.github.heisiar"
version = "0.1.0"

// Platform configuration: Build for IDEA 2025.2.5+ or AS 2025.2+
val runIntellijIdea = project.findProperty("runIntellijIdea")?.toString()?.toBoolean() ?: false
val platformType = if (runIntellijIdea) "IC" else "AI"
val platformVersion = if (runIntellijIdea) "2025.2.5" else "2025.2.2.4"

repositories {
    mavenCentral()
    google()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        // Base platform: IDEA 2025.2.5 or AS 2025.2.2.4
        create(platformType, platformVersion)
        
        // Gradle support
        bundledPlugin("com.intellij.gradle")
        bundledPlugin("org.jetbrains.kotlin")
        
        // Android plugin - only for AS
        if (!runIntellijIdea) {
            bundledPlugin("org.jetbrains.android")
        }
        
        // Test framework
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
        
        // Compose UI - provided by platform (compileOnly)
        composeUI()
        
        // Jewel UI library - provided by platform (compileOnly)
        bundledModule("intellij.platform.jewel.foundation")
        bundledModule("intellij.platform.jewel.ui")
        bundledModule("intellij.platform.jewel.ideLafBridge")
        bundledModule("intellij.platform.jewel.markdown.core")
        bundledModule("intellij.platform.jewel.markdown.ideLafBridgeStyling")
        bundledModule("intellij.libraries.compose.foundation.desktop")
        bundledModule("intellij.libraries.skiko")
    }
    
    // All Compose and Jewel provided by platform
    // Plugin size: ~3 MB (no bundled libraries)
    
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
            // IDEA 2025.2.5+ (252) or AS 2025.2+ (252)
            sinceBuild = "252.2.5"
            untilBuild = "262.*"
        }
        
        name = "Compose Multiplatform Wizard"
        
        changeNotes = """
            <h3>0.1.0 - First Public Release</h3>
            <ul>
              <li>First public release</li>
              <li>Create Compose Multiplatform projects for Desktop, Android, iOS, and Web</li>
              <li>Works in IntelliJ IDEA 2025.2.5+ and Android Studio 2025.2+</li>
              <li>Automatic library version resolution from Maven Central</li>
            </ul>
        """.trimIndent()
    }
}

tasks {
    test {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
            showStandardStreams = false
        }
    }

    runIde {
        jvmArgs(
            "-Xmx2048m",
            "-Dfus.internal.test.mode=true",
            "-Didea.is.internal=true"
        )
        autoReload = true
    }
    
    buildPlugin {
        archiveBaseName.set("compose-multiplatform-wizard-plugin")
        archiveVersion.set(project.version.toString())
        // Add suffix based on target platform
        archiveClassifier.set(if (runIntellijIdea) "ij" else "ai")
    }
}

kotlin {
    jvmToolchain(21)
}

kover {
    reports {
        filters {
            excludes {
                classes(
                    "*.ComposableSingletons*",
                    "*\$*",
                    "*.androidstudio.*",
                    "*.idea.*"
                )
            }
        }
    }
}

