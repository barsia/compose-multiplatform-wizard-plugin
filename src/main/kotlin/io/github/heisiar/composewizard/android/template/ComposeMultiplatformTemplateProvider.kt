package io.github.heisiar.composewizard.android.template

import com.android.tools.idea.wizard.template.Category
import com.android.tools.idea.wizard.template.Constraint
import com.android.tools.idea.wizard.template.FormFactor
import com.android.tools.idea.wizard.template.ModuleTemplateData
import com.android.tools.idea.wizard.template.Recipe
import com.android.tools.idea.wizard.template.RecipeExecutor
import com.android.tools.idea.wizard.template.StringParameter
import com.android.tools.idea.wizard.template.Template
import com.android.tools.idea.wizard.template.TemplateData
import com.android.tools.idea.wizard.template.TextFieldWidget
import com.android.tools.idea.wizard.template.Widget
import com.android.tools.idea.wizard.template.WizardTemplateProvider
import com.android.tools.idea.wizard.template.WizardUiContext
import com.android.tools.idea.wizard.template.BooleanParameter
import com.android.tools.idea.wizard.template.CheckBoxWidget
import com.android.tools.idea.wizard.template.PackageNameWidget
import com.android.tools.idea.wizard.template.Thumb
import java.io.File
import java.net.URL

class ComposeMultiplatformTemplateProvider : WizardTemplateProvider() {
    override fun getTemplates(): List<Template> {
        return listOf(
            composeMultiplatformProjectTemplate
        )
    }
}

val composeMultiplatformProjectTemplate: Template
    get() = object : Template {
        override val name: String = "Compose Multiplatform"
        override val description: String = "Create a new Compose Multiplatform project for Android, iOS, Desktop, and Web"
        override val minSdk: Int = 24
        override val category: Category = Category.Application
        override val formFactor: FormFactor = FormFactor.Mobile
        override val constraints: Collection<com.android.tools.idea.wizard.template.TemplateConstraint> = emptyList()
        override val uiContexts: Collection<WizardUiContext> = listOf(
            WizardUiContext.NewProject,
            WizardUiContext.NewProjectExtraDetail
        )
        override val documentationUrl: String? = null
        
        val includeAndroid = BooleanParameter(
            name = "🤖 Android",
            defaultValue = true,
            help = "Android module"
        )
        
        val includeIos = BooleanParameter(
            name = "🍎 iOS",
            defaultValue = true,
            help = "iOS module for cross-platform support"
        )
        
        val includeDesktop = BooleanParameter(
            name = "🖥️ Desktop (JVM)", 
            defaultValue = true,
            help = "Desktop (JVM) module"
        )
        
        val includeWeb = BooleanParameter(
            name = "🌐 Web (Wasm)",
            defaultValue = true,
            help = "Web (Wasm) module"
        )
        
        override val widgets: Collection<Widget<*>> = listOf(
            com.android.tools.idea.wizard.template.LabelWidget("Target platforms:"),
            com.android.tools.idea.wizard.template.Separator,
            CheckBoxWidget(includeAndroid),
            CheckBoxWidget(includeIos),
            CheckBoxWidget(includeDesktop),
            CheckBoxWidget(includeWeb)
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
                includeWeb.value
            )
        }
        
        override val useGenericInstrumentedTests: Boolean = false
        override val useGenericLocalTests: Boolean = false
    }

fun composeMultiplatformProjectRecipe(
    moduleData: ModuleTemplateData?,
    includeAndroid: Boolean,
    includeIos: Boolean,
    includeDesktop: Boolean,
    includeWeb: Boolean
): Boolean {
    println("=== Compose Multiplatform Recipe Started ===")
    println("Platforms: Android=$includeAndroid, iOS=$includeIos, Desktop=$includeDesktop, Web=$includeWeb")
    
    if (moduleData == null) {
        println("ERROR: ModuleTemplateData is null")
        return false
    }
    
    // 1. Get values from standard AS fields
    val packageName = moduleData.packageName
    val projectPath = moduleData.projectTemplateData.rootDir.path
    val applicationName = moduleData.projectTemplateData.rootDir.name
    
    println("Package: $packageName")
    println("Application: $applicationName")
    println("Creating project at: $projectPath")
    
    // 2. Validation
    val validation = io.github.heisiar.composewizard.shared.ValidationUtils.validateProjectId(packageName)
    if (!validation.isValid) {
        println("ERROR: Invalid package name - ${validation.errors.first()}")
        return false
    }
    
    // 3. Create TemplateProcessor with all parameters
    val processor = io.github.heisiar.composewizard.shared.TemplateProcessor(
        projectName = applicationName,
        projectId = packageName,
        composeVersion = "1.10.0-alpha03",
        includeTests = false,
        targetDesktop = includeDesktop,
        targetAndroid = includeAndroid,
        targetIOS = includeIos,
        targetWeb = includeWeb,
        enableDevVersions = false
    )
    
    // 4. Copy template to project
    try {
        processor.copyTemplateToProject(projectPath)
        println("=== Project created successfully! ===")
        return true
    } catch (e: Exception) {
        println("ERROR creating project: ${e.message}")
        e.printStackTrace()
        return false
    }
}

