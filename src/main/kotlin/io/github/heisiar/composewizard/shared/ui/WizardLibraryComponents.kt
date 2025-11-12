package io.github.heisiar.composewizard.shared.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text

@Composable
fun BackgroundPlatformIcon(
    active: Boolean,
    icon: String,
    color: Color,
    isApple: Boolean = false,
    isGlobe: Boolean = false
) {
    val targetBackgroundAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (active && !isApple && !isGlobe) 0.25f else 0.04f,
        animationSpec = tween(300),
        label = "backgroundAlpha"
    )
    
    val targetIconAlpha by androidx.compose.animation.core.animateFloatAsState(
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

@Composable
fun SkeletonText(
    modifier: Modifier = Modifier,
    width: Dp? = null,
    height: Dp = 28.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.03f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeletonAlpha"
    )
    
    val skeletonColor = if (isDarkTheme()) {
        Color.White.copy(alpha = alpha)
    } else {
        Color.Gray.copy(alpha = alpha)
    }
    
    Box(
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .height(height)
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(skeletonColor)
    )
}

@androidx.compose.foundation.ExperimentalFoundationApi
@Composable
fun LibraryVersionCopyIcon(version: String) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()
    var isCopied by remember { mutableStateOf(false) }
    
    LaunchedEffect(isCopied) {
        if (isCopied) {
            kotlinx.coroutines.delay(1500)
            isCopied = false
        }
    }
    
    val focusBorderColor = Color(0xFF3574F0)
    
    Box(
        modifier = Modifier
            .size(20.dp)
            .border(
                width = if (isFocused) 1.dp else 0.dp,
                color = if (isFocused) focusBorderColor else Color.Transparent,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .onKeyEvent { keyEvent ->
                    when {
                        (keyEvent.key == Key.Enter || keyEvent.key == Key.Spacebar) && 
                        keyEvent.type == KeyEventType.KeyDown -> {
                            val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
                            val stringSelection = java.awt.datatransfer.StringSelection(version)
                            clipboard.setContents(stringSelection, null)
                            isCopied = true
                            true
                        }
                        else -> false
                    }
                }
                .clickable(
                    indication = null,
                    interactionSource = interactionSource
                ) {
                    val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
                    val stringSelection = java.awt.datatransfer.StringSelection(version)
                    clipboard.setContents(stringSelection, null)
                    isCopied = true
                }
                .pointerHoverIcon(PointerIcon(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                key = if (isCopied) org.jetbrains.jewel.ui.icons.AllIconsKeys.Actions.Checked 
                     else org.jetbrains.jewel.ui.icons.AllIconsKeys.Actions.Copy,
                contentDescription = if (isCopied) "Copied" else "Copy",
                modifier = Modifier.size(14.dp),
                tint = if (isCopied) {
                    JewelTheme.globalColors.text.info
                } else {
                    JewelTheme.globalColors.text.normal.copy(
                        alpha = if (isHovered) 0.8f else 0.5f
                    )
                }
            )
        }
    }
}

