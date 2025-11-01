plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.10.3"
    id("org.jetbrains.compose") version "1.7.1"
}

group = "io.github.heisiar"
version = "1.0.0"

// Platform configuration: IDEA by default, AS if runAndroidStudio=true
val runAndroidStudio = project.findProperty("runAndroidStudio")?.toString()?.toBoolean() ?: false
val platformType = if (runAndroidStudio) "AI" else "IC"
val platformVersion = if (runAndroidStudio) "2025.2.1.7" else "2025.2.4"

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
        // Base platform - IntelliJ IDEA Community or Android Studio
        create(platformType, platformVersion)
        
        // Gradle support
        bundledPlugin("org.jetbrains.plugins.gradle")
        bundledPlugin("org.jetbrains.kotlin")
        
        // Android plugin (only in AS, but safe to declare)
        if (runAndroidStudio) {
            bundledPlugin("org.jetbrains.android")
        }
        
        // Test framework
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
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
        jvmArgs("-Xmx2048m")
        autoReload = true
        
        doFirst {
            val platform = if (runAndroidStudio) "Android Studio" else "IntelliJ IDEA"
            println("==============================================")
            println("  Running plugin in: $platform ($platformType $platformVersion)")
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

