package io.github.heisiar.composewizard.idea

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.offset
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import io.github.heisiar.composewizard.idea.statistics.ComposeWizardUsageCollector
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.WindowRoundedCornersManager
import com.intellij.ui.popup.AbstractPopup
import com.intellij.util.ui.JBUI
import java.awt.Point
import javax.swing.JLabel
import javax.swing.BorderFactory
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import java.awt.Cursor
import com.intellij.ide.IdeBundle
import com.intellij.ide.impl.ProjectUtil
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.ui.UIUtil
import java.awt.Dimension
import java.awt.event.KeyEvent
import java.io.File
import java.nio.file.InvalidPathException
import java.nio.file.Path
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.SwingUtilities
import javax.swing.UIManager
import kotlin.io.path.*

class ComposeMultiplatformWizardStep(
    private val builder: ComposeMultiplatformModuleBuilder
) : ModuleWizardStep() {

    companion object {
        const val DEFAULT_COMPOSE_VERSION = "1.10.0-alpha03"
    }

    // Store values
    private var projectNameValue = suggestUniqueName("ComposeProject", getDefaultProjectPath())
    private var projectPathValue = getDefaultProjectPath()
    private var projectIdValue = "org.example.project"
    private var composeVersionValue = DEFAULT_COMPOSE_VERSION
    private var initGit = true
    private var includeTests = false
    private var targetDesktop = true
    private var targetAndroid = true
    private var targetIOS = true
    private var targetWeb = true
    private var enableDevVersions = false
    
    // FUS: Track wizard start time
    private val wizardStartTime = System.currentTimeMillis()

    // Main panel - Compose-based
    private val mainPanel: ComposePanel by lazy {
        ComposePanel().apply {
            preferredSize = Dimension(250, 500)
            setContent {
                MaterialTheme(
                    colorScheme = createColorSchemeFromIde()
                ) {
                    CreateComposeUI()
                }
            }
        }
    }

    private fun createColorSchemeFromIde(): ColorScheme {
        val panelBg = UIManager.getColor("Panel.background")
        val textColor = UIManager.getColor("Label.foreground")
        val isDark = isDarkTheme()

        return if (isDark) {
            darkColorScheme(
                background = androidx.compose.ui.graphics.Color(
                    panelBg?.red ?: 45,
                    panelBg?.green ?: 45,
                    panelBg?.blue ?: 45
                ),
                surface = androidx.compose.ui.graphics.Color(
                    (panelBg?.red ?: 45) + 10,
                    (panelBg?.green ?: 45) + 10,
                    (panelBg?.blue ?: 45) + 10
                ),
                onBackground = androidx.compose.ui.graphics.Color(
                    textColor?.red ?: 220,
                    textColor?.green ?: 220,
                    textColor?.blue ?: 220
                ),
                onSurface = androidx.compose.ui.graphics.Color(
                    textColor?.red ?: 220,
                    textColor?.green ?: 220,
                    textColor?.blue ?: 220
                )
            )
        } else {
            lightColorScheme(
                background = androidx.compose.ui.graphics.Color(
                    panelBg?.red ?: 255,
                    panelBg?.green ?: 255,
                    panelBg?.blue ?: 255
                ),
                surface = androidx.compose.ui.graphics.Color(
                    (panelBg?.red ?: 255),
                    (panelBg?.green ?: 255),
                    (panelBg?.blue ?: 255)
                ),
                onBackground = androidx.compose.ui.graphics.Color(
                    textColor?.red ?: 0,
                    textColor?.green ?: 0,
                    textColor?.blue ?: 0
                ),
                onSurface = androidx.compose.ui.graphics.Color(
                    textColor?.red ?: 0,
                    textColor?.green ?: 0,
                    textColor?.blue ?: 0
                )
            )
        }
    }

    private fun isDarkTheme(): Boolean {
        val background = UIManager.getColor("Panel.background")
        if (background != null) {
            val brightness = background.red + background.green + background.blue
            return brightness < 383
        }
        return false
    }
    
    @Composable
    private fun ValidationPopupJB(
        message: String,
        isWarning: Boolean
    ) {
        val mainPanelComponent = mainPanel
        
        // Get Material3 colors with softer error color for dark theme
        val surface = MaterialTheme.colorScheme.surface
        val errorColor = MaterialTheme.colorScheme.error
        
        val bgColor = if (isWarning) {
            androidx.compose.ui.graphics.Color(
                red = surface.red * 0.85f + errorColor.red * 0.15f,
                green = surface.green * 0.85f + errorColor.green * 0.15f,
                blue = surface.blue * 0.85f + errorColor.blue * 0.15f,
                alpha = 0.95f
            )
        } else {
            androidx.compose.ui.graphics.Color(
                red = surface.red * 0.7f + errorColor.red * 0.3f,
                green = surface.green * 0.7f + errorColor.green * 0.3f,
                blue = surface.blue * 0.7f + errorColor.blue * 0.3f,
                alpha = 0.95f
            )
        }
        
        val borderComposeColor = if (isWarning) {
            MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
        } else {
            MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
        }
        
        // Use white text for good contrast in dark theme, dark text in light theme
        val textColor = if (isDarkTheme()) {
            androidx.compose.ui.graphics.Color.White
        } else {
            androidx.compose.ui.graphics.Color(0xFF1E1E1E)
        }
        
        // Convert Compose colors to AWT colors BEFORE DisposableEffect
        val backgroundColor = java.awt.Color(
            (bgColor.red * 255).toInt(),
            (bgColor.green * 255).toInt(),
            (bgColor.blue * 255).toInt(),
            (bgColor.alpha * 255).toInt()
        )
        
        val borderColor = java.awt.Color(
            (borderComposeColor.red * 255).toInt(),
            (borderComposeColor.green * 255).toInt(),
            (borderComposeColor.blue * 255).toInt(),
            (borderComposeColor.alpha * 255).toInt()
        )
        
        val textColorAWT = java.awt.Color(
            (textColor.red * 255).toInt(),
            (textColor.green * 255).toInt(),
            (textColor.blue * 255).toInt()
        )
        
        DisposableEffect(message, isWarning) {
            var popup: JBPopup? = null
            
            // Use JEditorPane for selectable text, like ComponentValidator
            val tipComponent = javax.swing.JEditorPane().apply {
                contentType = "text/html"
                isEditable = false
                
                // Set text color via CSS to match Compose popups
                val hexColor = String.format("#%02x%02x%02x", textColorAWT.red, textColorAWT.green, textColorAWT.blue)
                text = "<html><body style='color: $hexColor;'>$message</body></html>"
                
                // Like ComponentValidator: setOpaque(true) and setBackground()
                isOpaque = true
                background = backgroundColor
                border = JBUI.Borders.empty(5, 9)
                
                // Disable caret updates
                if (caret is javax.swing.text.DefaultCaret) {
                    (caret as javax.swing.text.DefaultCaret).updatePolicy = javax.swing.text.DefaultCaret.NEVER_UPDATE
                }
                caretPosition = 0
            }
            
            // Create popup - don't set border color here to avoid corner clipping issue
            popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(tipComponent, null)
                .setCancelOnClickOutside(false)
                .setCancelOnWindowDeactivation(false)
                .setShowShadow(true)
                .createPopup()
            
            // Position like ComponentValidator: Point(0, insets.top - popupHeight)
            // For top field, we position relative to mainPanel
            val popupSize = tipComponent.preferredSize
            val point = Point(
                JBUI.scale(0),
                -JBUI.scale(0) - popupSize.height
            )
            
            // Show popup relative to the main panel
            popup?.show(RelativePoint(mainPanelComponent, point))
            
            // After showing, configure rounded corners with border color
            // This ensures the border is part of the rounded corners, not clipped by them
            if (popup is com.intellij.ui.popup.AbstractPopup) {
                try {
                    val window = (popup as com.intellij.ui.popup.AbstractPopup).popupWindow
                    if (window != null && com.intellij.ui.WindowRoundedCornersManager.isAvailable()) {
                        // Use WindowRoundedCornersManager to set border color as part of rounded corners
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
                popup?.cancel()
            }
        }
    }
    
    @Composable
    private fun BoxScope.ValidationPopup(
        message: String,
        isWarning: Boolean
    ) {
        val surface = MaterialTheme.colorScheme.surface
        val errorColor = MaterialTheme.colorScheme.error
        
        val backgroundColor = if (isWarning) {
            androidx.compose.ui.graphics.Color(
                red = surface.red * 0.85f + errorColor.red * 0.15f,
                green = surface.green * 0.85f + errorColor.green * 0.15f,
                blue = surface.blue * 0.85f + errorColor.blue * 0.15f,
                alpha = 0.95f
            )
        } else {
            androidx.compose.ui.graphics.Color(
                red = surface.red * 0.7f + errorColor.red * 0.3f,
                green = surface.green * 0.7f + errorColor.green * 0.3f,
                blue = surface.blue * 0.7f + errorColor.blue * 0.3f,
                alpha = 0.95f
            )
        }
        
        val borderColor = if (isWarning) {
            MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
        } else {
            MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
        }
        
        // Use white text for good contrast in dark theme, dark text in light theme
        val textColor = if (isDarkTheme()) {
            androidx.compose.ui.graphics.Color.White
        } else {
            androidx.compose.ui.graphics.Color(0xFF1E1E1E)
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
            Surface(
                modifier = Modifier.pointerInput(Unit) {},
                color = backgroundColor,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.5.dp, borderColor)
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
    private fun PlatformCheckbox(
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
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    checkmarkColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.size(20.dp)
            )
            Text(label)
        }
    }
    
    private val FolderIcon: ImageVector
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
    
    private val GlobeIcon: ImageVector
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
    
    private val AppleIcon: ImageVector
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
    private fun BackgroundPlatformIcon(
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
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
    @Composable
    private fun CreateComposeUI() {
        var projectName by remember { mutableStateOf(projectNameValue) }
        var projectPath by remember { mutableStateOf(projectPathValue) }
        var projectId by remember { mutableStateOf(projectIdValue) }
        var composeVersion by remember { mutableStateOf(composeVersionValue) }
        var desktop by remember { mutableStateOf(targetDesktop) }
        var android by remember { mutableStateOf(targetAndroid) }
        var ios by remember { mutableStateOf(targetIOS) }
        var web by remember { mutableStateOf(targetWeb) }
        var git by remember { mutableStateOf(initGit) }
        var tests by remember { mutableStateOf(includeTests) }
        var enableDevVersions by remember { mutableStateOf(io.github.heisiar.composewizard.shared.settings.WizardSettings.getInstance().enableDevVersions) }
        
        // FUS: Track wizard opened
        LaunchedEffect(Unit) {
            ComposeWizardUsageCollector.logWizardOpened()
        }
        
        // Dev checkbox visibility logic
        // By default hidden, visible only with internal mode OR explicitly shown by user OR checkbox enabled
        val isInternalMode = com.intellij.openapi.application.ApplicationManager.getApplication().isInternal
        var devCheckboxVisibleByUser by remember { mutableStateOf(io.github.heisiar.composewizard.shared.settings.WizardSettings.getInstance().devCheckboxVisibleByUser) }
        var isDevCheckboxVisible by remember { mutableStateOf(isInternalMode || devCheckboxVisibleByUser || enableDevVersions) }
        
        // Update visibility when dependencies change
        LaunchedEffect(isInternalMode, devCheckboxVisibleByUser, enableDevVersions) {
            val newValue = isInternalMode || devCheckboxVisibleByUser || enableDevVersions
            isDevCheckboxVisible = newValue
        }
        
        // Triple-click detector state for toggling checkbox visibility
        var clickCount by remember { mutableStateOf(0) }
        var lastClickTime by remember { mutableStateOf(0L) }
        
        // Validation state for each field (like in platform)
        var projectNameError by remember { mutableStateOf<String?>(null) }
        var projectPathError by remember { mutableStateOf<String?>(null) }
        var projectLocationWarning by remember { mutableStateOf<String?>(null) }
        var projectIdError by remember { mutableStateOf<String?>(null) }
        
        // Focus state to show/hide validation popups
        val projectNameInteractionSource = remember { MutableInteractionSource() }
        val projectPathInteractionSource = remember { MutableInteractionSource() }
        val projectIdInteractionSource = remember { MutableInteractionSource() }
        
        val projectNameFocused by projectNameInteractionSource.collectIsFocusedAsState()
        val projectPathFocused by projectPathInteractionSource.collectIsFocusedAsState()
        val projectIdFocused by projectIdInteractionSource.collectIsFocusedAsState()
        
        // Validate Project Name on change
        LaunchedEffect(projectName) {
            val newError = validateProjectName(projectName)
            if (newError != null && newError != projectNameError) {
                val errorType = when {
                    newError.contains("empty", ignoreCase = true) -> "empty"
                    newError.contains("invalid", ignoreCase = true) -> "invalid_chars"
                    newError.contains("reserved", ignoreCase = true) -> "reserved_name"
                    else -> "invalid_chars"
                }
                ComposeWizardUsageCollector.logValidationError("project_name", errorType)
            }
            projectNameError = newError
        }
        
        // Validate Project Path on change
        LaunchedEffect(projectPath) {
            val newError = validateProjectPath(projectPath)
            if (newError != null && newError != projectPathError) {
                val errorType = when {
                    newError.contains("invalid", ignoreCase = true) -> "invalid_path"
                    newError.contains("write", ignoreCase = true) -> "no_write_access"
                    else -> "invalid_path"
                }
                ComposeWizardUsageCollector.logValidationError("project_location", errorType)
            }
            projectPathError = newError
        }
        
        // Validate full project location on change
        LaunchedEffect(projectName, projectPath) {
            val newWarning = validateProjectLocation(projectName, projectPath)
            if (newWarning != null && newWarning != projectLocationWarning) {
                val errorType = when {
                    newWarning.contains("already open", ignoreCase = true) -> "already_open"
                    newWarning.contains("not empty", ignoreCase = true) -> "directory_not_empty"
                    else -> "directory_not_empty"
                }
                ComposeWizardUsageCollector.logValidationError("project_location", errorType)
            }
            projectLocationWarning = newWarning
        }
        
        // Validate Project ID on change
        LaunchedEffect(projectId) {
            val validation = builder.validateProjectId(projectId)
            val newError = if (!validation.isValid) {
                validation.errors.firstOrNull()
            } else {
                null
            }
            if (newError != null && newError != projectIdError) {
                val errorType = when {
                    newError.contains("empty", ignoreCase = true) -> "empty"
                    newError.contains("invalid", ignoreCase = true) || 
                    newError.contains("package", ignoreCase = true) -> "invalid_package_format"
                    else -> "invalid_package_format"
                }
                ComposeWizardUsageCollector.logValidationError("project_id", errorType)
            }
            projectIdError = newError
        }
        
        var availableVersions by remember { mutableStateOf<List<String>>(emptyList()) }
        var versionsExpanded by remember { mutableStateOf(false) }

        val hasNoTargets = !desktop && !android && !ios && !web
        
        // Log validation error when no platforms selected
        LaunchedEffect(hasNoTargets) {
            if (hasNoTargets) {
                ComposeWizardUsageCollector.logValidationError("platforms", "no_platform_selected")
            }
        }
        
        // Load available versions from Maven when enableDevVersions changes
        // and auto-select the latest version from the new source
        LaunchedEffect(enableDevVersions) {
            val newVersions = loadComposeVersionsFromMaven(enableDevVersions)
            availableVersions = newVersions
            // Auto-select the latest (first) version from the new source
            if (newVersions.isNotEmpty()) {
                composeVersion = newVersions.first()
            }
        }

            // Update stored values when state changes
            LaunchedEffect(projectName, projectPath, projectId, composeVersion, desktop, android, ios, web, git, tests, enableDevVersions, devCheckboxVisibleByUser) {
                projectNameValue = projectName
                projectPathValue = projectPath
                projectIdValue = projectId
                composeVersionValue = composeVersion
                this@ComposeMultiplatformWizardStep.enableDevVersions = enableDevVersions
                io.github.heisiar.composewizard.shared.settings.WizardSettings.getInstance().enableDevVersions = enableDevVersions
                io.github.heisiar.composewizard.shared.settings.WizardSettings.getInstance().devCheckboxVisibleByUser = devCheckboxVisibleByUser
                targetDesktop = desktop
                targetAndroid = android
                targetIOS = ios
                targetWeb = web
                initGit = git
                includeTests = tests
            
            updateButtonState(!hasNoTargets)
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Main scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .widthIn(min = 220.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(start = 0.dp, end = 0.dp, top = 0.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Project Name and Compose Version
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top // Top alignment for fields with checkbox below
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = projectName,
                                onValueChange = { 
                                    projectName = it
                                    ComposeWizardUsageCollector.logFieldEdited("project_name", it.isNotEmpty())
                                },
                                label = { Text("Project Name", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                isError = projectNameError != null,
                                interactionSource = projectNameInteractionSource
                            )
                            
                            // Validation popup (like in platform) - positioned above the field
                            // Show popup only when field is focused
                            if (projectNameError != null && projectNameFocused) {
                                key(projectNameError, projectNameFocused) {
                                    ValidationPopupJB(
                                        message = projectNameError!!,
                                        isWarning = false
                                    )
                                }
                            }
                        }
                        Column(
                            modifier = Modifier.width(190.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ExposedDropdownMenuBox(
                                expanded = versionsExpanded,
                                onExpandedChange = { versionsExpanded = it }
                            ) {
                                val showTooltip = composeVersion.length > 15
                                
                                if (showTooltip) {
                                    androidx.compose.foundation.TooltipArea(
                                        tooltip = {
                                            androidx.compose.material3.Surface(
                                                color = MaterialTheme.colorScheme.surface,
                                                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                                                shadowElevation = 4.dp,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                                            ) {
                                                Text(
                                                    text = composeVersion,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        },
                                        delayMillis = 500
                                    ) {
                                        OutlinedTextField(
                                            value = composeVersion,
                                            onValueChange = { },
                                            label = { Text("Compose Version") },
                                            trailingIcon = { 
                                                Box(
                                                    modifier = Modifier
                                                        .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)))
                                                ) {
                                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = versionsExpanded)
                                                }
                                            },
                                            modifier = Modifier
                                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                                .fillMaxWidth(),
                                            singleLine = true,
                                            readOnly = true,
                                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                                        )
                                    }
                                } else {
                                    OutlinedTextField(
                                        value = composeVersion,
                                        onValueChange = { },
                                        label = { Text("Compose Version") },
                                        trailingIcon = { 
                                            Box(
                                                modifier = Modifier
                                                    .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)))
                                            ) {
                                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = versionsExpanded)
                                            }
                                        },
                                        modifier = Modifier
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                            .fillMaxWidth(),
                                        singleLine = true,
                                        readOnly = true,
                                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                                    )
                                }
                                ExposedDropdownMenu(
                                    expanded = versionsExpanded,
                                    onDismissRequest = { versionsExpanded = false }
                                ) {
                                    val scrollState = rememberScrollState()
                                    Box(
                                        modifier = Modifier
                                            .height(300.dp)
                                            .fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .verticalScroll(scrollState)
                                                .fillMaxWidth()
                                                .padding(end = 12.dp)
                                        ) {
                                            if (availableVersions.isEmpty()) {
                                                DropdownMenuItem(
                                                    text = { Text("Loading...") },
                                                    onClick = { },
                                                    enabled = false
                                                )
                                            } else {
                                                availableVersions.forEach { version ->
                                                    DropdownMenuItem(
                                                        text = { Text(version) },
                                                        onClick = {
                                                            composeVersion = version
                                                            versionsExpanded = false
                                                            val isDevVersion = version.contains("-dev") || 
                                                                              version.contains("-alpha") || 
                                                                              version.contains("-beta") ||
                                                                              version.contains("-rc")
                                                            ComposeWizardUsageCollector.logComposeVersionSelected(version, isDevVersion)
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                        VerticalScrollbar(
                                            adapter = rememberScrollbarAdapter(scrollState),
                                            modifier = Modifier
                                                .align(Alignment.CenterEnd)
                                                .height(300.dp),
                                            style = androidx.compose.foundation.ScrollbarStyle(
                                                minimalHeight = 16.dp,
                                                thickness = 8.dp,
                                                shape = CircleShape,
                                                hoverDurationMillis = 300,
                                                unhoverColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                                hoverColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                        )
                                    }
                                }
                            }
                            
                            // Toggle between Production and Dev-maven versions
                            // Only visible if: internal mode OR unlocked in session OR toggle is enabled
                            if (isDevCheckboxVisible) {
                                Row(
                                    modifier = Modifier
                                        .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)))
                                        .clickable(
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() }
                                        ) { 
                                            enableDevVersions = !enableDevVersions
                                            ComposeWizardUsageCollector.logDevVersionsToggled(enableDevVersions)
                                        },
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Switch(
                                        checked = enableDevVersions,
                                        onCheckedChange = null,
                                        modifier = Modifier.scale(0.75f)
                                    )
                                    Text(
                                        text = "Dev-maven",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (enableDevVersions) 
                                            MaterialTheme.colorScheme.primary 
                                        else 
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }

                    // Project Location
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = projectPath,
                            onValueChange = { 
                                projectPath = it
                                ComposeWizardUsageCollector.logFieldEdited("project_location", it.isNotEmpty())
                            },
                            label = { Text("Location") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = projectPathError != null,
                            interactionSource = projectPathInteractionSource
                        )
                        
                        // Validation popup (like in platform) - positioned above the field
                        if (projectPathError != null && projectPathFocused) {
                            ValidationPopup(
                                message = projectPathError!!,
                                isWarning = false
                            )
                        }
                    }
                    val interactionSource = remember { MutableInteractionSource() }
                    val isHovered by interactionSource.collectIsHoveredAsState()
                    
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .offset(y = 4.dp)
                            .background(
                                color = if (isHovered) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable(
                                indication = null,
                                interactionSource = interactionSource
                            ) {
                                browseForFolder()?.let { projectPath = it }
                            }
                            .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = FolderIcon,
                            contentDescription = "Browse folder",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                
                // Full project path hint - clearly belongs to Location field
                SelectionContainer {
                    Text(
                        text = "Project will be created at: ${collapsePath(File(expandPath(projectPath), projectName).absolutePath)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier
                            .padding(start = 2.dp)
                            .offset(y = (-4).dp)
                    )
                }
                
                // Show location warning if directory is not empty
                if (projectLocationWarning != null) {
                    SelectionContainer {
                        Text(
                            text = projectLocationWarning!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFF9800),
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                    }
                }

                    // Project ID
                    Box {
                        OutlinedTextField(
                            value = projectId,
                            onValueChange = { 
                                projectId = it
                                ComposeWizardUsageCollector.logFieldEdited("project_id", it.isNotEmpty())
                            },
                            label = { Text("Project ID") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = projectIdError != null,
                            interactionSource = projectIdInteractionSource
                        )
                        
                        // Validation popup (like in platform) - positioned above the field
                        if (projectIdError != null && projectIdFocused) {
                            ValidationPopup(
                                message = projectIdError!!,
                                isWarning = false
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                // Platforms section with icons underneath
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally)
                ) {
                    // Desktop
                    Column(
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier
                            .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)))
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { 
                                desktop = !desktop
                                ComposeWizardUsageCollector.logPlatformToggled("Desktop", desktop)
                            }
                    ) {
                        PlatformCheckbox(
                            checked = desktop,
                            label = "Desktop",
                            onCheckedChange = { }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        BackgroundPlatformIcon(
                            active = desktop,
                            icon = "🖥",
                            color = Color(0xFF6200EE)
                        )
                    }
                    
                    // iOS
                    Column(
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier
                            .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)))
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { 
                                ios = !ios
                                ComposeWizardUsageCollector.logPlatformToggled("iOS", ios)
                            }
                    ) {
                        PlatformCheckbox(
                            checked = ios,
                            label = "iOS",
                            onCheckedChange = { }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        BackgroundPlatformIcon(
                            active = ios,
                            icon = "",
                            color = Color.Transparent,
                            isApple = true
                        )
                    }
                    
                    // Web
                    Column(
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier
                            .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)))
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { 
                                web = !web
                                ComposeWizardUsageCollector.logPlatformToggled("Web", web)
                            }
                    ) {
                        PlatformCheckbox(
                            checked = web,
                            label = "Web",
                            onCheckedChange = { }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        BackgroundPlatformIcon(
                            active = web,
                            icon = "",
                            color = Color.Transparent,
                            isGlobe = true
                        )
                    }
                    
                    // Android
                    Column(
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier
                            .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)))
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { 
                                android = !android
                                ComposeWizardUsageCollector.logPlatformToggled("Android", android)
                            }
                    ) {
                        PlatformCheckbox(
                            checked = android,
                            label = "Android",
                            onCheckedChange = { }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        BackgroundPlatformIcon(
                            active = android,
                            icon = "📱",
                            color = Color(0xFF3DDC84)
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Git repository checkbox
                Row(
                    modifier = Modifier
                        .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { 
                            git = !git
                            ComposeWizardUsageCollector.logGitToggled(git)
                        },
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = git,
                        onCheckedChange = null
                    )
                    Text("Create Git repository")
                }

                // Include tests checkbox
                Row(
                    modifier = Modifier
                        .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { 
                            tests = !tests
                            ComposeWizardUsageCollector.logTestsToggled(tests)
                        },
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = tests,
                        onCheckedChange = null
                    )
                    Text("Include tests")
                }
            }
            
            // Platform selection error at the bottom (like disabled button message)
            if (hasNoTargets) {
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.weight(1f)) // Push message to the right
                    Text(
                        text = "⚡",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                    SelectionContainer {
                        Text(
                            text = "At least one platform must be selected",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
        
        // Plugin version at bottom left (Easter egg for dev checkbox)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, bottom = 8.dp, top = 4.dp)
        ) {
            Text(
                text = "v1.0.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastClickTime < 500) {
                            clickCount++
                            if (clickCount >= 3) {
                                // Triple click: toggle dev checkbox visibility
                                if (!devCheckboxVisibleByUser) {
                                    ComposeWizardUsageCollector.logDevVersionsUnlocked()
                                }
                                devCheckboxVisibleByUser = !devCheckboxVisibleByUser
                                clickCount = 0
                            }
                        } else {
                            clickCount = 1
                        }
                        lastClickTime = currentTime
                    }
            )
        }
    }
    }
    
    override fun getComponent(): JComponent = mainPanel

    private fun browseForFolder(): String? {
        val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor().apply {
            title = "Select Project Location"
        }

        val chosen = FileChooser.chooseFile(descriptor, null, null)
        return chosen?.path?.let { collapsePath(it) }
    }

    private var createButton: JButton? = null

    override fun _init() {
        super._init()
        SwingUtilities.invokeLater {
            updateButtonText()
            updateButtonState(validate())
        }
    }

    private fun updateButtonText() {
        val comp = component
        var parent = comp.parent
        while (parent != null) {
            val buttons = UIUtil.findComponentsOfType(parent as? JComponent ?: return, JButton::class.java)
            for (button in buttons) {
                if (button.text?.contains("Next") == true || button.text?.contains("OK") == true || button.text?.contains("Create") == true) {
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

    private fun updateButtonState(enabled: Boolean) {
        SwingUtilities.invokeLater {
            if (createButton == null) {
                updateButtonText()
            }
            createButton?.isEnabled = enabled
        }
    }

        override fun updateDataModel() {
            val expandedPath = expandPath(projectPathValue)
            val fullPath = File(expandedPath, projectNameValue).absolutePath
            builder.contentEntryPath = fullPath
            builder.name = projectNameValue
            builder.moduleFilePath = "$fullPath/$projectNameValue.iml"

            // FUS: Log wizard completed with all settings
            val timeSpent = System.currentTimeMillis() - wizardStartTime
            val platformsCount = listOf(targetDesktop, targetAndroid, targetIOS, targetWeb).count { it }
            ComposeWizardUsageCollector.logWizardCompleted(
                platformsCount = platformsCount,
                includeTests = includeTests,
                includeGit = initGit,
                usedDevVersions = enableDevVersions,
                timeSpentMs = timeSpent
            )

            // Pass settings to builder
            builder.projectName = projectNameValue
            builder.projectId = projectIdValue
            builder.composeVersion = composeVersionValue
            builder.targetDesktop = targetDesktop
            builder.targetAndroid = targetAndroid
            builder.targetIOS = targetIOS
            builder.targetWeb = targetWeb
            builder.initGit = initGit
            builder.includeTests = includeTests
            builder.enableDevVersions = io.github.heisiar.composewizard.shared.settings.WizardSettings.getInstance().enableDevVersions
        }

    override fun validate(): Boolean {
        println("=== VALIDATE CALLED ===")
        
        // Validate project name
        val nameError = validateProjectName(projectNameValue)
        if (nameError != null) {
            println("ERROR: $nameError")
            return false
        }
        
        // Validate project path
        val pathError = validateProjectPath(projectPathValue)
        if (pathError != null) {
            println("ERROR: $pathError")
            return false
        }
        
        // Validate project location (full path)
        val locationError = validateProjectLocationBlocking(projectNameValue, projectPathValue)
        if (locationError != null) {
            println("ERROR: $locationError")
            return false
        }
        
        // Validate Project ID format
        val projectIdValidation = builder.validateProjectId(projectIdValue)
        if (!projectIdValidation.isValid) {
            println("ERROR: Invalid Project ID - ${projectIdValidation.errors.firstOrNull()}")
            return false
        }
        
        // At least one platform must be selected
        if (!targetDesktop && !targetAndroid && !targetIOS && !targetWeb) {
            println("ERROR: No platform selected")
            return false
        }
        
        println("=== VALIDATE PASSED ===")
        return true
    }
    
    private val namePattern = "[a-zA-Z\\d\\s_.-]*".toRegex()
    private val firstSymbolNamePattern = "[a-zA-Z\\d_].*".toRegex()
    private val reservedWordsPattern = "(^|[ .])(con|prn|aux|nul|com\\d|lpt\\d)($|[ .])".toRegex(RegexOption.IGNORE_CASE)
    
    private fun validateProjectName(name: String): String? {
        if (name.isEmpty()) {
            return "Project name must not be empty"
        }
        
        if (!namePattern.matches(name)) {
            return "Project name can only contain letters, digits, spaces, '_', '.' and '-'"
        }
        
        if (!firstSymbolNamePattern.matches(name)) {
            return "Project name must start with a letter, digit or '_'"
        }
        
        if (reservedWordsPattern.find(name) != null) {
            return "Project name contains reserved words"
        }
        
        return null
    }
    
    private fun validateProjectPath(path: String): String? {
        if (path.isEmpty()) {
            return "Location must not be empty"
        }
        
        val expandedPath = expandPath(path)
        
        try {
            Path.of(expandedPath)
        } catch (e: InvalidPathException) {
            return "Invalid path"
        }
        
        val pathFile = File(expandedPath)
        if (pathFile.exists()) {
            if (!pathFile.isDirectory) {
                return "Location is not a directory"
            }
            if (!pathFile.canWrite()) {
                return "Location is not writable"
            }
        }
        
        return null
    }
    
    private fun validateProjectLocation(projectName: String, projectPath: String): String? {
        if (projectName.isEmpty() || projectPath.isEmpty()) {
            return null
        }
        
        val expandedPath = expandPath(projectPath)
        val fullPath = File(expandedPath, projectName)
        
        if (fullPath.exists() && fullPath.isDirectory) {
            val entries = fullPath.listFiles()
            if (entries != null && entries.isNotEmpty()) {
                return "Directory '${fullPath.name}' is not empty"
            }
        }
        
        return null
    }
    
    private fun validateProjectLocationBlocking(projectName: String, projectPath: String): String? {
        if (projectName.isEmpty() || projectPath.isEmpty()) {
            return null
        }
        
        val expandedPath = expandPath(projectPath)
        val fullPath = File(expandedPath, projectName)
        
        try {
            val existingProject = ProjectUtil.findProject(fullPath.toPath())
            if (existingProject != null) {
                return "Project directory is already taken by project '${existingProject.name}'"
            }
        } catch (e: Exception) {
            // In test environment or when Application is not initialized, skip this check
            // This is acceptable as it's a non-critical validation
        }
        
        return null
    }

    private fun getDefaultProjectPath(): String {
        return "~/IdeaProjects"
    }
    
    private fun expandPath(path: String): String {
        return if (path.startsWith("~/")) {
            val userHome = System.getProperty("user.home")
            FileUtil.toSystemIndependentName("$userHome/${path.substring(2)}")
        } else {
            path
        }
    }
    
    private fun collapsePath(path: String): String {
        val userHome = System.getProperty("user.home")
        val normalizedHome = FileUtil.toSystemIndependentName(userHome)
        val normalizedPath = FileUtil.toSystemIndependentName(path)
        return if (normalizedPath.startsWith(normalizedHome)) {
            "~${normalizedPath.substring(normalizedHome.length)}"
        } else {
            path
        }
    }
    
    private fun suggestUniqueName(baseName: String, path: String): String {
        val expandedPath = expandPath(path)
        val dir = File(expandedPath)
        if (!dir.exists()) return baseName
        
        return FileUtil.createSequentFileName(dir, baseName, "") { file ->
            !file.exists()
        }
    }

    fun getBuilder(): ComposeMultiplatformModuleBuilder = builder
    
    fun getProjectName(): String = projectNameValue
    
    fun getProjectPath(): String = projectPathValue
    
        fun getProjectId(): String = projectIdValue
        
    fun getComposeVersion(): String = composeVersionValue
    
    private suspend fun loadComposeVersionsFromMaven(includeDevVersions: Boolean = false): List<String> {
        return io.github.heisiar.composewizard.shared.services.ComposeVersionService().fetchAvailableVersions(includeDevVersions)
    }
}
