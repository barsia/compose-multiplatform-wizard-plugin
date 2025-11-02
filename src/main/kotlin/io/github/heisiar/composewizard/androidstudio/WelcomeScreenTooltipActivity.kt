package io.github.heisiar.composewizard.androidstudio

import com.intellij.ide.AppLifecycleListener
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.JBColor
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.JBUI
import io.github.heisiar.composewizard.shared.settings.WizardSettings
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.GradientPaint
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Point
import java.awt.RenderingHints
import javax.swing.AbstractButton
import javax.swing.JComponent
import javax.swing.JLabel

class WelcomeScreenTooltipActivity : AppLifecycleListener {
    
    private val LOG = Logger.getInstance(WelcomeScreenTooltipActivity::class.java)
    
        override fun appFrameCreated(commandLineArgs: MutableList<String>) {
            val settings = WizardSettings.getInstance()
            
            LOG.info("WelcomeScreenTooltipActivity: appFrameCreated called")
            
            if (settings.welcomeTooltipShown) {
                LOG.info("WelcomeScreenTooltipActivity: tooltip already shown, skipping")
                return
            }
        
        ApplicationManager.getApplication().invokeLater {
            ApplicationManager.getApplication().executeOnPooledThread {
                var attempts = 0
                val maxAttempts = 10
                var tooltipShown = false
                
                while (attempts < maxAttempts) {
                    Thread.sleep(1000)
                    attempts++
                    
                    ApplicationManager.getApplication().invokeLater {
                        LOG.info("WelcomeScreenTooltipActivity: attempt $attempts/$maxAttempts to show tooltip")
                        if (showWelcomeTooltip()) {
                            settings.welcomeTooltipShown = true
                            tooltipShown = true
                            LOG.info("WelcomeScreenTooltipActivity: tooltip shown successfully on attempt $attempts")
                        }
                    }
                    
                    if (tooltipShown) {
                        break
                    }
                }
                
                if (!tooltipShown) {
                    LOG.warn("WelcomeScreenTooltipActivity: failed to show tooltip after $maxAttempts attempts")
                }
            }
        }
    }
    
    private fun showWelcomeTooltip(): Boolean {
        val frame = WindowManager.getInstance().findVisibleFrame()
        if (frame == null) {
            LOG.debug("WelcomeScreenTooltipActivity: no visible frame found")
            return false
        }
        
        LOG.debug("WelcomeScreenTooltipActivity: searching for More Actions button in frame: ${frame.javaClass.simpleName}")
        val moreActionsButton = findMoreActionsButton(frame)
        if (moreActionsButton == null) {
            LOG.debug("WelcomeScreenTooltipActivity: More Actions button not found")
            return false
        }
        
        LOG.info("WelcomeScreenTooltipActivity: More Actions button found: ${moreActionsButton.javaClass.name}")
        
        val colorStart = JBColor(0x49C0FF, 0x49C0FF)
        val colorEnd = JBColor(0x6C4CFF, 0x6C4CFF)
        
        val label = JLabel("Create Compose Multiplatform projects here")
        label.foreground = JBColor.WHITE
        label.border = JBUI.Borders.empty(6, 10)
        
        val buttonLocationOnScreen = moreActionsButton.locationOnScreen
        val buttonCenterX = buttonLocationOnScreen.x + moreActionsButton.width / 2
        
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
        val pointerColor = JBColor(Color(r, g, b), Color(r, g, b))
        
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
            .setBorderColor(JBColor(Color(0, 0, 0, 0), Color(0, 0, 0, 0)))
            .setBorderInsets(JBUI.emptyInsets())
            .setPointerSize(JBUI.size(8, 4))
            .setShadow(false)
            .setFadeoutTime(10000)
            .setHideOnClickOutside(true)
            .setHideOnKeyOutside(true)
            .setHideOnAction(true)
            .setAnimationCycle(200)
            .createBalloon()
        
        val point = RelativePoint(moreActionsButton, Point(moreActionsButton.width / 2, moreActionsButton.height))
        balloon.show(point, Balloon.Position.below)
        
        return true
    }
    
    @Suppress("DialogTitleCapitalization")
    private fun findMoreActionsButton(component: Component): Component? {
        if (component is ActionButton) {
            val action = component.action
            val presentation = action.templatePresentation
            val text = presentation.text
            val description = presentation.description
            
            if (text?.contains("More Actions", ignoreCase = true) == true ||
                description?.contains("More Actions", ignoreCase = true) == true ||
                text?.contains("Configure", ignoreCase = true) == true ||
                description?.contains("Configure", ignoreCase = true) == true ||
                action.javaClass.name.contains("Configure", ignoreCase = true) ||
                action.javaClass.name.contains("MoreActions", ignoreCase = true)) {
                LOG.info("Found More Actions ActionButton: ${action.javaClass.simpleName}, text='$text', desc='$description'")
                return component
            }
        }
        
        if (component is AbstractButton) {
            val text = component.text
            val accessibleName = component.accessibleContext?.accessibleName
            val toolTip = component.toolTipText
            
            if (text?.contains("More Actions", ignoreCase = true) == true ||
                accessibleName?.contains("More Actions", ignoreCase = true) == true ||
                toolTip?.contains("More Actions", ignoreCase = true) == true ||
                toolTip?.contains("Configure", ignoreCase = true) == true) {
                LOG.info("Found More Actions button: ${component.javaClass.simpleName}, text='$text', tooltip='$toolTip'")
                return component
            }
        }
        
        if (component is java.awt.Container) {
            for (child in component.components) {
                val result = findMoreActionsButton(child)
                if (result != null) {
                    return result
                }
            }
        }
        
        return null
    }
}

