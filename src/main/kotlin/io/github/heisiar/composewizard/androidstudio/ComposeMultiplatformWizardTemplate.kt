package io.github.heisiar.composewizard.androidstudio

// NOTE: This file will only be loaded in Android Studio runtime
// com.android.tools.idea.wizard.template classes are available at runtime but not at compile time
// We comment out the implementation for now and will use only Actions

/*
import com.android.tools.idea.wizard.template.*
import io.github.heisiar.composewizard.shared.TemplateProcessor
import io.github.heisiar.composewizard.shared.ValidationUtils
import java.io.File

fun composeMultiplatformTemplate(): Template {
    return template {
        name = "Compose Multiplatform"
        description = "Create a multiplatform project with Compose for Android, iOS, Desktop, and Web"
        minApi = 24
        category = Category.Application
        formFactor = FormFactor.Mobile
        
        val packageName = stringParameter {
            name = "Package name"
            default = "com.example.myapp"
            constraints = listOf(Constraint.PACKAGE, Constraint.NONEMPTY)
        }
        
        val includeIos = booleanParameter {
            name = "Include iOS"
            default = true
        }
        
        val includeDesktop = booleanParameter {
            name = "Include Desktop"
            default = true
        }
        
        val includeWeb = booleanParameter {
            name = "Include Web"
            default = false
        }
        
        recipe = { data ->
            val projectDir = File(data.projectTemplateData.topOut.path, data.projectTemplateData.topOut.name)
            projectDir.mkdirs()
            
            val processor = TemplateProcessor(
                projectName = data.projectTemplateData.topOut.name,
                projectId = packageName.value,
                composeVersion = "1.7.1",
                includeTests = true,
                targetDesktop = includeDesktop.value,
                targetAndroid = true,
                targetIOS = includeIos.value,
                targetWeb = includeWeb.value,
                enableDevVersions = false
            )
            
            processor.copyTemplateToProject(projectDir.absolutePath)
        }
    }
}
*/

