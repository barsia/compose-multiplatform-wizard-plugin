package io.github.heisiar.composewizard.androidstudio.project

import com.android.tools.idea.npw.model.NewProjectModel
import com.android.tools.idea.npw.model.NewProjectModuleModel
import com.android.tools.idea.wizard.model.ModelWizardStep
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.AlignY
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import io.github.heisiar.composewizard.shared.models.ComposeMultiplatformModuleBuilder
import io.github.heisiar.composewizard.shared.ui.ComposeMultiplatformWizardStep
import javax.swing.JComponent

/**
 * Custom step 2 for New Project wizard that replaces the standard ConfigureAndroidProjectStep.
 * 
 * This step embeds the full Compose UI wizard (via Jewel) directly into the standard
 * Android Studio New Project flow, providing:
 * - Beautiful Compose UI on step 2
 * - All parameters on one screen (no step 3 needed)
 * - No Groovy/Java/Minimum SDK confusion
 * - Same UX as IntelliJ IDEA and Module wizard
 * 
 * This is the PROPER way to create Compose Multiplatform projects in Android Studio!
 */
class ComposeMultiplatformConfigureProjectStep(
    private val projectModel: NewProjectModel,
    private val moduleModel: NewProjectModuleModel
) : ModelWizardStep<NewProjectModuleModel>(moduleModel, "Configure Compose Multiplatform Project") {
    
    private val propertyGraph = PropertyGraph()
    
    // Embed Compose UI wizard via Jewel
    private val moduleBuilder = ComposeMultiplatformModuleBuilder().apply {
        // Pre-fill with defaults
        projectName = projectModel.applicationName.get()
        projectId = projectModel.packageName.get()
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
            // Add padding for better UX
            border = JBUI.Borders.empty(10, 15, 10, 15)
        }
    }
    
    override fun onProceeding() {
        super.onProceeding()
        
        // Update models from Compose UI
        composeStep.updateDataModel()
        
        // Update project model
        projectModel.applicationName.set(composeStep.getProjectName())
        projectModel.packageName.set(composeStep.getProjectId())
    }
    
    override fun canGoForward(): com.android.tools.idea.observable.core.ObservableBool {
        // Use Compose UI validation
        return com.android.tools.idea.observable.core.BoolValueProperty(composeStep.validate())
    }
    
    override fun getPreferredFocusComponent(): JComponent {
        return composeStep.component
    }
    
    fun getModuleBuilder(): ComposeMultiplatformModuleBuilder {
        return moduleBuilder
    }
}

