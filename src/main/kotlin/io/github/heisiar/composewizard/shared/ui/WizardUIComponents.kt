package io.github.heisiar.composewizard.shared.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.JBUI
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Checkbox
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import java.awt.Cursor
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
fun ValidationPopup(
    message: String,
    isWarning: Boolean
) {
    val surface = JewelTheme.globalColors.panelBackground
    val errorColor = JewelTheme.globalColors.text.error
    
    fun Color.red() = ((value shr 16) and 0xFFu).toFloat() / 255f
    fun Color.green() = ((value shr 8) and 0xFFu).toFloat() / 255f
    fun Color.blue() = (value and 0xFFu).toFloat() / 255f
    
    val backgroundColor = if (isWarning) {
        Color(
            red = surface.red() * 0.85f + errorColor.red() * 0.15f,
            green = surface.green() * 0.85f + errorColor.green() * 0.15f,
            blue = surface.blue() * 0.85f + errorColor.blue() * 0.15f,
            alpha = 0.95f
        )
    } else {
        Color(
            red = surface.red() * 0.7f + errorColor.red() * 0.3f,
            green = surface.green() * 0.7f + errorColor.green() * 0.3f,
            blue = surface.blue() * 0.7f + errorColor.blue() * 0.3f,
            alpha = 0.95f
        )
    }
    
    val borderColor = if (isWarning) {
        JewelTheme.globalColors.text.error.copy(alpha = 0.6f)
    } else {
        JewelTheme.globalColors.text.error.copy(alpha = 0.8f)
    }
    
    val textColor = if (isDarkTheme()) {
        Color.White
    } else {
        Color(0xFF1E1E1E)
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
                .border(1.5.dp, borderColor, RoundedCornerShape(8.dp))
        ) {
            Text(
                text = message,
                color = textColor,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 6.dp, horizontal = 10.dp),
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun PlatformCheckbox(
    checked: Boolean,
    label: String,
    onCheckedChange: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = {},
            modifier = Modifier.size(20.dp),
            enabled = false
        )
        Text(label)
    }
}

val FolderIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Folder",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = androidx.compose.ui.graphics.SolidColor(Color.White)) {
            moveTo(20f, 6f)
            horizontalLineTo(12f)
            lineTo(10f, 4f)
            horizontalLineTo(4f)
            curveTo(2.9f, 4f, 2.01f, 4.9f, 2.01f, 6f)
            lineTo(2f, 18f)
            curveTo(2f, 19.1f, 2.9f, 20f, 4f, 20f)
            horizontalLineTo(20f)
            curveTo(21.1f, 20f, 22f, 19.1f, 22f, 18f)
            verticalLineTo(8f)
            curveTo(22f, 6.9f, 21.1f, 6f, 20f, 6f)
            close()
        }
    }.build()

val GlobeIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Globe",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = androidx.compose.ui.graphics.SolidColor(Color.White)) {
            moveTo(3.51211712f, 15f)
            lineTo(8.17190229f, 15f)
            curveTo(8.05949197f, 14.0523506f, 8f, 13.0444554f, 8f, 12f)
            curveTo(8f, 10.9555446f, 8.05949197f, 9.94764942f, 8.17190229f, 9f)
            lineTo(3.51211712f, 9f)
            curveTo(3.18046266f, 9.93833678f, 3f, 10.9480937f, 3f, 12f)
            curveTo(3f, 13.0519063f, 3.18046266f, 14.0616632f, 3.51211712f, 15f)
            close()
            moveTo(3.93551965f, 16f)
            curveTo(5.12590433f, 18.3953444f, 7.35207678f, 20.1851177f, 10.0280093f, 20.783292f)
            curveTo(9.24889451f, 19.7227751f, 8.65216136f, 18.0371362f, 8.31375067f, 16f)
            close()
            moveTo(20.4878829f, 15f)
            curveTo(20.8195373f, 14.0616632f, 21f, 13.0519063f, 21f, 12f)
            curveTo(21f, 10.9480937f, 20.8195373f, 9.93833678f, 20.4878829f, 9f)
            lineTo(15.8280977f, 9f)
            curveTo(15.940508f, 9.94764942f, 16f, 10.9555446f, 16f, 12f)
            curveTo(16f, 13.0444554f, 15.940508f, 14.0523506f, 15.8280977f, 15f)
            close()
            moveTo(20.0644804f, 16f)
            lineTo(15.6862493f, 16f)
            curveTo(15.3478386f, 18.0371362f, 14.7511055f, 19.7227751f, 13.9719907f, 20.783292f)
            curveTo(16.6479232f, 20.1851177f, 18.8740957f, 18.3953444f, 20.0644804f, 16f)
            close()
            moveTo(9.18440269f, 15f)
            lineTo(14.8155973f, 15f)
            curveTo(14.9340177f, 14.0623882f, 15f, 13.0528256f, 15f, 12f)
            curveTo(15f, 10.9471744f, 14.9340177f, 9.93761183f, 14.8155973f, 9f)
            lineTo(9.18440269f, 9f)
            curveTo(9.06598229f, 9.93761183f, 9f, 10.9471744f, 9f, 12f)
            curveTo(9f, 13.0528256f, 9.06598229f, 14.0623882f, 9.18440269f, 15f)
            close()
            moveTo(9.3349823f, 16f)
            curveTo(9.85717082f, 18.9678295f, 10.9180729f, 21f, 12f, 21f)
            curveTo(13.0819271f, 21f, 14.1428292f, 18.9678295f, 14.6650177f, 16f)
            close()
            moveTo(3.93551965f, 8f)
            lineTo(8.31375067f, 8f)
            curveTo(8.65216136f, 5.96286383f, 9.24889451f, 4.27722486f, 10.0280093f, 3.21670804f)
            curveTo(7.35207678f, 3.81488234f, 5.12590433f, 5.60465556f, 3.93551965f, 8f)
            close()
            moveTo(20.0644804f, 8f)
            curveTo(18.8740957f, 5.60465556f, 16.6479232f, 3.81488234f, 13.9719907f, 3.21670804f)
            curveTo(14.7511055f, 4.27722486f, 15.3478386f, 5.96286383f, 15.6862493f, 8f)
            close()
            moveTo(9.3349823f, 8f)
            lineTo(14.6650177f, 8f)
            curveTo(14.1428292f, 5.03217048f, 13.0819271f, 3f, 12f, 3f)
            curveTo(10.9180729f, 3f, 9.85717082f, 5.03217048f, 9.3349823f, 8f)
            close()
            moveTo(12f, 22f)
            curveTo(6.4771525f, 22f, 2f, 17.5228475f, 2f, 12f)
            curveTo(2f, 6.4771525f, 6.4771525f, 2f, 12f, 2f)
            curveTo(17.5228475f, 2f, 22f, 6.4771525f, 22f, 12f)
            curveTo(22f, 17.5228475f, 17.5228475f, 22f, 12f, 22f)
            close()
        }
    }.build()

val AppleIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Apple",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = androidx.compose.ui.graphics.SolidColor(Color.White)) {
            moveTo(15.5f, 21f)
            curveTo(16.189f, 21f, 17.383f, 19.542f, 18.87f, 16.639f)
            curveTo(17.635f, 15.604f, 17f, 14.384f, 17f, 13f)
            curveTo(17f, 11.644f, 17.609f, 10.445f, 18.797f, 9.424f)
            curveTo(17.963f, 8.464f, 17.037f, 8f, 16f, 8f)
            curveTo(14.906f, 8f, 14.059f, 8.149f, 13.459f, 8.437f)
            curveTo(12.82f, 8.744f, 12.081f, 8.765f, 11.425f, 8.495f)
            curveTo(10.629f, 8.167f, 9.654f, 8f, 8.5f, 8f)
            curveTo(6.434f, 8f, 5f, 10.086f, 5f, 13f)
            curveTo(5f, 15.711f, 7.833f, 21f, 9f, 21f)
            curveTo(9.429f, 21f, 9.918f, 20.842f, 10.466f, 20.514f)
            curveTo(11.652f, 19.804f, 13.14f, 19.836f, 14.294f, 20.595f)
            curveTo(14.71f, 20.868f, 15.109f, 21f, 15.5f, 21f)
            close()
            moveTo(19.8f, 16.1f)
            curveTo(19.993f, 16.245f, 20.056f, 16.508f, 19.947f, 16.724f)
            curveTo(18.187f, 20.246f, 16.783f, 22f, 15.5f, 22f)
            curveTo(14.903f, 22f, 14.316f, 21.807f, 13.744f, 21.431f)
            curveTo(12.911f, 20.882f, 11.836f, 20.859f, 10.98f, 21.371f)
            curveTo(10.287f, 21.786f, 9.627f, 22f, 9f, 22f)
            curveTo(7.113f, 22f, 4f, 16.189f, 4f, 13f)
            curveTo(4f, 9.596f, 5.785f, 7f, 8.5f, 7f)
            curveTo(9.777f, 7f, 10.88f, 7.188f, 11.806f, 7.57f)
            curveTo(12.199f, 7.732f, 12.643f, 7.72f, 13.026f, 7.536f)
            curveTo(13.778f, 7.175f, 14.769f, 7f, 16f, 7f)
            curveTo(17.502f, 7f, 18.811f, 7.748f, 19.9f, 9.2f)
            curveTo(20.065f, 9.421f, 20.021f, 9.734f, 19.8f, 9.9f)
            curveTo(18.585f, 10.811f, 18f, 11.835f, 18f, 13f)
            curveTo(18f, 14.165f, 18.585f, 15.189f, 19.8f, 16.1f)
            close()
            moveTo(12.4995f, 6f)
            curveTo(12.2235f, 6f, 11.9995f, 5.776f, 11.9995f, 5.5f)
            curveTo(11.9995f, 3.567f, 13.5675f, 2f, 15.4995f, 2f)
            curveTo(15.7765f, 2f, 15.9995f, 2.224f, 15.9995f, 2.5f)
            curveTo(15.9995f, 4.433f, 14.4325f, 6f, 12.4995f, 6f)
            close()
            moveTo(14.9365f, 3.064f)
            curveTo(14.0075f, 3.278f, 13.2775f, 4.008f, 13.0635f, 4.936f)
            curveTo(13.9925f, 4.722f, 14.7225f, 3.992f, 14.9365f, 3.064f)
            close()
        }
    }.build()

@Composable
fun BackgroundPlatformIcon(
    active: Boolean,
    icon: String,
    color: Color,
    isApple: Boolean = false,
    isGlobe: Boolean = false
) {
    val targetBackgroundAlpha by animateFloatAsState(
        targetValue = if (active && !isApple && !isGlobe) 0.25f else 0.04f,
        animationSpec = tween(300),
        label = "backgroundAlpha"
    )
    
    val targetIconAlpha by animateFloatAsState(
        targetValue = if (active) 0.9f else 0.15f,
        animationSpec = tween(300),
        label = "iconContentAlpha"
    )
    
    val adaptiveIconColor = if (isApple || isGlobe) {
        if (isDarkTheme()) Color(0xFFFFFFFF) else Color(0xFF000000)
    } else {
        color
    }
    
    Box(
        modifier = Modifier
            .size(80.dp)
            .wrapContentHeight(Alignment.CenterVertically),
        contentAlignment = Alignment.Center
    ) {
        if (!isApple && !isGlobe) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .alpha(targetBackgroundAlpha)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                color.copy(alpha = 0.2f),
                                color.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
        }
        Box(
            modifier = Modifier
                .width(52.dp)
                .height(52.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isApple) {
                Icon(
                    imageVector = AppleIcon,
                    contentDescription = "Apple",
                    tint = adaptiveIconColor,
                    modifier = Modifier
                        .size(48.dp)
                        .alpha(targetIconAlpha)
                )
            } else if (isGlobe) {
                Icon(
                    imageVector = GlobeIcon,
                    contentDescription = "Globe",
                    tint = adaptiveIconColor,
                    modifier = Modifier
                        .size(48.dp)
                        .alpha(targetIconAlpha)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .offset(y = (-2).dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = icon,
                        color = color,
                        fontSize = 44.sp,
                        modifier = Modifier
                            .alpha(targetIconAlpha),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

