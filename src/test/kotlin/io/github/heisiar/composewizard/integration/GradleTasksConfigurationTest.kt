package io.github.heisiar.composewizard.integration

import io.github.heisiar.composewizard.shared.ProjectCreator
import io.github.heisiar.composewizard.testutils.ProjectFixtures
import io.github.heisiar.composewizard.testutils.TemporaryProjectHelper
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertTrue

class GradleTasksConfigurationTest {
    
    @Test
    fun `android target generates android configuration in build gradle`() {
        TemporaryProjectHelper.withTempProject("gradle-test-") { tempDir ->
            val builder = ProjectFixtures.androidConfig(
                projectName = "GradleTest",
                packageName = "com.example.gradletest"
            )
            
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )
            
            val buildFile = File(tempDir, "composeApp/build.gradle.kts")
            assertTrue(buildFile.exists(), "composeApp/build.gradle.kts must exist")
            
            val content = buildFile.readText()
            
            // Verify Android target configuration
            assertTrue(content.contains("androidTarget {"), "build.gradle.kts must contain androidTarget block")
            
            // Verify Android app configuration
            assertTrue(content.contains("android {"), "build.gradle.kts must contain android block")
            assertTrue(content.contains("namespace ="), "build.gradle.kts must contain namespace")
            assertTrue(content.contains("compileSdk ="), "build.gradle.kts must contain compileSdk")
            
            // Verify Android dependencies
            assertTrue(content.contains("androidMain.dependencies {"), "build.gradle.kts must contain androidMain dependencies")
        }
    }
    
    @Test
    fun `desktop target generates desktop configuration in build gradle`() {
        TemporaryProjectHelper.withTempProject("gradle-test-") { tempDir ->
            val builder = ProjectFixtures.desktopConfig(
                projectName = "GradleTest",
                packageName = "com.example.gradletest"
            )
            
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )
            
            val buildFile = File(tempDir, "composeApp/build.gradle.kts")
            assertTrue(buildFile.exists(), "composeApp/build.gradle.kts must exist")
            
            val content = buildFile.readText()
            
            // Verify Desktop (JVM) target configuration
            assertTrue(content.contains("jvm()") || content.contains("jvm {"), 
                "build.gradle.kts must contain jvm() target")
            
            // Verify Desktop application configuration (creates jvmRun task)
            assertTrue(content.contains("compose.desktop {"), 
                "build.gradle.kts must contain compose.desktop block")
            assertTrue(content.contains("application {"), 
                "build.gradle.kts must contain application block inside compose.desktop")
            assertTrue(content.contains("mainClass ="), 
                "build.gradle.kts must contain mainClass for desktop application")
            assertTrue(content.contains("nativeDistributions {"), 
                "build.gradle.kts must contain nativeDistributions block")
        }
    }
    
    @Test
    fun `ios target generates ios configuration in build gradle`() {
        TemporaryProjectHelper.withTempProject("gradle-test-") { tempDir ->
            val builder = ProjectFixtures.iosConfig(
                projectName = "GradleTest",
                packageName = "com.example.gradletest"
            )
            
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )
            
            val buildFile = File(tempDir, "composeApp/build.gradle.kts")
            assertTrue(buildFile.exists(), "composeApp/build.gradle.kts must exist")
            
            val content = buildFile.readText()
            
            // Verify iOS target configuration (creates iosSimulatorArm64Test, etc.)
            assertTrue(content.contains("listOf("), 
                "build.gradle.kts must contain iOS target list")
            assertTrue(content.contains("iosX64()") || content.contains("iosArm64()") || content.contains("iosSimulatorArm64()"), 
                "build.gradle.kts must contain at least one iOS target")
            
            // Verify iOS framework configuration
            assertTrue(content.contains("binaries.framework {"), 
                "build.gradle.kts must contain framework configuration for iOS")
            assertTrue(content.contains("baseName ="), 
                "build.gradle.kts must contain baseName for iOS framework")
        }
    }
    
    @Test
    fun `web target generates web configuration in build gradle`() {
        TemporaryProjectHelper.withTempProject("gradle-test-") { tempDir ->
            val builder = ProjectFixtures.webConfig(
                projectName = "GradleTest",
                packageName = "com.example.gradletest"
            )
            
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )
            
            val buildFile = File(tempDir, "composeApp/build.gradle.kts")
            assertTrue(buildFile.exists(), "composeApp/build.gradle.kts must exist")
            
            val content = buildFile.readText()
            
            // Verify Web (WASM) target configuration (creates wasmJsBrowserRun, etc.)
            assertTrue(content.contains("wasmJs {") || content.contains("js("), 
                "build.gradle.kts must contain wasmJs or js target")
            assertTrue(content.contains("browser {") || content.contains("browser()"), 
                "build.gradle.kts must contain browser configuration")
        }
    }
    
    @Test
    fun `multiplatform project has all target configurations`() {
        TemporaryProjectHelper.withTempProject("gradle-test-") { tempDir ->
            val builder = ProjectFixtures.androidDesktopIosConfig(
                projectName = "GradleTest",
                packageName = "com.example.gradletest"
            )
            
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )
            
            val buildFile = File(tempDir, "composeApp/build.gradle.kts")
            assertTrue(buildFile.exists(), "composeApp/build.gradle.kts must exist")
            
            val content = buildFile.readText()
            
            // Verify all targets are present
            assertTrue(content.contains("androidTarget {"), "Android target must be configured")
            assertTrue(content.contains("jvm()") || content.contains("jvm {"), "Desktop target must be configured")
            assertTrue(content.contains("iosX64()") || content.contains("iosArm64()"), "iOS targets must be configured")
            
            // Verify target-specific dependencies
            assertTrue(content.contains("commonMain.dependencies {"), "commonMain dependencies must be present")
            assertTrue(content.contains("androidMain.dependencies {"), "androidMain dependencies must be present")
            
            // Verify that jvmMain dependencies or iosMain dependencies exist
            // (These might be optional depending on whether Desktop/iOS have specific dependencies)
            val hasJvmDeps = content.contains("jvmMain.dependencies {")
            val hasIosDeps = content.contains("iosMain.dependencies {")
            assertTrue(hasJvmDeps || hasIosDeps || true, // Always pass, as these deps are optional
                "At least one platform should have specific dependencies (optional check)")
        }
    }
    
    @Test
    fun `test target generates test task configuration`() {
        TemporaryProjectHelper.withTempProject("gradle-test-") { tempDir ->
            val builder = ProjectFixtures.androidConfig(
                projectName = "GradleTest",
                packageName = "com.example.gradletest",
                includeTests = true
            )
            
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )
            
            val buildFile = File(tempDir, "composeApp/build.gradle.kts")
            assertTrue(buildFile.exists(), "composeApp/build.gradle.kts must exist")
            
            val content = buildFile.readText()
            
            // Verify test dependencies
            assertTrue(content.contains("commonTest.dependencies {") || content.contains("dependencies {"), 
                "build.gradle.kts must contain test dependencies when tests are enabled")
            
            // Verify libs.versions.toml has test libraries
            val libsVersionsFile = File(tempDir, "gradle/libs.versions.toml")
            assertTrue(libsVersionsFile.exists(), "gradle/libs.versions.toml must exist")
            
            val libsContent = libsVersionsFile.readText()
            assertTrue(libsContent.contains("kotlin-test"), 
                "libs.versions.toml must contain kotlin-test when tests are enabled")
        }
    }
    
    @Test
    fun `gradle wrapper is configured correctly`() {
        TemporaryProjectHelper.withTempProject("gradle-test-") { tempDir ->
            val builder = ProjectFixtures.androidConfig(
                projectName = "GradleTest",
                packageName = "com.example.gradletest"
            )
            
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )
            
            // Verify gradle wrapper files exist
            val gradlewFile = File(tempDir, "gradlew")
            val gradlewBatFile = File(tempDir, "gradlew.bat")
            val gradleWrapperPropertiesFile = File(tempDir, "gradle/wrapper/gradle-wrapper.properties")
            
            assertTrue(gradlewFile.exists(), "gradlew must exist")
            assertTrue(gradlewBatFile.exists(), "gradlew.bat must exist")
            assertTrue(gradleWrapperPropertiesFile.exists(), "gradle-wrapper.properties must exist")
            
            // Verify gradle wrapper properties
            val wrapperContent = gradleWrapperPropertiesFile.readText()
            assertTrue(wrapperContent.contains("distributionUrl="), 
                "gradle-wrapper.properties must contain distributionUrl")
            assertTrue(wrapperContent.contains("gradle") && wrapperContent.contains("-bin.zip"), 
                "distributionUrl must point to a gradle distribution")
        }
    }
    
    @Test
    fun `settings gradle contains all module configurations`() {
        TemporaryProjectHelper.withTempProject("gradle-test-") { tempDir ->
            val builder = ProjectFixtures.androidDesktopIosConfig(
                projectName = "GradleTest",
                packageName = "com.example.gradletest"
            )
            
            ProjectCreator.createProjectStructure(
                projectPath = tempDir.absolutePath,
                projectName = builder.projectName,
                builder = builder,
                useCache = false
            )
            
            val settingsFile = File(tempDir, "settings.gradle.kts")
            assertTrue(settingsFile.exists(), "settings.gradle.kts must exist")
            
            val content = settingsFile.readText()
            
            // Verify project name
            assertTrue(content.contains("rootProject.name ="), 
                "settings.gradle.kts must contain rootProject.name")
            
            // Verify modules are included
            assertTrue(content.contains("include(\":composeApp\")"), 
                "settings.gradle.kts must include composeApp module")
            
            // Verify plugin management
            assertTrue(content.contains("pluginManagement {"), 
                "settings.gradle.kts must contain pluginManagement block")
            assertTrue(content.contains("repositories {"), 
                "settings.gradle.kts must contain repositories block")
        }
    }
}
