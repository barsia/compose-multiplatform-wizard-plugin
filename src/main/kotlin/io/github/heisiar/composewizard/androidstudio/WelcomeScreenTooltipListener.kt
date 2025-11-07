package io.github.heisiar.composewizard.androidstudio

import com.intellij.ide.AppLifecycleListener
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.JBColor
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.JBUI
import io.github.heisiar.composewizard.shared.settings.WizardSettings
import java.awt.*
import javax.swing.AbstractButton
import javax.swing.JComponent
import javax.swing.JLabel

/**
 * Shows a welcome tooltip on Welcome Screen (when no projects are open),
 * pointing to the "More Actions" button where users can find the Compose Multiplatform project wizard.
 * 
 * This listener is triggered when the app frame is created (IDE starts or all projects are closed).
 */
class WelcomeScreenTooltipListener : AppLifecycleListener {
    
    private val LOG = Logger.getInstance(WelcomeScreenTooltipListener::class.java)
    
    override fun appFrameCreated(commandLineArgs: MutableList<String>) {
        val settings = WizardSettings.getInstance()
        
        LOG.info("WelcomeScreenTooltipListener: appFrameCreated called")
        
        // Check if tooltip was already shown
        if (settings.welcomeTooltipShown) {
            LOG.info("WelcomeScreenTooltipListener: tooltip already shown, skipping")
            return
        }
        
        // Retry approach: try to find frame multiple times with small delays
        ApplicationManager.getApplication().executeOnPooledThread {
            var attempts = 0
            val maxAttempts = 10
            var frameFound = false
            
            while (attempts < maxAttempts && !frameFound) {
                attempts++
                
                val frame = WindowManager.getInstance().findVisibleFrame()
                if (frame != null) {
                    LOG.info("WelcomeScreenTooltipListener: Frame found on attempt $attempts")
                    frameFound = true
                    
                    ApplicationManager.getApplication().invokeLater {
                        attachToFrame(frame)
                    }
                } else {
                    LOG.debug("WelcomeScreenTooltipListener: No visible frame on attempt $attempts, retrying...")
                    Thread.sleep(200)  // Small delay between retries
                }
            }
            
            if (!frameFound) {
                LOG.warn("WelcomeScreenTooltipListener: Failed to find visible frame after $maxAttempts attempts")
            }
        }
    }
    
    private fun attachToFrame(frame: Window) {
        // Check if we're on Welcome Screen
        val frameClassName = frame.javaClass.simpleName
        val isWelcomeScreen = frameClassName.contains("Welcome", ignoreCase = true) || 
                              frameClassName.contains("FlatWelcome", ignoreCase = true)
        
        if (!isWelcomeScreen) {
            LOG.debug("WelcomeScreenTooltipListener: not a Welcome Screen frame: $frameClassName")
            return
        }
        
        LOG.info("WelcomeScreenTooltipListener: Welcome Screen detected, searching for More Actions button")
        
        val settings = WizardSettings.getInstance()
        
        // Check if there are open projects to determine which button to find
        val openProjects = ProjectManager.getInstance().openProjects
        val hasOpenProjects = openProjects.isNotEmpty()
        
        // Retry approach: button might not be ready yet
        ApplicationManager.getApplication().executeOnPooledThread {
            var attempts = 0
            val maxAttempts = 10
            var moreActionsButton: java.awt.Component? = null
            
            while (attempts < maxAttempts && moreActionsButton == null) {
                attempts++
                moreActionsButton = findActionsButton(frame, hasOpenProjects)
                
                if (moreActionsButton == null) {
                    LOG.debug("WelcomeScreenTooltipListener: More Actions button not found on attempt $attempts, retrying...")
                    Thread.sleep(200)
                }
            }
            
            if (moreActionsButton == null) {
                LOG.warn("WelcomeScreenTooltipListener: More Actions button not found after $maxAttempts attempts")
                return@executeOnPooledThread
            }
            
            LOG.info("WelcomeScreenTooltipListener: More Actions button found on attempt $attempts: ${moreActionsButton.javaClass.simpleName}")
            
            // Validate that this is the correct button (not a different one with the same name)
            if (!isValidWelcomeScreenButton(moreActionsButton)) {
                LOG.warn("WelcomeScreenTooltipListener: Found button is not valid (invisible, disabled, or wrong context)")
                return@executeOnPooledThread
            }
            
            // Wait 500ms for UI to stabilize and smooth UX
            Thread.sleep(500)
            
            ApplicationManager.getApplication().invokeLater {
                showWelcomeTooltipForButton(moreActionsButton)
                settings.welcomeTooltipShown = true
                LOG.info("WelcomeScreenTooltipListener: tooltip shown successfully")
            }
        }
    }
    
    /**
     * Validate that the found button is the correct one on Welcome Screen (Welcome Wizard).
     * 
     * STRICT checks - button MUST be on Welcome Wizard, other buttons are ignored:
     * 1. Button is visible
     * 2. Button is enabled
     * 3. Button is inside ActionToolbarImpl (correct context)
     * 4. Button's root window is the Welcome Screen frame
     */
    private fun isValidWelcomeScreenButton(button: Component): Boolean {
        // Check if button is visible
        if (!button.isVisible) {
            LOG.debug("WelcomeScreenTooltipListener: Button is not visible")
            return false
        }
        
        // Check if button is enabled (for components that support it)
        if (button is AbstractButton && !button.isEnabled) {
            LOG.debug("WelcomeScreenTooltipListener: Button is not enabled")
            return false
        }
        
        // STRICT CHECK: Button must be inside Welcome Screen frame (not any other window/popup)
        val buttonWindow = javax.swing.SwingUtilities.getWindowAncestor(button)
        if (buttonWindow == null) {
            LOG.debug("WelcomeScreenTooltipListener: Button has no window ancestor")
            return false
        }
        
        val frameClassName = buttonWindow.javaClass.simpleName
        val isWelcomeFrame = frameClassName.contains("Welcome", ignoreCase = true) || 
                             frameClassName.contains("FlatWelcome", ignoreCase = true)
        
        if (!isWelcomeFrame) {
            LOG.debug("WelcomeScreenTooltipListener: Button is NOT in Welcome Screen frame (found in: $frameClassName)")
            return false
        }
        
        // Check if button is inside ActionToolbarImpl (correct context)
        var parent = button.parent
        var foundToolbar = false
        var depth = 0
        val maxDepth = 10 // Prevent infinite loop
        
        while (parent != null && depth < maxDepth) {
            val parentClassName = parent.javaClass.name
            val parentSuperClass = parent.javaClass.superclass?.name ?: "null"
            
            // Check by class name OR by superclass (for anonymous classes that extend ActionToolbarImpl)
            if (parentClassName.contains("ActionToolbarImpl") || 
                parentSuperClass.contains("ActionToolbarImpl")) {
                foundToolbar = true
                LOG.debug("WelcomeScreenTooltipListener: Button is inside ActionToolbarImpl at depth $depth")
                break
            }
            parent = parent.parent
            depth++
        }
        
        if (!foundToolbar) {
            LOG.debug("WelcomeScreenTooltipListener: Button is not inside ActionToolbarImpl after checking $depth levels")
            return false
        }
        
        LOG.info("WelcomeScreenTooltipListener: Button validation passed")
        return true
    }
    
    /**
     * Find the actions button on Welcome Screen.
     * 
     * @param component Root component to search in
     * @param hasOpenProjects If true, search for three-dots menu button (⋮) at the top.
     *                        If false, search for "More Actions" button in the wizard.
     */
    @Suppress("DialogTitleCapitalization")
    private fun findActionsButton(component: Component, hasOpenProjects: Boolean): Component? {
        if (component is ActionButton) {
            val action = component.action
            val presentation = action.templatePresentation
            val text = presentation.text
            val description = presentation.description
            val accessibleName = component.accessibleContext?.accessibleName
            
            if (hasOpenProjects) {
                // Look for three-dots menu button (⋮) at the top - usually has no text, just icon
                // Check by action class name or icon
                if (text == null || text.isEmpty() || text.isBlank()) {
                    // This might be the three-dots button (icon-only button)
                    if (action.javaClass.name.contains("ShowSettings", ignoreCase = true) ||
                        action.javaClass.name.contains("SettingsEntryPoint", ignoreCase = true) ||
                        action.javaClass.name.contains("GearAction", ignoreCase = true) ||
                        action.javaClass.name.contains("WelcomeScreenGearAction", ignoreCase = true) ||
                        action.javaClass.name.contains("MoreActions", ignoreCase = true) ||
                        action.javaClass.name.contains("SecondaryActions", ignoreCase = true) ||
                        description?.contains("Settings", ignoreCase = true) == true ||
                        description?.contains("Options", ignoreCase = true) == true ||
                        description?.contains("More Actions", ignoreCase = true) == true ||
                        accessibleName?.contains("More Actions", ignoreCase = true) == true) {
                        LOG.info("Found three-dots/settings button on Welcome Screen: ${action.javaClass.simpleName}, desc='$description'")
                        return component
                    }
                }
            } else {
                // Look for "More Actions" button in wizard (no open projects)
                if (text?.contains("More Actions", ignoreCase = true) == true ||
                    description?.contains("More Actions", ignoreCase = true) == true ||
                    text?.contains("Configure", ignoreCase = true) == true ||
                    description?.contains("Configure", ignoreCase = true) == true ||
                    action.javaClass.name.contains("Configure", ignoreCase = true) ||
                    action.javaClass.name.contains("MoreActions", ignoreCase = true)) {
                    LOG.info("Found More Actions button on Welcome Screen: ${action.javaClass.simpleName}, text='$text', desc='$description'")
                    return component
                }
            }
        }
        
        if (component is AbstractButton) {
            val text = component.text
            val accessibleName = component.accessibleContext?.accessibleName
            val toolTip = component.toolTipText
            
            if (hasOpenProjects) {
                // Look for settings/gear button
                if (toolTip?.contains("Settings", ignoreCase = true) == true ||
                    toolTip?.contains("Options", ignoreCase = true) == true ||
                    accessibleName?.contains("Settings", ignoreCase = true) == true) {
                    LOG.info("Found settings button on Welcome Screen: ${component.javaClass.simpleName}, tooltip='$toolTip'")
                    return component
                }
            } else {
                // Look for "More Actions" button
                if (text?.contains("More Actions", ignoreCase = true) == true ||
                    accessibleName?.contains("More Actions", ignoreCase = true) == true ||
                    toolTip?.contains("More Actions", ignoreCase = true) == true ||
                    toolTip?.contains("Configure", ignoreCase = true) == true) {
                    LOG.info("Found More Actions button on Welcome Screen: ${component.javaClass.simpleName}, text='$text', tooltip='$toolTip'")
                    return component
                }
            }
        }
        
        if (component is java.awt.Container) {
            for (child in component.components) {
                val result = findActionsButton(child, hasOpenProjects)
                if (result != null) {
                    return result
                }
            }
        }
        
        return null
    }
    
    /**
     * Show tooltip for a specific button.
     */
    private fun showWelcomeTooltipForButton(targetButton: Component): Boolean {
        val frame = WindowManager.getInstance().findVisibleFrame()
        if (frame == null) {
            LOG.debug("WelcomeScreenTooltipListener: no visible frame found")
            return false
        }
        
        LOG.info("WelcomeScreenTooltipListener: Showing tooltip for button: ${targetButton.javaClass.name}")
        
        val colorStart = JBColor(0x49C0FF, 0x49C0FF)
        val colorEnd = JBColor(0x6C4CFF, 0x6C4CFF)
        
        val label = JLabel("Create Compose Multiplatform projects here")
        label.foreground = java.awt.Color.WHITE
        label.border = JBUI.Borders.empty(6, 10)
        
        val buttonLocationOnScreen = targetButton.locationOnScreen
        val buttonCenterX = buttonLocationOnScreen.x + targetButton.width / 2
        
        val tooltipWidth = label.preferredSize.width.toFloat()
        val tooltipHeight = label.preferredSize.height.toFloat()
        
        val screenBounds = frame.graphicsConfiguration.bounds
        val tooltipLeftX = (buttonCenterX - tooltipWidth / 2).coerceAtLeast(screenBounds.x.toFloat())
        val tooltipRightX = (tooltipLeftX + tooltipWidth).coerceAtMost((screenBounds.x + screenBounds.width).toFloat())
        val actualTooltipLeft = if (tooltipRightX - tooltipWidth < screenBounds.x) {
            screenBounds.x.toFloat()
        } else {
            (tooltipRightX - tooltipWidth).coerceAtLeast(screenBounds.x.toFloat())
        }
        
        val pointerX = (buttonCenterX - actualTooltipLeft).coerceIn(0f, tooltipWidth)
        val pointerY = 0f
        
        val ratio = (pointerX * tooltipWidth + pointerY * tooltipHeight) / (tooltipWidth * tooltipWidth + tooltipHeight * tooltipHeight)
        
        val r = (colorStart.red + (colorEnd.red - colorStart.red) * ratio).toInt().coerceIn(0, 255)
        val g = (colorStart.green + (colorEnd.green - colorStart.green) * ratio).toInt().coerceIn(0, 255)
        val b = (colorStart.blue + (colorEnd.blue - colorStart.blue) * ratio).toInt().coerceIn(0, 255)
        val pointerColor = JBColor(java.awt.Color(r, g, b), java.awt.Color(r, g, b))
        
        val gradientPanel = object : JComponent() {
            init {
                layout = BorderLayout()
                isOpaque = true
                add(label, BorderLayout.CENTER)
            }
            
            override fun paintComponent(g: Graphics) {
                val g2d = g.create() as Graphics2D
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                
                val gradient = GradientPaint(
                    0f, 0f, colorStart,
                    width.toFloat(), height.toFloat(), colorEnd
                )
                g2d.paint = gradient
                g2d.fillRoundRect(0, 0, width, height, 8, 8)
                g2d.dispose()
            }
        }
        
        val balloon = JBPopupFactory.getInstance()
            .createBalloonBuilder(gradientPanel)
            .setFillColor(pointerColor)
            .setBorderColor(JBColor(java.awt.Color(0, 0, 0, 0), java.awt.Color(0, 0, 0, 0)))
            .setBorderInsets(JBUI.emptyInsets())
            .setPointerSize(JBUI.size(8, 4))
            .setShadow(false)
            .setFadeoutTime(10000)
            .setHideOnClickOutside(true)
            .setHideOnKeyOutside(true)
            .setHideOnAction(true)
            .setAnimationCycle(200)
            .createBalloon()
        
        val point = RelativePoint(targetButton, Point(targetButton.width / 2, targetButton.height))
        balloon.show(point, Balloon.Position.below)
        
        return true
    }
}

