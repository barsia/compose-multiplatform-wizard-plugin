package io.github.heisiar.composewizard.shared.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.Tooltip

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BundledHotReloadItem(version: String) {
    if (version.isEmpty()) {
        SkeletonText()
        return
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Tooltip(tooltip = { Text("Bundled in Compose and cannot be disabled") }) {
            org.jetbrains.jewel.ui.component.CheckboxRow(
                checked = true,
                onCheckedChange = { },
                enabled = false
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Hot Reload",
                        style = JewelTheme.defaultTextStyle,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        maxLines = 1
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    Box(
                        modifier = Modifier.size(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BundledLibraryIndicator()
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = version,
                style = JewelTheme.defaultTextStyle,
                color = JewelTheme.globalColors.text.normal.copy(alpha = 0.6f)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            LibraryVersionCopyIcon(version = version)
        }
    }
}

