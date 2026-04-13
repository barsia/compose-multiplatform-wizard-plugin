package io.github.barsia.composewizard.shared.ui

import com.intellij.ide.IdeBundle
import com.intellij.util.ui.UIUtil
import java.awt.event.KeyEvent
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.SwingUtilities

class WizardButtonManager(
    private val component: JComponent
) {
    private var createButton: JButton? = null
    private var lastButtonState: Boolean? = null
    
    fun updateButtonText() {
        var parent = component.parent
        while (parent != null) {
            val buttons = UIUtil.findComponentsOfType(parent as? JComponent ?: return, JButton::class.java)
            for (button in buttons) {
                if (button.text?.contains("Next") == true || 
                    button.text?.contains("OK") == true || 
                    button.text?.contains("Create") == true) {
                    button.text = com.intellij.openapi.util.text.StringUtil.replace(
                        IdeBundle.message("button.create"), "&", ""
                    )
                    button.mnemonic = KeyEvent.VK_C
                    createButton = button
                    return
                }
            }
            parent = parent.parent
        }
    }
    
    fun updateButtonState(enabled: Boolean) {
        lastButtonState = enabled
        SwingUtilities.invokeLater {
            if (createButton == null) {
                updateButtonText()
            }
            createButton?.isEnabled = enabled
        }
    }
}

