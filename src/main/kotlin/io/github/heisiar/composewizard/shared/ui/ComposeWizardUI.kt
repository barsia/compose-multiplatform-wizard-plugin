package io.github.heisiar.composewizard.shared.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ComposeWizardUI(
    data: WizardData,
    onDataChange: (WizardData) -> Unit,
    showAndroidOption: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Create Compose Multiplatform Project",
            style = MaterialTheme.typography.headlineSmall
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Project name
        OutlinedTextField(
            value = data.projectName,
            onValueChange = { onDataChange(data.copy(projectName = it)) },
            label = { Text("Project name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        // Project location
        OutlinedTextField(
            value = data.projectLocation,
            onValueChange = { onDataChange(data.copy(projectLocation = it)) },
            label = { Text("Project location") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        // Package name
        OutlinedTextField(
            value = data.packageName,
            onValueChange = { onDataChange(data.copy(packageName = it)) },
            label = { Text("Package name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Platforms section
        Text(
            text = "Target platforms",
            style = MaterialTheme.typography.titleMedium
        )
        
        Column(
            modifier = Modifier.padding(start = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (showAndroidOption) {
                CheckboxRow(
                    checked = data.includeAndroid,
                    onCheckedChange = { onDataChange(data.copy(includeAndroid = it)) },
                    label = "Android"
                )
            }
            
            CheckboxRow(
                checked = data.includeIos,
                onCheckedChange = { onDataChange(data.copy(includeIos = it)) },
                label = "iOS"
            )
            
            CheckboxRow(
                checked = data.includeDesktop,
                onCheckedChange = { onDataChange(data.copy(includeDesktop = it)) },
                label = "Desktop (JVM)"
            )
            
            CheckboxRow(
                checked = data.includeWeb,
                onCheckedChange = { onDataChange(data.copy(includeWeb = it)) },
                label = "Web (Kotlin/Wasm & Kotlin/JS)"
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Additional options
        Text(
            text = "Additional options",
            style = MaterialTheme.typography.titleMedium
        )
        
        Column(
            modifier = Modifier.padding(start = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CheckboxRow(
                checked = data.includeTests,
                onCheckedChange = { onDataChange(data.copy(includeTests = it)) },
                label = "Include sample tests"
            )
            
            CheckboxRow(
                checked = data.enableDevVersions,
                onCheckedChange = { onDataChange(data.copy(enableDevVersions = it)) },
                label = "Enable development versions"
            )
        }
    }
}

@Composable
private fun CheckboxRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label)
    }
}

