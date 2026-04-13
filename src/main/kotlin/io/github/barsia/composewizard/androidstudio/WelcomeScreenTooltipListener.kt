package io.github.barsia.composewizard.androidstudio

import com.intellij.ide.AppLifecycleListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.WindowManager
import io.github.barsia.composewizard.shared.settings.WizardSettings
import java.awt.Window

class WelcomeScreenTooltipListener : AppLifecycleListener {
    
    private val LOG = Logger.getInstance(WelcomeScreenTooltipListener::class.java)
    private val buttonFinder = WelcomeScreenButtonFinder()
    private val tooltipUI = WelcomeTooltipUI()
    
    override fun appFrameCreated(commandLineArgs: MutableList<String>) {
        val settings = WizardSettings.getInstance()
        
        LOG.info("WelcomeScreenTooltipListener: appFrameCreated called")
        
        if (settings.welcomeTooltipShown) {
            LOG.info("WelcomeScreenTooltipListener: tooltip already shown, skipping")
            return
        }
        
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
                    Thread.sleep(200)
                }
            }
            
            if (!frameFound) {
                LOG.warn("WelcomeScreenTooltipListener: Failed to find visible frame after $maxAttempts attempts")
            }
        }
    }
    
    private fun attachToFrame(frame: Window) {
        val frameClassName = frame.javaClass.simpleName
        val isWelcomeScreen = frameClassName.contains("Welcome", ignoreCase = true) || 
                              frameClassName.contains("FlatWelcome", ignoreCase = true)
        
        if (!isWelcomeScreen) {
            LOG.debug("WelcomeScreenTooltipListener: not a Welcome Screen frame: $frameClassName")
            return
        }
        
        LOG.info("WelcomeScreenTooltipListener: Welcome Screen detected, searching for More Actions button")
        
        val settings = WizardSettings.getInstance()
        
        val openProjects = ProjectManager.getInstance().openProjects
        val hasOpenProjects = openProjects.isNotEmpty()
        
        ApplicationManager.getApplication().executeOnPooledThread {
            var attempts = 0
            val maxAttempts = 10
            var moreActionsButton: java.awt.Component? = null
            
            while (attempts < maxAttempts && moreActionsButton == null) {
                attempts++
                moreActionsButton = buttonFinder.findActionsButton(frame, hasOpenProjects)
                
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
            
            if (!buttonFinder.isValidWelcomeScreenButton(moreActionsButton)) {
                LOG.warn("WelcomeScreenTooltipListener: Found button is not valid (invisible, disabled, or wrong context)")
                return@executeOnPooledThread
            }
            
            Thread.sleep(500)
            
            ApplicationManager.getApplication().invokeLater {
                tooltipUI.showWelcomeTooltipForButton(moreActionsButton)
                settings.welcomeTooltipShown = true
                LOG.info("WelcomeScreenTooltipListener: tooltip shown successfully")
            }
        }
    }
}
