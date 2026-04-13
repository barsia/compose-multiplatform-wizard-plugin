package io.github.barsia.composewizard.generator

import io.github.barsia.composewizard.testutils.ProjectFixtures
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReadmeGeneratorTest {

    @Test
    fun `readme contains project name`() {
        val config = ProjectFixtures.toProjectConfig(ProjectFixtures.ANDROID_DESKTOP_CONFIG)
        val readme = ReadmeGenerator.generate(config)
        
        assertContains(readme, "# ${config.projectName}")
    }

    @Test
    fun `readme for Android project contains Android instructions`() {
        val config = ProjectFixtures.toProjectConfig(ProjectFixtures.androidConfig(
            includeTests = false,
            initGit = false,
            projectName = "AndroidApp",
            packageName = "com.example.android"
        ))
        val readme = ReadmeGenerator.generate(config)
        
        assertContains(readme, "Android")
        assertContains(readme, "### Build and Run Android Application")
        assertContains(readme, "./gradlew :composeApp:assembleDebug")
    }

    @Test
    fun `readme for iOS project contains iOS instructions`() {
        val config = ProjectFixtures.toProjectConfig(ProjectFixtures.IOS_ONLY_CONFIG)
        val readme = ReadmeGenerator.generate(config)
        
        assertContains(readme, "iOS")
        assertContains(readme, "### Build and Run iOS Application")
        assertContains(readme, "/iosApp")
        assertContains(readme, "Xcode")
    }

    @Test
    fun `readme for Desktop project contains Desktop instructions`() {
        val config = ProjectFixtures.toProjectConfig(ProjectFixtures.desktopConfig(
            includeTests = false,
            initGit = false,
            projectName = "DesktopApp",
            packageName = "com.example.desktop"
        ))
        val readme = ReadmeGenerator.generate(config)
        
        assertContains(readme, "Desktop (JVM)")
        assertContains(readme, "### Build and Run Desktop (JVM) Application")
        assertContains(readme, "./gradlew :composeApp:run")
    }

    @Test
    fun `readme for Web project contains Web instructions`() {
        val config = ProjectFixtures.toProjectConfig(ProjectFixtures.webConfig(
            includeTests = false,
            initGit = false,
            projectName = "WebApp",
            packageName = "com.example.web"
        ))
        val readme = ReadmeGenerator.generate(config)
        
        assertContains(readme, "Web")
        assertContains(readme, "### Build and Run Web Application")
        assertContains(readme, "./gradlew :composeApp:wasmJsBrowserDevelopmentRun")
        assertContains(readme, "./gradlew :composeApp:jsBrowserDevelopmentRun")
        assertContains(readme, "#compose-web")
        assertContains(readme, "YouTrack")
    }

    @Test
    fun `readme for multiplatform project contains all platform instructions`() {
        val config = ProjectFixtures.toProjectConfig(ProjectFixtures.ALL_PLATFORMS_CONFIG)
        val readme = ReadmeGenerator.generate(config)
        
        assertContains(readme, "Android")
        assertContains(readme, "iOS")
        assertContains(readme, "Desktop (JVM)")
        assertContains(readme, "Web")
        
        assertContains(readme, "### Build and Run Android Application")
        assertContains(readme, "### Build and Run iOS Application")
        assertContains(readme, "### Build and Run Desktop (JVM) Application")
        assertContains(readme, "### Build and Run Web Application")
    }

    @Test
    fun `readme contains project structure description`() {
        val config = ProjectFixtures.toProjectConfig(ProjectFixtures.ANDROID_DESKTOP_CONFIG)
        val readme = ReadmeGenerator.generate(config)
        
        assertContains(readme, "/composeApp")
        assertContains(readme, "commonMain")
        assertContains(readme, "shared across your Compose Multiplatform applications")
    }

    @Test
    fun `readme for iOS project mentions iosApp directory`() {
        val config = ProjectFixtures.toProjectConfig(ProjectFixtures.IOS_ONLY_CONFIG)
        val readme = ReadmeGenerator.generate(config)
        
        assertContains(readme, "/iosApp")
        assertContains(readme, "entry point for your iOS app")
        assertContains(readme, "SwiftUI")
    }

    @Test
    fun `readme for non-iOS project does not mention iosApp`() {
        val config = ProjectFixtures.toProjectConfig(ProjectFixtures.desktopConfig(
            includeTests = false,
            initGit = false,
            projectName = "DesktopApp",
            packageName = "com.example.desktop"
        ))
        val readme = ReadmeGenerator.generate(config)
        
        assertFalse(readme.contains("/iosApp"), "Non-iOS project should not mention iosApp")
    }

    @Test
    fun `readme contains footer with learn more link`() {
        val config = ProjectFixtures.toProjectConfig(ProjectFixtures.ANDROID_DESKTOP_CONFIG)
        val readme = ReadmeGenerator.generate(config)
        
        assertContains(readme, "Learn more about")
        assertContains(readme, "Compose Multiplatform")
        assertContains(readme, "jetbrains.com")
    }

    @Test
    fun `readme platforms are listed in correct order`() {
        val config = ProjectFixtures.toProjectConfig(ProjectFixtures.ALL_PLATFORMS_CONFIG)
        val readme = ReadmeGenerator.generate(config)
        
        val androidIndex = readme.indexOf("Android")
        val iosIndex = readme.indexOf("iOS")
        val desktopIndex = readme.indexOf("Desktop (JVM)")
        val webIndex = readme.indexOf("Web")
        
        assertTrue(androidIndex < iosIndex, "Android should come before iOS")
        assertTrue(iosIndex < desktopIndex, "iOS should come before Desktop")
        assertTrue(desktopIndex < webIndex, "Desktop should come before Web")
    }

    @Test
    fun `readme build instructions are in correct order`() {
        val config = ProjectFixtures.toProjectConfig(ProjectFixtures.ALL_PLATFORMS_CONFIG)
        val readme = ReadmeGenerator.generate(config)
        
        val androidBuildIndex = readme.indexOf("### Build and Run Android Application")
        val iosBuildIndex = readme.indexOf("### Build and Run iOS Application")
        val desktopBuildIndex = readme.indexOf("### Build and Run Desktop (JVM) Application")
        val webBuildIndex = readme.indexOf("### Build and Run Web Application")
        
        assertTrue(androidBuildIndex < iosBuildIndex, "Android build should come before iOS")
        assertTrue(iosBuildIndex < desktopBuildIndex, "iOS build should come before Desktop")
        assertTrue(desktopBuildIndex < webBuildIndex, "Desktop build should come before Web")
    }

    @Test
    fun `readme contains Windows build commands`() {
        val config = ProjectFixtures.toProjectConfig(ProjectFixtures.ANDROID_DESKTOP_CONFIG)
        val readme = ReadmeGenerator.generate(config)
        
        assertContains(readme, "on Windows")
        assertContains(readme, ".\\gradlew.bat")
    }

    @Test
    fun `readme contains macOS Linux build commands`() {
        val config = ProjectFixtures.toProjectConfig(ProjectFixtures.ANDROID_DESKTOP_CONFIG)
        val readme = ReadmeGenerator.generate(config)
        
        assertContains(readme, "on macOS/Linux")
        assertContains(readme, "./gradlew")
    }

    @Test
    fun `readme mentions platform-specific code examples`() {
        val config = ProjectFixtures.toProjectConfig(ProjectFixtures.ALL_PLATFORMS_CONFIG)
        val readme = ReadmeGenerator.generate(config)
        
        assertContains(readme, "iosMain")
        assertContains(readme, "jvmMain")
        assertContains(readme, "CoreCrypto")
    }

    @Test
    fun `readme for Android+Desktop has correct platform list`() {
        val config = ProjectFixtures.toProjectConfig(ProjectFixtures.ANDROID_DESKTOP_CONFIG)
        val readme = ReadmeGenerator.generate(config)
        
        assertContains(readme, "targeting Android, Desktop (JVM)")
    }

    @Test
    fun `readme for single platform project has correct description`() {
        val config = ProjectFixtures.toProjectConfig(ProjectFixtures.androidConfig(
            includeTests = false,
            initGit = false,
            projectName = "AndroidApp",
            packageName = "com.example.android"
        ))
        val readme = ReadmeGenerator.generate(config)
        
        assertContains(readme, "targeting Android")
        assertFalse(readme.contains("targeting Android, iOS"), "Should not list multiple platforms")
    }

    @Test
    fun `readme contains separator line`() {
        val config = ProjectFixtures.toProjectConfig(ProjectFixtures.ANDROID_DESKTOP_CONFIG)
        val readme = ReadmeGenerator.generate(config)
        
        assertContains(readme, "---")
    }
}

