package io.github.heisiar.composewizard.shared.ui

import androidx.compose.runtime.*
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.material3.MaterialTheme
import java.awt.Dimension
import javax.swing.JComponent

/**
 * Creates a Swing component containing Compose UI.
 * Can be used in both IntelliJ IDEA and Android Studio.
 */
fun createComposeWizardPanel(
    data: MutableState<WizardData>,
    showAndroidOption: Boolean = true
): JComponent {
    val panel = ComposePanel()
    panel.preferredSize = Dimension(600, 500)
    
    panel.setContent {
        MaterialTheme {
            ComposeWizardUI(
                data = data.value,
                onDataChange = { data.value = it },
                showAndroidOption = showAndroidOption
            )
        }
    }
    
    return panel
}

