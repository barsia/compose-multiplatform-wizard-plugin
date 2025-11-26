import java.util.*

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

// Platform configuration: Build for IDEA 2025.3+ or AS 2025.2.1+
val runIntellijIdea = project.findProperty("runIntellijIdea")?.toString()?.toBoolean() ?: false
val platformType = if (runIntellijIdea) "IC" else "AI"
val platformVersion = if (runIntellijIdea) "2025.3" else "2025.2.1.7"

// Analytics secrets from local.properties (NOT committed to git)
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { stream -> localProperties.load(stream) }
}

val ga4MeasurementId: String = localProperties.getProperty("ga4.measurement.id") ?: "G-XXXXXXXXXX"
val ga4ApiSecret: String = localProperties.getProperty("ga4.api.secret") ?: "your_api_secret_here"

repositories {
    mavenCentral()
    google()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        // Base platform: IDEA 2025.3 or AS 2025.2.1.7
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
            // IDEA 2025.3+ and AS 2025.2.3+ (both require build 253+)
            sinceBuild = "253"
            untilBuild = "263.*"
        }

        name = "Compose Multiplatform Wizard"
        
        changeNotes = """
            <h3>0.1.0</h3>
            <ul>
              <li>First public release</li>
              <li>Create Compose Multiplatform projects for Desktop, Android, iOS, and Web</li>
              <li>Works in IntelliJ IDEA 2025.3+ and Android Studio (build 253+)</li>
              <li>Automatic library version resolution from Maven Central</li>
            </ul>
        """.trimIndent()
    }
}

tasks {
    // Generate analytics config at build time from local.properties
    val generateAnalyticsConfig by registering {
        val outputDir = layout.buildDirectory.dir("generated/resources")
        outputs.dir(outputDir)
        
        doLast {
            val configFile = outputDir.get().asFile.resolve("analytics.properties")
            configFile.parentFile.mkdirs()
            configFile.writeText("""
                # Auto-generated from local.properties - DO NOT EDIT
                ga4.measurement.id=$ga4MeasurementId
                ga4.api.secret=$ga4ApiSecret
            """.trimIndent())
        }
    }
    
    processResources {
        dependsOn(generateAnalyticsConfig)
        from(layout.buildDirectory.dir("generated/resources"))
    }
    
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
        // Universal distribution for both IDEA and AS
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

