package io.github.heisiar.composewizard.shared.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import java.awt.FlowLayout
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Settings UI for Compose Multiplatform Wizard plugin.
 * Accessible via: Settings → Tools → Compose Multiplatform Wizard
 */
class WizardSettingsConfigurable : Configurable {
    
    private var mainPanel: JPanel? = null
    private var analyticsCheckbox: JBCheckBox? = null
    
    override fun getDisplayName(): String {
        return "Compose Multiplatform Wizard"
    }
    
    override fun createComponent(): JComponent {
        val settings = WizardSettings.getInstance()
        
        analyticsCheckbox = JBCheckBox("Enable anonymous usage statistics").apply {
            isSelected = settings.analyticsEnabled
            toolTipText = "Help improve the plugin by sharing anonymous usage data"
            addMouseListener(object : java.awt.event.MouseAdapter() {
                override fun mouseEntered(e: java.awt.event.MouseEvent?) {
                    cursor = java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)
                }
                override fun mouseExited(e: java.awt.event.MouseEvent?) {
                    cursor = java.awt.Cursor.getDefaultCursor()
                }
            })
        }
        
        // Privacy info with clickable link
        val descriptionLabel = JBLabel("<html><small>We collect anonymous usage statistics to improve the plugin.</small></html>").apply {
            border = JBUI.Borders.emptyLeft(30)
        }
        
        val linkPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
            border = JBUI.Borders.emptyLeft(32)
            add(JBLabel("<html><small>No personal data is collected. </small></html>"))
            add(HyperlinkLabel("Privacy Policy").apply {
                setHyperlinkTarget("https://github.com/heisiar/compose-multiplatform-wizard-plugin/blob/main/PRIVACY.md")
                font = JBUI.Fonts.smallFont()
            })
        }
        
        mainPanel = FormBuilder.createFormBuilder()
            .addComponent(JBLabel("<html><b>Analytics</b></html>"))
            .addComponent(analyticsCheckbox!!)
            .addComponent(descriptionLabel)
            .addComponent(linkPanel)
            .addComponentFillVertically(JPanel(), 0)
            .panel
        
        return mainPanel!!
    }
    
    override fun isModified(): Boolean {
        val settings = WizardSettings.getInstance()
        return analyticsCheckbox?.isSelected != settings.analyticsEnabled
    }
    
    override fun apply() {
        val settings = WizardSettings.getInstance()
        
        val wasAnalyticsEnabled = settings.analyticsEnabled
        val newAnalyticsEnabled = analyticsCheckbox?.isSelected ?: false
        
        settings.analyticsEnabled = newAnalyticsEnabled
        
        // If user enables analytics but hasn't given consent yet, keep it disabled
        // Consent dialog will show on next wizard launch
        if (newAnalyticsEnabled && !settings.analyticsConsentGiven) {
            settings.analyticsEnabled = false
        }
        
        // If user disables analytics, revoke consent
        if (wasAnalyticsEnabled && !newAnalyticsEnabled) {
            settings.analyticsConsentGiven = false
        }
    }
    
    override fun reset() {
        val settings = WizardSettings.getInstance()
        analyticsCheckbox?.isSelected = settings.analyticsEnabled
    }
    
    override fun disposeUIResources() {
        mainPanel = null
        analyticsCheckbox = null
    }
}

