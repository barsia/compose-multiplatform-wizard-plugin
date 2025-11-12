package io.github.heisiar.composewizard.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.JBUI
import org.jetbrains.jewel.ui.component.Text
import java.awt.Point
import javax.swing.UIManager

fun isDarkTheme(): Boolean {
    val background = UIManager.getColor("Panel.background")
    if (background != null) {
        val brightness = background.red + background.green + background.blue
        return brightness < 383
    }
    return false
}

@Composable
fun ValidationPopupJB(
    mainPanel: ComposePanel,
    message: String,
    isWarning: Boolean
) {
    val isDark = isDarkTheme()
    
    val bgColor = if (isWarning) {
        if (isDark) {
            Color(0xFF3D3223)
        } else {
            Color(0xFFF5F0E6)
        }
    } else {
        if (isDark) {
            Color(0xFF5E3838)
        } else {
            Color(0xFFFFF2F3)
        }
    }
    
    val borderComposeColor = if (isWarning) {
        if (isDark) {
            Color(0xFF5E4D33)
        } else {
            Color(0xFFE0CEA8)
        }
    } else {
        if (isDark) {
            Color(0xFFBD5757)
        } else {
            Color(0xFFED99A1)
        }
    }
    
    val textColor = if (isDark) {
        Color(0xFFFFFFFF)
    } else {
        Color(0xFF000000)
    }
    
    val bgColorInt = (bgColor.value shr 32).toInt()
    val borderColorInt = (borderComposeColor.value shr 32).toInt()
    
    val backgroundColor = java.awt.Color(
        (bgColorInt shr 16) and 0xFF,
        (bgColorInt shr 8) and 0xFF,
        bgColorInt and 0xFF,
        (bgColorInt shr 24) and 0xFF
    )
    
    val borderColor = java.awt.Color(
        (borderColorInt shr 16) and 0xFF,
        (borderColorInt shr 8) and 0xFF,
        borderColorInt and 0xFF,
        (borderColorInt shr 24) and 0xFF
    )
    
    val textColorInt = (textColor.value shr 32).toInt()
    val textColorAWT = java.awt.Color(
        (textColorInt shr 16) and 0xFF,
        (textColorInt shr 8) and 0xFF,
        textColorInt and 0xFF
    )
    
    DisposableEffect(message, isWarning) {
        var popup: com.intellij.openapi.ui.popup.JBPopup? = null
        
        val tipComponent = javax.swing.JLabel().apply {
            text = message
            foreground = textColorAWT
            font = com.intellij.util.ui.JBUI.Fonts.label()
            isOpaque = true
            background = backgroundColor
            border = javax.swing.border.CompoundBorder(
                javax.swing.border.LineBorder(borderColor, JBUI.scale(1), false),
                JBUI.Borders.empty(5, 9)
            )
        }
        
        popup = com.intellij.openapi.ui.popup.JBPopupFactory.getInstance()
            .createComponentPopupBuilder(tipComponent, null)
            .setCancelOnClickOutside(false)
            .setCancelOnWindowDeactivation(false)
            .setShowShadow(true)
            .setShowBorder(false)
            .createPopup()
        
        val popupSize = tipComponent.preferredSize
        val point = Point(
            JBUI.scale(0),
            -JBUI.scale(0) - popupSize.height/2
        )
        
        popup.show(RelativePoint(mainPanel, point))
        
        if (popup is com.intellij.ui.popup.AbstractPopup) {
            try {
                val window = popup.popupWindow
                if (window != null && com.intellij.ui.WindowRoundedCornersManager.isAvailable()) {
                    com.intellij.ui.WindowRoundedCornersManager.setRoundedCorners(
                        window,
                        borderColor
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        onDispose {
            popup.cancel()
        }
    }
}

@Composable
fun ValidationPopup(
    message: String,
    isWarning: Boolean
) {
    val isDark = isDarkTheme()
    
    val backgroundColor = if (isWarning) {
        if (isDark) {
            Color(0xFF3D3223)
        } else {
            Color(0xFFF5F0E6)
        }
    } else {
        if (isDark) {
            Color(0xFF5E3838)
        } else {
            Color(0xFFFFF2F3)
        }
    }
    
    val borderColor = if (isWarning) {
        if (isDark) {
            Color(0xFF5E4D33)
        } else {
            Color(0xFFE0CEA8)
        }
    } else {
        if (isDark) {
            Color(0xFFBD5757)
        } else {
            Color(0xFFED99A1)
        }
    }
    
    val textColor = if (isDark) {
        Color(0xFFFFFFFF)
    } else {
        Color(0xFF000000)
    }
    
    Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset(0, -60),
        properties = PopupProperties(
            focusable = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .background(backgroundColor, RoundedCornerShape(8.dp))
                .border(1.dp, borderColor, RoundedCornerShape(8.dp))
        ) {
            Text(
                text = message,
                color = textColor,
                fontSize = 13.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
                modifier = Modifier.padding(vertical = 6.dp, horizontal = 10.dp),
                lineHeight = 18.sp
            )
        }
    }
}
