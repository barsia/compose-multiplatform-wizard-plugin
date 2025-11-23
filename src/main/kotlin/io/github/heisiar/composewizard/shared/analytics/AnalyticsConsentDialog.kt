package io.github.heisiar.composewizard.shared.analytics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.DialogWrapper
import io.github.heisiar.composewizard.shared.settings.WizardSettings
import org.jetbrains.jewel.foundation.ExperimentalJewelApi
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icon.PathIconKey
import java.awt.Cursor
import java.awt.Dimension
import javax.swing.JComponent

/**
 * GDPR-compliant consent dialog for analytics.
 * Built with Jewel for modern, native IDE look.
 * 
 * Shown on first plugin use, explains:
 * - What data is collected
 * - Why it's collected
 * - How to disable it
 * - Link to Privacy Policy
 */
class AnalyticsConsentDialog : DialogWrapper(null, true) {
    
    init {
        title = "Welcome to Compose Multiplatform Wizard"
        setOKButtonText("Agree")
        setCancelButtonText("Continue without Analytics")
        setResizable(false) // Disable fullscreen/resize button
        init()
    }
    
    @OptIn(ExperimentalJewelApi::class)
    override fun createCenterPanel(): JComponent {
        return ComposePanel().apply {
            preferredSize = Dimension(620, 250)
            setContent {
                org.jetbrains.jewel.bridge.theme.SwingBridgeTheme {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        SelectionContainer {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Header with icon and intro
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        key = PathIconKey("icons/plugin-logo.svg", AnalyticsConsentDialog::class.java),
                                        contentDescription = "Plugin",
                                        modifier = Modifier.size(56.dp),
                                        tint = androidx.compose.ui.graphics.Color.Unspecified
                                    )
                                    
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Help us improve the Compose Multiplatform Wizard",
                                            fontSize = 16.sp,
                                            color = JewelTheme.globalColors.text.normal,
                                            lineHeight = 22.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "We collect anonymous usage data to understand how the plugin is used and prioritize improvements.",
                                            fontSize = 13.sp,
                                            color = JewelTheme.globalColors.text.normal.copy(alpha = 0.7f),
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                                
                                // Two columns for what we collect/don't collect
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                                ) {
                                    // Left: What we collect
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            "✓ We collect",
                                            fontSize = 14.sp,
                                            color = androidx.compose.ui.graphics.Color(0xFF4CAF50)
                                        )
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Text("• Selected platforms", fontSize = 12.sp, lineHeight = 17.sp)
                                            Text("• Plugin and IDE version", fontSize = 12.sp, lineHeight = 17.sp)
                                            Text("• Anonymous usage patterns", fontSize = 12.sp, lineHeight = 17.sp)
                                        }
                                    }
                                    
                                    // Right: What we DON'T collect
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            "✗ We do NOT collect",
                                            fontSize = 14.sp,
                                            color = androidx.compose.ui.graphics.Color(0xFFFF9800)
                                        )
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Text("• Personal information", fontSize = 12.sp, lineHeight = 17.sp)
                                            Text("• Source code or project names", fontSize = 12.sp, lineHeight = 17.sp)
                                            Text("• IP addresses", fontSize = 12.sp, lineHeight = 17.sp)
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Push note to bottom
                        Spacer(modifier = Modifier.weight(1f))
                        
                        // Important note - at the bottom, above action buttons
                        Text(
                            "Note: The plugin operates fully regardless of your consent choice.",
                            fontSize = 12.sp,
                            color = JewelTheme.globalColors.text.normal.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        // Footer row: Settings path on left, Privacy Policy on right
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Change anytime: Settings → Tools → Compose Multiplatform Wizard",
                                fontSize = 12.sp,
                                color = JewelTheme.globalColors.text.normal.copy(alpha = 0.6f)
                            )
                            Text(
                                "Privacy Policy",
                                fontSize = 12.sp,
                                color = JewelTheme.globalColors.text.normal.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)))
                                    .clickable {
                                        BrowserUtil.browse("https://github.com/heisiar/compose-multiplatform-wizard-plugin/blob/main/PRIVACY.md")
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
    
    override fun doOKAction() {
        // User clicked "Agree" - enable analytics
        val settings = WizardSettings.getInstance()
        settings.analyticsConsentGiven = true
        settings.analyticsEnabled = true
        settings.analyticsConsentDialogShown = true
        super.doOKAction()
    }
    
    override fun doCancelAction() {
        // User clicked "Continue without Analytics" - disable analytics
        val settings = WizardSettings.getInstance()
        settings.analyticsConsentGiven = false
        settings.analyticsEnabled = false
        settings.analyticsConsentDialogShown = true
        super.doCancelAction()
    }
    
    companion object {
        /**
         * Show consent dialog if not shown before.
         * Call this when wizard is first opened.
         */
        fun showIfNeeded() {
            val settings = WizardSettings.getInstance()
            if (!settings.analyticsConsentDialogShown) {
                ApplicationManager.getApplication().invokeLater {
                    AnalyticsConsentDialog().show()
                }
            }
        }
    }
}
