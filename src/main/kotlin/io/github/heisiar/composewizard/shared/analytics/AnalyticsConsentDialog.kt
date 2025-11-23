package io.github.heisiar.composewizard.shared.analytics

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.DialogWrapper
import io.github.heisiar.composewizard.shared.settings.WizardSettings
import org.jetbrains.jewel.foundation.ExperimentalJewelApi
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icon.PathIconKey
import java.awt.Cursor
import java.awt.Dimension
import javax.swing.JComponent

/**
 * GDPR-compliant consent dialog for analytics.
 * Built with Jewel for modern, native IDE look.
 * Uses DialogWrapper for platform integration, but Jewel buttons inside content.
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
        setResizable(false)
        init()
    }
    
    @OptIn(ExperimentalJewelApi::class)
    override fun createCenterPanel(): JComponent {
        return ComposePanel().apply {
            preferredSize = Dimension(620, 360) // Increased height for two-column layout
            setContent {
                org.jetbrains.jewel.bridge.theme.SwingBridgeTheme {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 4.dp)
                            .focusable(false)
                            .semantics(mergeDescendants = false) {
                                // Dialog content is readable but not focusable
                            }
                    ) {
                        // Main text content
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusable(false),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusable(false),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        key = PathIconKey("icons/plugin-logo.svg", AnalyticsConsentDialog::class.java),
                                        contentDescription = "Compose Multiplatform Wizard Plugin Logo",
                                        modifier = Modifier
                                            .size(56.dp)
                                            .focusable(false)
                                            .align(Alignment.Top),
                                        tint = androidx.compose.ui.graphics.Color.Unspecified
                                    )
                                    
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .focusable(false),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            "Help us improve the Compose Multiplatform Wizard plugin",
                                            fontSize = 16.sp,
                                            color = JewelTheme.globalColors.text.normal,
                                            lineHeight = 22.sp
                                        )
                                        
                                        Text(
                                            "By sharing anonymous usage statistics, you help us understand which features are most valuable and identify areas for improvement.",
                                            fontSize = 13.sp,
                                            color = JewelTheme.globalColors.text.normal.copy(alpha = 0.7f),
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                                
// Two-column layout for data collection info with colored icons
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusable(false),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Column 1: We Collect
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .focusable(false),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            "We Collect",
                                            fontSize = 13.sp,
                                            color = JewelTheme.globalColors.text.normal,
                                            lineHeight = 18.sp
                                        )
                                        Row(
                                            modifier = Modifier.focusable(false),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "✓",
                                                fontSize = 13.sp,
                                                color = androidx.compose.ui.graphics.Color(0xFF59A869) // Green
                                            )
                                            Text(
                                                "Platform selection",
                                                fontSize = 13.sp,
                                                color = JewelTheme.globalColors.text.normal.copy(alpha = 0.7f),
                                                lineHeight = 18.sp
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.focusable(false),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "✓",
                                                fontSize = 13.sp,
                                                color = androidx.compose.ui.graphics.Color(0xFF59A869) // Green
                                            )
                                            Text(
                                                "Features used",
                                                fontSize = 13.sp,
                                                color = JewelTheme.globalColors.text.normal.copy(alpha = 0.7f),
                                                lineHeight = 18.sp
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.focusable(false),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "✓",
                                                fontSize = 13.sp,
                                                color = androidx.compose.ui.graphics.Color(0xFF59A869) // Green
                                            )
                                            Text(
                                                "Plugin version",
                                                fontSize = 13.sp,
                                                color = JewelTheme.globalColors.text.normal.copy(alpha = 0.7f),
                                                lineHeight = 18.sp
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.focusable(false),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "✓",
                                                fontSize = 13.sp,
                                                color = androidx.compose.ui.graphics.Color(0xFF59A869) // Green
                                            )
                                            Text(
                                                "IDE version",
                                                fontSize = 13.sp,
                                                color = JewelTheme.globalColors.text.normal.copy(alpha = 0.7f),
                                                lineHeight = 18.sp
                                            )
                                        }
                                    }
                                    
                                    // Column 2: We Do NOT Collect
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .focusable(false),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            "We Do NOT Collect",
                                            fontSize = 13.sp,
                                            color = JewelTheme.globalColors.text.normal,
                                            lineHeight = 18.sp
                                        )
                                        Row(
                                            modifier = Modifier.focusable(false),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "✗",
                                                fontSize = 13.sp,
                                                color = androidx.compose.ui.graphics.Color(0xFFE05555) // Red
                                            )
                                            Text(
                                                "Personal information",
                                                fontSize = 13.sp,
                                                color = JewelTheme.globalColors.text.normal.copy(alpha = 0.7f),
                                                lineHeight = 18.sp
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.focusable(false),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "✗",
                                                fontSize = 13.sp,
                                                color = androidx.compose.ui.graphics.Color(0xFFE05555) // Red
                                            )
                                            Text(
                                                "Source code",
                                                fontSize = 13.sp,
                                                color = JewelTheme.globalColors.text.normal.copy(alpha = 0.7f),
                                                lineHeight = 18.sp
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.focusable(false),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "✗",
                                                fontSize = 13.sp,
                                                color = androidx.compose.ui.graphics.Color(0xFFE05555) // Red
                                            )
                                            Text(
                                                "Project names",
                                                fontSize = 13.sp,
                                                color = JewelTheme.globalColors.text.normal.copy(alpha = 0.7f),
                                                lineHeight = 18.sp
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.focusable(false),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "✗",
                                                fontSize = 13.sp,
                                                color = androidx.compose.ui.graphics.Color(0xFFE05555) // Red
                                            )
                                            Text(
                                                "IP addresses",
                                                fontSize = 13.sp,
                                                color = JewelTheme.globalColors.text.normal.copy(alpha = 0.7f),
                                                lineHeight = 18.sp
                                            )
                                        }
                                    }
                                }
                                                                
                                Text(
                                    "All data is anonymized and used solely to improve the plugin experience.",
                                    fontSize = 13.sp,
                                    color = JewelTheme.globalColors.text.normal.copy(alpha = 0.7f),
                                    lineHeight = 18.sp
                                )
                                
                                Text(
                                    "You can always change this behavior in Settings | Tools | Compose Multiplatform Wizard.",
                                    fontSize = 13.sp,
                                    color = JewelTheme.globalColors.text.normal.copy(alpha = 0.6f),
                                    lineHeight = 18.sp
                                )
                                
                                Text(
                                    "The plugin operates fully regardless of your consent choice.",
                                    fontSize = 13.sp,
                                    color = JewelTheme.globalColors.text.normal.copy(alpha = 0.6f),
                                    lineHeight = 18.sp
                                )
                            }
                        
                        // Push privacy policy to bottom
                        Spacer(modifier = Modifier
                            .weight(1f)
                            .focusable(false)
                        )
                        
                        // Privacy Policy link
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .focusable(false),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Privacy Policy",
                                fontSize = 12.sp,
                                color = androidx.compose.ui.graphics.Color(0xFF589DF6), // IntelliJ link blue
                                modifier = Modifier
                                    .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)))
                                    .                                    pointerInput(Unit) {
                                        detectTapGestures {
                                            BrowserUtil.browse("https://github.com/heisiar/compose-multiplatform-wizard-project/blob/main/PRIVACY.md")
                                        }
                                    }
                                    .semantics {
                                        contentDescription = "Link to Privacy Policy"
                                    }
                            )
                        }
                        
                        // Jewel buttons - the only focusable elements
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusable(false),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    handleDecline()
                                }
                            ) {
                                Text("Don't Send")
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp).focusable(false))
                            
                            DefaultButton(
                                onClick = {
                                    handleAgree()
                                }
                            ) {
                                Text("Send Anonymous Statistics")
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Hide standard DialogWrapper buttons
    override fun createActions(): Array<javax.swing.Action> {
        return emptyArray()
    }
    
    private fun handleAgree() {
        val settings = WizardSettings.getInstance()
        settings.analyticsConsentGiven = true
        settings.analyticsEnabled = true
        settings.analyticsConsentDialogShown = true
        close(OK_EXIT_CODE)
    }
    
    private fun handleDecline() {
        val settings = WizardSettings.getInstance()
        settings.analyticsConsentGiven = false
        settings.analyticsEnabled = false
        settings.analyticsConsentDialogShown = true
        close(CANCEL_EXIT_CODE)
    }
    
    override fun doCancelAction() {
        // X button or ESC -> decline
        handleDecline()
        super.doCancelAction()
    }
    
    companion object {
        /**
         * Show consent dialog if not shown before.
         * Call this when wizard is first opened.
         */
        @JvmStatic
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
