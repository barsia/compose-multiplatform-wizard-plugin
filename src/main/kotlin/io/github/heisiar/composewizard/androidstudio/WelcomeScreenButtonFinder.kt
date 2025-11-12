package io.github.heisiar.composewizard.androidstudio

import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.diagnostic.Logger
import java.awt.Component
import java.awt.Container
import javax.swing.AbstractButton

class WelcomeScreenButtonFinder {
    
    private val LOG = Logger.getInstance(WelcomeScreenButtonFinder::class.java)
    
    @Suppress("DialogTitleCapitalization")
    fun findActionsButton(component: Component, hasOpenProjects: Boolean): Component? {
        if (component is ActionButton) {
            val action = component.action
            val presentation = action.templatePresentation
            val text = presentation.text
            val description = presentation.description
            val accessibleName = component.accessibleContext.accessibleName
            
            if (hasOpenProjects) {
                if (text == null || text.isEmpty() || text.isBlank()) {
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
            val accessibleName = component.accessibleContext.accessibleName
            val toolTip = component.toolTipText
            
            if (hasOpenProjects) {
                if (toolTip?.contains("Settings", ignoreCase = true) == true ||
                    toolTip?.contains("Options", ignoreCase = true) == true ||
                    accessibleName?.contains("Settings", ignoreCase = true) == true) {
                    LOG.info("Found settings button on Welcome Screen: ${component.javaClass.simpleName}, tooltip='$toolTip'")
                    return component
                }
            } else {
                if (text?.contains("More Actions", ignoreCase = true) == true ||
                    accessibleName?.contains("More Actions", ignoreCase = true) == true ||
                    toolTip?.contains("More Actions", ignoreCase = true) == true ||
                    toolTip?.contains("Configure", ignoreCase = true) == true) {
                    LOG.info("Found More Actions button on Welcome Screen: ${component.javaClass.simpleName}, text='$text', tooltip='$toolTip'")
                    return component
                }
            }
        }
        
        if (component is Container) {
            for (child in component.components) {
                val result = findActionsButton(child, hasOpenProjects)
                if (result != null) {
                    return result
                }
            }
        }
        
        return null
    }
    
    fun isValidWelcomeScreenButton(button: Component): Boolean {
        if (!button.isVisible) {
            LOG.debug("WelcomeScreenTooltipListener: Button is not visible")
            return false
        }
        
        if (button is AbstractButton && !button.isEnabled) {
            LOG.debug("WelcomeScreenTooltipListener: Button is not enabled")
            return false
        }
        
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
        
        var parent = button.parent
        var foundToolbar = false
        var depth = 0
        val maxDepth = 10
        
        while (parent != null && depth < maxDepth) {
            val parentClassName = parent.javaClass.name
            val parentSuperClass = parent.javaClass.superclass?.name ?: "null"
            
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
}

