package io.github.heisiar.composewizard.androidstudio

import com.android.tools.idea.wizard.template.*
import io.github.heisiar.composewizard.shared.ComposeVersions
import io.github.heisiar.composewizard.shared.TemplateProcessor
import io.github.heisiar.composewizard.shared.ValidationUtils
import io.github.heisiar.composewizard.shared.statistics.ComposeWizardUsageCollector

/**
 * Compose Multiplatform wizard template for Android Studio.
 * Appears in New Project wizard under "Phone and Tablet".
 */
val composeMultiplatformTemplate: Template
    get() {
        
        return object : Template {
            override val name: String = "Compose Multiplatform"
            override val description: String = "Create a new Compose Multiplatform project for Android, iOS, Desktop, and Web"
            override val minSdk: Int = 24
            override val category: Category = Category.Application
            override val formFactor: FormFactor = FormFactor.Generic
            override val constraints: Collection<TemplateConstraint> = listOf(TemplateConstraint.Kotlin)
            override val uiContexts: Collection<WizardUiContext> = listOf(
                WizardUiContext.NewProject,
                WizardUiContext.NewProjectExtraDetail
            )
            override val documentationUrl: String? = null
        
        val includeAndroid = BooleanParameter(
            name = "Android",
            defaultValue = true,
            help = "Include Android target"
        )
        
        val includeIos = BooleanParameter(
            name = "iOS",
            defaultValue = true,
            help = "Include iOS target"
        )
        
        val includeDesktop = BooleanParameter(
            name = "Desktop", 
            defaultValue = true,
            help = "Include Desktop (JVM) target"
        )
        
        val includeWeb = BooleanParameter(
            name = "Web",
            defaultValue = true,
            help = "Include Web (Wasm) target"
        )
        
        val includeTests = BooleanParameter(
            name = "Include sample tests",
            defaultValue = true,
            help = "Add example unit and UI tests"
        )
        
        val initGit = BooleanParameter(
            name = "Initialize Git repository",
            defaultValue = true,
            help = "Create .git directory and initial commit"
        )
        
        val composeVersion = StringParameter(
            name = "Compose Multiplatform version",
            defaultValue = ComposeVersions.DEFAULT_VERSION,
            help = "Enter version number",
            constraints = listOf()
        )
        
        override val widgets: Collection<Widget<*>> = listOf(
            LabelWidget("Target platforms:"),
            CheckBoxWidget(includeAndroid),
            CheckBoxWidget(includeIos),
            CheckBoxWidget(includeDesktop),
            CheckBoxWidget(includeWeb),
            Separator,
            LabelWidget("Additional options:"),
            CheckBoxWidget(includeTests),
            CheckBoxWidget(initGit),
            Separator,
            TextFieldWidget(composeVersion),
            LabelWidget("Available: ${ComposeVersions.KNOWN_STABLE_VERSIONS.joinToString(", ")}")
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
            try {
                val projectDir = java.io.File(projectPath)
                // Simple git init - could be enhanced with .gitignore and initial commit
                Runtime.getRuntime().exec(arrayOf("git", "init"), null, projectDir).waitFor()
                println("Git repository initialized")
            } catch (e: Exception) {
                println("WARNING: Could not initialize git: ${e.message}")
                // Don't fail the whole project creation if git fails
            }
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

