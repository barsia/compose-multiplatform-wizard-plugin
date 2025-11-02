package io.github.heisiar.composewizard.androidstudio.module

import com.android.tools.idea.observable.core.ObservableBool
import com.android.tools.idea.wizard.model.SkippableWizardStep
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.AlignY
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.panel
import io.github.heisiar.composewizard.shared.models.ComposeMultiplatformModuleBuilder
import io.github.heisiar.composewizard.shared.ui.ComposeMultiplatformWizardStep
import javax.swing.JComponent

/**
 * Custom wizard step for Compose Multiplatform module configuration.
 * 
 * This step embeds the full Compose UI wizard (via Jewel) into the Android Studio
 * Module wizard, providing a rich, modern UI with:
 * - Beautiful Compose UI with live preview
 * - All platforms on one screen
 * - No Groovy or Java options (Kotlin DSL only)
 * - Version selection with dev versions support
 * - Real-time validation
 * 
 * This is the same UI used in IntelliJ IDEA New Project wizard!
 */
class ComposeMultiplatformConfigureStep(
    private val model: ComposeMultiplatformModuleModel,
    private val basePackage: String,
    title: String
) : SkippableWizardStep<ComposeMultiplatformModuleModel>(model, title) {
    
    // Embed Compose UI wizard via Jewel
    private val moduleBuilder = ComposeMultiplatformModuleBuilder().apply {
        // Pre-fill with module defaults
        projectName = model.moduleName.get()
        projectId = model.packageName.get()
    }
    
    private val composeStep = ComposeMultiplatformWizardStep(moduleBuilder)
    
    override fun getComponent(): JComponent {
        return panel {
            row {
                cell(composeStep.component)
                    .align(AlignX.FILL)
                    .align(AlignY.FILL)
                    .resizableColumn()
            }.resizableRow()
                .topGap(TopGap.NONE)
        }.apply {
            // Add padding around Compose UI to prevent fields from being too close to edges
            border = com.intellij.util.ui.JBUI.Borders.empty(10, 15, 10, 15)
        }
    }
    
    override fun onProceeding() {
        super.onProceeding()
        
        // Update model from Compose UI
        composeStep.updateDataModel()
        
        model.moduleName.set(composeStep.getProjectName())
        model.packageName.set(composeStep.getProjectId())
        model.includeAndroid.set(moduleBuilder.targetAndroid)
        model.includeIos.set(moduleBuilder.targetIOS)
        model.includeDesktop.set(moduleBuilder.targetDesktop)
        model.includeWeb.set(moduleBuilder.targetWeb)
        model.includeTests.set(moduleBuilder.includeTests)
        model.composeVersion.set(moduleBuilder.composeVersion)
    }
    
    override fun canGoForward(): ObservableBool {
        // Use Compose UI validation
        return com.android.tools.idea.observable.core.BoolValueProperty(composeStep.validate())
    }
    
    override fun getPreferredFocusComponent(): JComponent {
        return composeStep.component
    }
}

