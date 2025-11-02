package io.github.heisiar.composewizard.androidstudio

import com.android.tools.idea.wizard.template.*
import io.github.heisiar.composewizard.shared.ComposeVersions
import io.github.heisiar.composewizard.shared.TemplateProcessor
import io.github.heisiar.composewizard.shared.ValidationUtils
import io.github.heisiar.composewizard.shared.statistics.ComposeWizardUsageCollector
import io.github.heisiar.composewizard.shared.services.ComposeVersionCache
import java.io.File

/**
 * Compose Multiplatform wizard template for Android Studio.
 * Appears in New Project wizard under "Phone and Tablet".
 */
val composeMultiplatformTemplate: Template
    get() {
        
        return object : Template {
            override val name: String = "Compose Multiplatform"
            override val description: String = """
                <html>
                <b>Create a Compose Multiplatform project for Android, iOS, Desktop, and Web</b><br><br>
                <b>Note:</b> Only <span style="color: #E8850F;">Kotlin DSL</span> is supported for build configuration (Groovy is not supported).
                </html>
            """.trimIndent()
            override val minSdk: Int = 24
            override val category: Category = Category.Application
            override val formFactor: FormFactor = FormFactor.Generic
            override val constraints: Collection<TemplateConstraint> = listOf(TemplateConstraint.Kotlin)
            override val uiContexts: Collection<WizardUiContext> = listOf(
                WizardUiContext.NewProject,
                WizardUiContext.NewProjectExtraDetail
            )
            override val documentationUrl: String? = "https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-multiplatform-getting-started.html"
        
        val includeAndroid = BooleanParameter(
            name = "Android",
            defaultValue = io.github.heisiar.composewizard.shared.WizardDefaults.TARGET_ANDROID,
            help = "Include Android target"
        )
        
        val includeIos = BooleanParameter(
            name = "iOS",
            defaultValue = io.github.heisiar.composewizard.shared.WizardDefaults.TARGET_IOS,
            help = "Include iOS target"
        )
        
        val includeDesktop = BooleanParameter(
            name = "Desktop", 
            defaultValue = io.github.heisiar.composewizard.shared.WizardDefaults.TARGET_DESKTOP,
            help = "Include Desktop (JVM) target"
        )
        
        val includeWeb = BooleanParameter(
            name = "Web",
            defaultValue = io.github.heisiar.composewizard.shared.WizardDefaults.TARGET_WEB,
            help = "Include Web (Wasm) target"
        )

        val initGit = BooleanParameter(
            name = "Create Git repository",
            defaultValue = io.github.heisiar.composewizard.shared.WizardDefaults.INIT_GIT,
            help = "Create .git directory and initial commit"
        )
        
        val includeTests = BooleanParameter(
            name = "Include tests",
            defaultValue = io.github.heisiar.composewizard.shared.WizardDefaults.INCLUDE_TESTS,
            help = "Add example tests"
        )
        
        val composeVersion = StringParameter(
            name = "Compose Multiplatform version",
            defaultValue = ComposeVersions.DEFAULT_VERSION,
            help = buildVersionHelp(),
            constraints = listOf()
        )
        
        override val widgets: Collection<Widget<*>> = listOf(
            TextFieldWidget(composeVersion),
            Separator,
            LabelWidget("Target platforms:"),
            CheckBoxWidget(includeAndroid),
            CheckBoxWidget(includeIos),
            CheckBoxWidget(includeDesktop),
            CheckBoxWidget(includeWeb),
            Separator,
            LabelWidget("Additional options:"),
            CheckBoxWidget(initGit),
            CheckBoxWidget(includeTests)
        )
        
        override fun thumb(): Thumb {
            val iconUrl = javaClass.classLoader.getResource("wizards/compose-multiplatform.png")
            return if (iconUrl != null) Thumb { iconUrl } else Thumb.NoThumb
        }
        
        override val recipe: Recipe = { data: TemplateData ->
            composeMultiplatformProjectRecipe(
                data as? ModuleTemplateData,
                includeAndroid.value,
                includeIos.value,
                includeDesktop.value,
                includeWeb.value,
                includeTests.value,
                initGit.value,
                composeVersion.value
            )
        }
        
        override val useGenericInstrumentedTests: Boolean = false
        override val useGenericLocalTests: Boolean = false
    }
}

fun composeMultiplatformProjectRecipe(
    moduleData: ModuleTemplateData?,
    includeAndroid: Boolean,
    includeIos: Boolean,
    includeDesktop: Boolean,
    includeWeb: Boolean,
    includeTests: Boolean,
    initGit: Boolean,
    composeVersion: String
): Boolean {
    val startTime = System.currentTimeMillis()
    
    // Log wizard opened (FUS)
    ComposeWizardUsageCollector.logWizardOpened()
    
    println("=== Compose Multiplatform Recipe Started ===")
    println("Platforms: Android=$includeAndroid, iOS=$includeIos, Desktop=$includeDesktop, Web=$includeWeb")
    println("Options: Tests=$includeTests, Git=$initGit")
    
    if (moduleData == null) {
        println("ERROR: ModuleTemplateData is null")
        return false
    }
    
    // Get values from standard AS fields
    val packageName = moduleData.packageName
    val projectPath = moduleData.projectTemplateData.rootDir.path
    val applicationName = moduleData.projectTemplateData.rootDir.name
    
    println("Package: $packageName")
    println("Application: $applicationName")
    println("Creating project at: $projectPath")
    
    // Validation
    val validation = ValidationUtils.validateProjectId(packageName)
    if (!validation.isValid) {
        println("ERROR: Invalid package name - ${validation.errors.first()}")
        ComposeWizardUsageCollector.logValidationError("packageName", "invalid_format")
        return false
    }
    
    // Use selected Compose version from dropdown
    println("Selected Compose version: $composeVersion")
    
    // Log platform toggles (FUS)
    ComposeWizardUsageCollector.logPlatformToggled("Android", includeAndroid)
    ComposeWizardUsageCollector.logPlatformToggled("iOS", includeIos)
    ComposeWizardUsageCollector.logPlatformToggled("Desktop", includeDesktop)
    ComposeWizardUsageCollector.logPlatformToggled("Web", includeWeb)
    ComposeWizardUsageCollector.logTestsToggled(includeTests)
    ComposeWizardUsageCollector.logGitToggled(initGit)
    
    // Create TemplateProcessor with all parameters
    val processor = TemplateProcessor(
        projectName = applicationName,
        projectId = packageName,
        composeVersion = composeVersion,
        includeTests = includeTests,
        targetDesktop = includeDesktop,
        targetAndroid = includeAndroid,
        targetIOS = includeIos,
        targetWeb = includeWeb,
        enableDevVersions = false
    )
    
    // Copy template to project
    try {
        processor.copyTemplateToProject(projectPath)
        
        // Initialize Git if requested
        if (initGit) {
            io.github.heisiar.composewizard.shared.ProjectCreator.initializeGitRepository(projectPath)
        }
        
        // Log successful project creation (FUS)
        val duration = System.currentTimeMillis() - startTime
        val targetPlatformsCount = listOf(includeAndroid, includeIos, includeDesktop, includeWeb).count { it }
        ComposeWizardUsageCollector.logWizardCompleted(
            platformsCount = targetPlatformsCount,
            includeTests = includeTests,
            includeGit = initGit,
            usedDevVersions = false,
            timeSpentMs = duration
        )
        
        println("=== Project created successfully in ${duration}ms! ===")
        return true
    } catch (e: Exception) {
        println("ERROR creating project: ${e.message}")
        e.printStackTrace()
        
        // Log validation error for failed creation
        ComposeWizardUsageCollector.logValidationError("projectCreation", "exception")
        
        return false
    }
}

/**
 * Recipe for creating Compose Multiplatform module in existing project.
 * 
 * This is a simplified version of composeMultiplatformProjectRecipe
 * that creates a module instead of a full project.
 */
fun composeMultiplatformModuleRecipe(
    moduleDir: File,
    moduleName: String,
    packageName: String,
    includeAndroid: Boolean,
    includeIos: Boolean,
    includeDesktop: Boolean,
    includeWeb: Boolean,
    includeTests: Boolean,
    composeVersion: String
): Boolean {
    println("=== Compose Multiplatform Module Recipe Started ===")
    println("Module: $moduleName")
    println("Package: $packageName")
    println("Platforms: Android=$includeAndroid, iOS=$includeIos, Desktop=$includeDesktop, Web=$includeWeb")
    
    try {
        // Create module directory structure
        moduleDir.mkdirs()
        
        // Create basic build.gradle.kts
        val buildGradle = File(moduleDir, "build.gradle.kts")
        buildGradle.writeText("""
            plugins {
                kotlin("multiplatform") version "1.9.20"
                ${if (includeAndroid) "id(\"com.android.library\")\n    " else ""}id("org.jetbrains.compose") version "$composeVersion"
            }
            
            kotlin {
                ${if (includeAndroid) "androidTarget()\n    " else ""}${if (includeIos) "iosX64()\n    iosArm64()\n    iosSimulatorArm64()\n    " else ""}${if (includeDesktop) "jvm(\"desktop\")\n    " else ""}${if (includeWeb) "js(IR) {\n        browser()\n    }\n    " else ""}
                sourceSets {
                    val commonMain by getting {
                        dependencies {
                            implementation(compose.runtime)
                            implementation(compose.foundation)
                            implementation(compose.material3)
                        }
                    }
                    ${if (includeTests) """
                    val commonTest by getting {
                        dependencies {
                            implementation(kotlin("test"))
                        }
                    }
                    """ else ""}
                }
            }
        """.trimIndent())
        
        // Create source directories
        val srcCommonMain = File(moduleDir, "src/commonMain/kotlin/${packageName.replace('.', '/')}")
        srcCommonMain.mkdirs()
        
        // Create simple App.kt
        val appKt = File(srcCommonMain, "App.kt")
        appKt.writeText("""
            package $packageName
            
            import androidx.compose.material3.Text
            import androidx.compose.runtime.Composable
            
            @Composable
            fun App() {
                Text("Hello from Compose Multiplatform!")
            }
        """.trimIndent())
        
        println("=== Module created successfully! ===")
        return true
    } catch (e: Exception) {
        println("ERROR creating module: ${e.message}")
        e.printStackTrace()
        return false
    }
}

/**
 * Build help text with available Compose versions from Maven.
 * 
 * Loads versions from Maven (with 3s timeout) and shows them in tooltip.
 * This is the best we can do in Template API - tooltip is not copyable,
 * but at least shows fresh versions from Maven.
 */
private fun buildVersionHelp(): String {
    return try {
        val cache = ComposeVersionCache.getInstance()
        // Wait for versions to load (max 3 seconds)
        val versions = cache.getStableVersionsBlocking(timeoutMs = 3000)
        
        // Show all loaded versions in tooltip
        buildString {
            appendLine("Available versions (from Maven):")
            appendLine()
            versions.take(20).forEachIndexed { index, version ->
                if (index == 0) {
                    appendLine("  $version  ← Latest")
                } else {
                    appendLine("  $version")
                }
            }
            if (versions.size > 20) {
                appendLine()
                appendLine("... and ${versions.size - 20} more versions")
            }
            appendLine()
            appendLine("Type version manually (e.g., 1.9.2)")
        }
    } catch (e: Exception) {
        "Enter Compose Multiplatform version (e.g., 1.9.2)"
    }
}

