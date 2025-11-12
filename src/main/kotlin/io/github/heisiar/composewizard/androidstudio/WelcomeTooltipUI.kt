package io.github.heisiar.composewizard.androidstudio

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.JBColor
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.GradientPaint
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Point
import java.awt.RenderingHints
import javax.swing.JComponent
import javax.swing.JLabel

class WelcomeTooltipUI {
    
    private val LOG = Logger.getInstance(WelcomeTooltipUI::class.java)
    
    fun showWelcomeTooltipForButton(targetButton: Component): Boolean {
        val frame = WindowManager.getInstance().findVisibleFrame()
        if (frame == null) {
            LOG.debug("WelcomeScreenTooltipListener: no visible frame found")
            return false
        }
        
        LOG.info("WelcomeScreenTooltipListener: Showing tooltip for button: ${targetButton.javaClass.name}")
        
        val colorStart = JBColor(0x49C0FF, 0x49C0FF)
        val colorEnd = JBColor(0x6C4CFF, 0x6C4CFF)
        
        val label = JLabel("Create Compose Multiplatform projects here")
        label.foreground = Color.WHITE
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
        
        val point = RelativePoint(targetButton, Point(targetButton.width / 2, targetButton.height))
        balloon.show(point, Balloon.Position.below)
        
        return true
    }
}

