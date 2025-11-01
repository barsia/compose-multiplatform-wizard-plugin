package io.github.heisiar.composewizard.androidstudio

import com.android.tools.idea.wizard.template.Template
import com.android.tools.idea.wizard.template.WizardTemplateProvider

/**
 * Provider for Compose Multiplatform wizard templates in Android Studio.
 * This makes our template appear in the New Project wizard under "Phone and Tablet".
 */
class ComposeMultiplatformTemplateProvider : WizardTemplateProvider() {
    override fun getTemplates(): List<Template> {
        return listOf(composeMultiplatformTemplate)
    }
}

