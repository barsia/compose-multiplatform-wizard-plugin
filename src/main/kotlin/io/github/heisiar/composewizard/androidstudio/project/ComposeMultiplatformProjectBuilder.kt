package io.github.heisiar.composewizard.androidstudio.project

import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.SettingsStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.Disposable
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.util.IconLoader
import icons.StudioIcons
import io.github.heisiar.composewizard.shared.models.ComposeMultiplatformModuleBuilder
import io.github.heisiar.composewizard.shared.ui.ComposeMultiplatformWizardStep
import javax.swing.Icon
import javax.swing.JComponent

/**
 * Custom ModuleBuilder that shows Compose UI directly in New Project wizard.
 * 
 * This builder creates a completely custom wizard flow:
 * - Step 1: Compose UI (via Jewel) - ALL parameters on one screen
 * - That's it! No step 2, no step 3!
 * 
 * This is registered with order="first" to appear before standard Android wizard.
 */
class ComposeMultiplatformProjectBuilder : ModuleBuilder() {
    
    private val moduleBuilder = ComposeMultiplatformModuleBuilder()
    private var composeStep: ComposeMultiplatformWizardStep? = null
    
    override fun getBuilderId(): String = "ComposeMultiplatform"
    
    override fun getPresentableName(): String = "Compose Multiplatform"
    
    override fun getDescription(): String = 
        "Create a new Compose Multiplatform project for Android, iOS, Desktop, and Web"
    
    override fun getNodeIcon(): Icon = 
        IconLoader.getIcon("/META-INF/compose.svg", javaClass)
    
    override fun getModuleType(): ModuleType<*> = 
        com.intellij.openapi.module.JavaModuleType.getModuleType()
    
    override fun getParentGroup(): String = "Android"
    
    override fun getWeight(): Int = 1 // Higher weight = appears first
    
    /**
     * This is the key method! It's called to create the wizard UI.
     * We return our Compose UI step here.
     */
    override fun getCustomOptionsStep(context: WizardContext, parentDisposable: Disposable): ModuleWizardStep {
        composeStep = ComposeMultiplatformWizardStep(moduleBuilder)
        return ComposeMultiplatformProjectWizardStep(composeStep!!, moduleBuilder, context)
    }
    
    override fun setupRootModel(modifiableRootModel: ModifiableRootModel) {
        // Project creation is handled by ComposeMultiplatformModuleBuilder
        // after wizard completes
    }
    
    /**
     * Wrapper step that embeds Compose UI into IntelliJ wizard framework.
     */
    private class ComposeMultiplatformProjectWizardStep(
        private val composeStep: ComposeMultiplatformWizardStep,
        private val moduleBuilder: ComposeMultiplatformModuleBuilder,
        private val context: WizardContext
    ) : ModuleWizardStep() {
        
        override fun getComponent(): JComponent {
            return com.intellij.ui.dsl.builder.panel {
                row {
                    cell(composeStep.component)
                        .align(com.intellij.ui.dsl.builder.AlignX.FILL)
                        .align(com.intellij.ui.dsl.builder.AlignY.FILL)
                        .resizableColumn()
                }.resizableRow()
                    .topGap(com.intellij.ui.dsl.builder.TopGap.NONE)
            }.apply {
                border = com.intellij.util.ui.JBUI.Borders.empty(10, 15, 10, 15)
            }
        }
        
        override fun updateDataModel() {
            composeStep.updateDataModel()
            
            // Update wizard context
            context.projectName = composeStep.getProjectName()
            context.setProjectFileDirectory(
                java.nio.file.Path.of(composeStep.getProjectPath()).resolve(composeStep.getProjectName()),
                false
            )
        }
        
        override fun validate(): Boolean {
            return composeStep.validate()
        }
    }
}

