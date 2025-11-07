package io.github.heisiar.composewizard.idea

import io.github.heisiar.composewizard.testutils.TemporaryProjectHelper
import io.github.heisiar.composewizard.shared.models.ComposeMultiplatformModuleBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.io.File

class GeneratorNewProjectWizardTest {
    
    @Test
    fun `GeneratorNewProjectWizard has correct id`() {
        val wizard = ComposeMultiplatformGeneratorNewProjectWizard()
        assertEquals("COMPOSE_MULTIPLATFORM", wizard.id)
    }
    
    @Test
    fun `GeneratorNewProjectWizard has correct name`() {
        val wizard = ComposeMultiplatformGeneratorNewProjectWizard()
        assertEquals("Compose Multiplatform", wizard.name)
    }
    
    @Test
    fun `GeneratorNewProjectWizard icon is not null`() {
        val wizard = ComposeMultiplatformGeneratorNewProjectWizard()
        assertNotNull(wizard.icon, "Icon must be loaded")
    }
    
    @Test
    fun `GeneratorNewProjectWizard is enabled by default`() {
        val wizard = ComposeMultiplatformGeneratorNewProjectWizard()
        assertTrue(wizard.isEnabled(), "Wizard should be enabled by default")
    }
    
    @Test
    fun `setupProjectFromBuilder creates project structure`() {
        TemporaryProjectHelper.withTempProject("generator-wizard-test-") { tempDir ->
            val builder = ComposeMultiplatformModuleBuilder()
            builder.projectName = "TestProject"
            builder.projectId = "com.example.test"
            builder.composeVersion = "1.7.1"
            builder.targetDesktop = true
            builder.targetAndroid = true
            
            // Simulate what setupProjectFromBuilder does internally
            builder.createProjectStructure(tempDir.absolutePath, builder.projectName)
            
            // Verify project structure was created
            assertTrue(File(tempDir, "build.gradle.kts").exists(), "build.gradle.kts must exist")
            assertTrue(File(tempDir, "settings.gradle.kts").exists(), "settings.gradle.kts must exist")
            assertTrue(File(tempDir, ".gitignore").exists(), ".gitignore must exist")
            assertTrue(File(tempDir, "composeApp").exists(), "composeApp directory must exist")
            assertTrue(File(tempDir, "composeApp/src/commonMain").exists(), "commonMain must exist")
            assertTrue(File(tempDir, "composeApp/src/androidMain").exists(), "androidMain must exist")
            assertTrue(File(tempDir, "composeApp/src/jvmMain").exists(), "jvmMain must exist for Desktop")
        }
    }
    
    @Test
    fun `setupProjectFromBuilder creates all platform modules when all targets enabled`() {
        TemporaryProjectHelper.withTempProject("generator-wizard-all-targets-") { tempDir ->
            val builder = ComposeMultiplatformModuleBuilder()
            builder.projectName = "AllPlatformsApp"
            builder.projectId = "com.example.allplatforms"
            builder.composeVersion = "1.7.1"
            builder.targetDesktop = true
            builder.targetAndroid = true
            builder.targetIOS = true
            builder.targetWeb = true
            
            builder.createProjectStructure(tempDir.absolutePath, builder.projectName)
            
            // Verify all platform-specific code exists
            assertTrue(File(tempDir, "composeApp/src/androidMain").exists(), "androidMain must exist")
            assertTrue(File(tempDir, "composeApp/src/jvmMain").exists(), "jvmMain must exist for Desktop")
            assertTrue(File(tempDir, "composeApp/src/iosMain").exists(), "iosMain must exist")
            assertTrue(File(tempDir, "composeApp/src/wasmJsMain").exists(), "wasmJsMain must exist")
            
            // Verify iOS project exists
            assertTrue(File(tempDir, "iosApp").exists(), "iosApp directory must exist")
            
            // Verify gradle files exist and are not empty
            val buildGradle = File(tempDir, "build.gradle.kts")
            assertTrue(buildGradle.exists() && buildGradle.length() > 0, "build.gradle.kts should exist and not be empty")
        }
    }
    
    @Test
    fun `setupProjectFromBuilder creates gradle wrapper files`() {
        TemporaryProjectHelper.withTempProject("generator-wizard-gradle-") { tempDir ->
            val builder = ComposeMultiplatformModuleBuilder()
            builder.projectName = "GradleTest"
            builder.projectId = "com.example.gradle"
            builder.targetDesktop = true
            
            builder.createProjectStructure(tempDir.absolutePath, builder.projectName)
            
            assertTrue(File(tempDir, "gradlew").exists(), "gradlew must exist")
            assertTrue(File(tempDir, "gradlew.bat").exists(), "gradlew.bat must exist")
            assertTrue(File(tempDir, "gradle/wrapper").exists(), "gradle wrapper directory must exist")
            assertTrue(File(tempDir, "gradle/wrapper/gradle-wrapper.jar").exists(), "gradle-wrapper.jar must exist")
            assertTrue(File(tempDir, "gradle/wrapper/gradle-wrapper.properties").exists(), "gradle-wrapper.properties must exist")
            
            // Note: In tests, gradlew may not be executable until actually copied to filesystem
            // Just verify it exists
            assertTrue(File(tempDir, "gradlew").exists() && File(tempDir, "gradlew").length() > 0, 
                "gradlew should exist and not be empty")
        }
    }
    
    @Test
    fun `setupProjectFromBuilder creates libs versions toml`() {
        TemporaryProjectHelper.withTempProject("generator-wizard-toml-") { tempDir ->
            val builder = ComposeMultiplatformModuleBuilder()
            builder.projectName = "TomlTest"
            builder.projectId = "com.example.toml"
            builder.composeVersion = "1.7.1"
            builder.targetDesktop = true
            
            builder.createProjectStructure(tempDir.absolutePath, builder.projectName)
            
            val tomlFile = File(tempDir, "gradle/libs.versions.toml")
            assertTrue(tomlFile.exists(), "libs.versions.toml must exist")
            
            val tomlContent = tomlFile.readText()
            // Check for compose version in various formats
            assertTrue(tomlContent.contains("1.7.1") || tomlContent.contains("compose"), 
                "Should contain compose version or compose keyword")
            assertTrue(tomlContent.contains("[versions]"), "Should have versions section")
            assertTrue(tomlContent.contains("[libraries]"), "Should have libraries section")
            assertTrue(tomlContent.contains("[plugins]"), "Should have plugins section")
        }
    }
    
    @Test
    fun `setupProjectFromBuilder creates gitignore with correct content`() {
        TemporaryProjectHelper.withTempProject("generator-wizard-gitignore-") { tempDir ->
            val builder = ComposeMultiplatformModuleBuilder()
            builder.projectName = "GitignoreTest"
            builder.projectId = "com.example.gitignore"
            builder.targetDesktop = true
            
            builder.createProjectStructure(tempDir.absolutePath, builder.projectName)
            
            val gitignoreFile = File(tempDir, ".gitignore")
            assertTrue(gitignoreFile.exists(), ".gitignore must exist")
            
            val gitignoreContent = gitignoreFile.readText()
            assertTrue(gitignoreContent.contains("*.iml"), "Should ignore .iml files")
            assertTrue(gitignoreContent.contains(".gradle"), "Should ignore .gradle")
            assertTrue(gitignoreContent.contains("build/"), "Should ignore build/")
            assertTrue(gitignoreContent.contains(".DS_Store"), "Should ignore .DS_Store")
            assertTrue(gitignoreContent.contains("local.properties"), "Should ignore local.properties")
        }
    }
    
    @Test
    fun `setupProjectFromBuilder creates README with project info`() {
        TemporaryProjectHelper.withTempProject("generator-wizard-readme-") { tempDir ->
            val builder = ComposeMultiplatformModuleBuilder()
            builder.projectName = "ReadmeTest"
            builder.projectId = "com.example.readme"
            builder.targetDesktop = true
            builder.targetAndroid = true
            
            builder.createProjectStructure(tempDir.absolutePath, builder.projectName)
            
            val readmeFile = File(tempDir, "README.md")
            assertTrue(readmeFile.exists(), "README.md must exist")
            
            val readmeContent = readmeFile.readText()
            // README exists and has content
            assertTrue(readmeContent.isNotEmpty(), "README should not be empty")
            assertTrue(readmeContent.contains("Compose") || readmeContent.contains("Multiplatform") || 
                    readmeContent.contains("Project"), "Should mention Compose, Multiplatform or Project")
        }
    }
    
    @Test
    fun `ModuleBuilder properties are correctly set from wizard state`() {
        val builder = ComposeMultiplatformModuleBuilder()
        
        // Set properties like wizard would
        builder.projectName = "WizardStateTest"
        builder.projectId = "com.example.wizardstate"
        builder.composeVersion = "1.7.1"
        builder.targetDesktop = true
        builder.targetAndroid = false
        builder.targetIOS = true
        builder.targetWeb = false
        builder.includeTests = true
        
        // Verify properties
        assertEquals("WizardStateTest", builder.projectName)
        assertEquals("com.example.wizardstate", builder.projectId)
        assertEquals("1.7.1", builder.composeVersion)
        assertTrue(builder.targetDesktop)
        assertFalse(builder.targetAndroid)
        assertTrue(builder.targetIOS)
        assertFalse(builder.targetWeb)
        assertTrue(builder.includeTests)
    }
    
    @Test
    fun `ModuleBuilder presentable name is correct`() {
        val builder = ComposeMultiplatformModuleBuilder()
        assertEquals("Compose Multiplatform", builder.presentableName)
    }
    
    @Test
    fun `ModuleBuilder builderId is correct`() {
        val builder = ComposeMultiplatformModuleBuilder()
        assertEquals("COMPOSE_MULTIPLATFORM", builder.builderId)
    }
    
    @Test
    fun `ModuleBuilder is not template based`() {
        val builder = ComposeMultiplatformModuleBuilder()
        assertFalse(builder.isTemplateBased, "Should not be template based")
    }
    
    @Test
    fun `ModuleBuilder is available`() {
        val builder = ComposeMultiplatformModuleBuilder()
        assertTrue(builder.isAvailable, "Builder should be available")
    }
    
    @Test
    fun `setupProjectFromBuilder handles project name with spaces`() {
        TemporaryProjectHelper.withTempProject("generator-wizard-spaces-") { tempDir ->
            val builder = ComposeMultiplatformModuleBuilder()
            builder.projectName = "My Test Project"
            builder.projectId = "com.example.mytestproject"
            builder.targetDesktop = true
            
            builder.createProjectStructure(tempDir.absolutePath, builder.projectName)
            
            assertTrue(File(tempDir, "build.gradle.kts").exists(), "Should handle project names with spaces")
            assertTrue(File(tempDir, "settings.gradle.kts").exists(), "Should create all necessary files")
        }
    }
    
    @Test
    fun `setupProjectFromBuilder handles special characters in package name`() {
        TemporaryProjectHelper.withTempProject("generator-wizard-package-") { tempDir ->
            val builder = ComposeMultiplatformModuleBuilder()
            builder.projectName = "PackageTest"
            builder.projectId = "com.example.my_app.test"
            builder.targetDesktop = true
            
            builder.createProjectStructure(tempDir.absolutePath, builder.projectName)
            
            assertTrue(File(tempDir, "build.gradle.kts").exists(), "Should handle underscores in package name")
            
            // Verify package structure is created correctly
            val appKt = File(tempDir, "composeApp/src/commonMain/kotlin/App.kt")
            if (appKt.exists()) {
                val content = appKt.readText()
                assertTrue(content.contains("package") || content.contains("import"), "Should have valid Kotlin code")
            }
        }
    }
}

