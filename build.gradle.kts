plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.10.3"
    id("org.jetbrains.compose") version "1.7.1"
}

group = "io.github.heisiar"
version = "1.0.0"

// Platform configuration: AS by default (needed for wizard API), IDEA for testing
val runIntellijIdea = project.findProperty("runIntellijIdea")?.toString()?.toBoolean() ?: false
val platformType = if (runIntellijIdea) "IC" else "AI"
val platformVersion = if (runIntellijIdea) "2025.2.4" else "2025.2.1.7"

repositories {
    mavenCentral()
    google()
    intellijPlatform {
        defaultRepositories()
    }
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    intellijPlatform {
        // Base platform - Android Studio (for wizard API) or IntelliJ IDEA (for testing)
        create(platformType, platformVersion)
        
        // Gradle support
        bundledPlugin("org.jetbrains.plugins.gradle")
        bundledPlugin("org.jetbrains.kotlin")
        
        // Android plugin - needed for AS wizard API
        if (!runIntellijIdea) {
        bundledPlugin("org.jetbrains.android")
        }
        
        // Test framework
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
    }
    
    // Android Studio API - only for compilation, available at runtime in AS
    // These are needed for AS wizard template integration
    if (!runIntellijIdea) {
        compileOnly("com.android.tools:sdk-common:31.7.2")
        compileOnly("com.android.tools.build:gradle-api:8.7.3")
        // Template API is part of Android Studio, available at runtime
        // We mark it as compileOnly because it's provided by AS
    }
    
    // Compose Multiplatform for wizard UI
    implementation(compose.desktop.macos_arm64)
    implementation(compose.desktop.macos_x64)
    implementation(compose.desktop.linux_x64)
    implementation(compose.desktop.windows_x64)
    implementation(compose.material3)
    implementation(compose.foundation)
    implementation(compose.ui)
    implementation(compose.runtime)
    
    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.1.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:2.1.0")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}

intellijPlatform {
    buildSearchableOptions = false
    
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "242"
            untilBuild = "253.*"
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
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
    
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

