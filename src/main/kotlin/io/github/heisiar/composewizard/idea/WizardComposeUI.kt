package io.github.heisiar.composewizard.idea

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.heisiar.composewizard.shared.models.ComposeMultiplatformModuleBuilder
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.*

/**
 * Main Compose UI content for IntelliJ IDEA wizard using Jewel components
 */
@Composable
fun WizardMainContent(
    step: ComposeWizardStep,
    builder: ComposeMultiplatformModuleBuilder
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JewelTheme.globalColors.panelBackground)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Create Compose Multiplatform Project",
                style = JewelTheme.defaultTextStyle.copy(
                    fontSize = JewelTheme.defaultTextStyle.fontSize * 1.2f
                )
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text("This is a placeholder UI - full Jewel implementation coming soon")
            
            Text("Name: ${step.name}")
            Text("Location: ${step.path}")
            Text("Package: ${builder.projectId}")
        }
    }
}







