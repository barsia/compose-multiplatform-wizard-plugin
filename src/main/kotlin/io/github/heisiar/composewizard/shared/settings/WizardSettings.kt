package io.github.heisiar.composewizard.shared.settings

import com.intellij.openapi.components.*
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "ComposeMultiplatformWizardSettings",
    storages = [Storage("composeMultiplatformWizard.xml")]
)
@Service(Service.Level.APP)
class WizardSettings : PersistentStateComponent<WizardSettings> {
    
    var enableDevVersions: Boolean = false
    var devCheckboxVisibleByUser: Boolean = false
    var welcomeTooltipShown: Boolean = false
    
    override fun getState(): WizardSettings {
        return this
    }
    
    override fun loadState(state: WizardSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }
    
    companion object {
        @JvmStatic
        fun getInstance(): WizardSettings {
            return service()
        }
    }
}

