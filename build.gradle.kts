import java.util.Properties

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.2.21"
    id("org.jetbrains.intellij.platform") version "2.14.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21"
    id("org.jetbrains.kotlinx.kover") version "0.8.3"
}

// Configure Java toolchain for the entire project (required for Jewel)
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

group = "io.github.barsia"
version = "0.1.2"

// Platform configuration:
// - default target: IntelliJ IDEA (resolves reliably in clean environments)
// - Android Studio target: provide -PandroidStudioLocalPath=... or set androidStudio.local.path in local.properties
val runIntellijIdea = project.findProperty("runIntellijIdea")?.toString()?.toBoolean() ?: false
val intellijIdeaVersion = project.findProperty("intellijIdeaVersion")?.toString() ?: "2025.3.4"
val pluginVerifierIdeVersion = project.findProperty("pluginVerifierIdeVersion")?.toString()
val pluginVerifierIdeParts = pluginVerifierIdeVersion?.split("-", limit = 2)
// Pinned to the latest 253 AS build that is resolvable by the current intellij-platform-gradle-plugin.
// Newer Panda patch/rc/canary artifacts use codename-based filenames and are not yet resolvable via androidStudio(...).
val androidStudioVersion = project.findProperty("androidStudioVersion")?.toString() ?: "2025.3.1.5"

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { stream -> localProperties.load(stream) }
}

val androidStudioPathFromGradleProperty = project.findProperty("androidStudioLocalPath")?.toString()?.takeIf { it.isNotBlank() }
val androidStudioPathFromLocalProperties = localProperties.getProperty("androidStudio.local.path")?.takeIf { it.isNotBlank() }
val androidStudioLocalPath = androidStudioPathFromGradleProperty ?: androidStudioPathFromLocalProperties
val useAndroidStudio = !runIntellijIdea && androidStudioLocalPath != null

repositories {
    mavenCentral()
    google()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        // Base platform:
        // - default: IntelliJ IDEA
        // - Android Studio only when a local installation path is provided
        if (useAndroidStudio) {
            local(androidStudioLocalPath!!)
        } else {
            intellijIdea(intellijIdeaVersion)
        }
        
        // Gradle support
        bundledPlugin("com.intellij.gradle")
        bundledPlugin("org.jetbrains.kotlin")
        
        // Android plugin - only for AS
        if (useAndroidStudio) {
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

    pluginVerification {
        if (!pluginVerifierIdeVersion.isNullOrBlank()) {
            withGroovyBuilder {
                "ides" {
                    if (pluginVerifierIdeParts != null && pluginVerifierIdeParts.size == 2) {
                        "create"(pluginVerifierIdeParts[0], pluginVerifierIdeParts[1])
                    } else {
                        "create"(pluginVerifierIdeVersion)
                    }
                }
            }
        }
    }
    
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "253.30387.90"
            untilBuild = "263.*"
        }

        name = "Compose Multiplatform Wizard"
        
        changeNotes = """
            <h3>0.1.2</h3>
            <ul>
              <li>Compatibility: IntelliJ Platform 253.30387.90+ (IDEA 2025.3+, Android Studio Panda 2+)</li>
              <li>Added "Add AGENTS.md / CLAUDE.md blueprint" option for AI coding assistants</li>
              <li>Fixed library version resolution for dev Compose versions</li>
              <li>Fixed Refresh button not invalidating library version cache</li>
              <li>Updated dev Maven repository URL</li>
              <li>Hardened remote XML parsing against XXE in version resolution code paths</li>
              <li>Removed plugin analytics and consent flow</li>
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
        // Single distribution for both IDEA and AS
        archiveClassifier.set("")
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
