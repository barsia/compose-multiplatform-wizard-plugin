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
    var enableDevVersionsSetByUser: Boolean = false  // Track if user explicitly changed this
    var devCheckboxVisibleByUser: Boolean = false
    var welcomeTooltipShown: Boolean = false
    
    /**
     * Optional GitHub Personal Access Token (fine-grained) for higher API rate limits.
     * 
     * Without token: 60 requests/hour (shared across all apps)
     * With token: 5000 requests/hour
     * 
     * Token permissions needed: public_repo (read-only)
     * Create token at: https://github.com/settings/tokens?type=beta
     * 
     * Leave empty to use anonymous access.
     */
    var githubToken: String = ""
    
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

