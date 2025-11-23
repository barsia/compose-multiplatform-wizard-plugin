package io.github.heisiar.composewizard.androidstudio

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import io.github.heisiar.composewizard.shared.models.ComposeMultiplatformModuleBuilder
import io.github.heisiar.composewizard.shared.wizard.AbstractWizardIntegration
import java.io.File

/**
 * Android Studio specific wizard integration.
 * 
 * Handles AS-specific project setup:
 * - Opens project using ProjectManager.loadAndOpenProject()
 * - Uses GitRepositoryInitializer API for proper Git initialization (respects .gitignore)
 * - No additional setup needed before opening (AS handles Gradle sync automatically)
 */
class ASWizardIntegration : AbstractWizardIntegration() {
    
    private var openedProject: com.intellij.openapi.project.Project? = null
    private var shouldInitGit = false
    
    override fun performIdeSpecificSetup(
        projectPath: String,
        projectName: String,
        builder: ComposeMultiplatformModuleBuilder
    ) {
        shouldInitGit = builder.initGit
    }
    
    override fun openProject(projectPath: String) {
        ApplicationManager.getApplication().invokeLater {
            try {
                val projectManager = com.intellij.openapi.project.ex.ProjectManagerEx.getInstanceEx()
                
                // Open project first, then configure
                val newProject = projectManager.loadAndOpenProject(projectPath)
                
                if (newProject != null) {
                    // Configure project AFTER opening
                    val root = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(File(projectPath))
                    if (root != null) {
                        // Synchronous VFS refresh
                        root.refresh(false, true)
                        
                        // Configure Gradle and Git
                        configureProjectAfterOpen(newProject, projectPath, root)
                    }
                }
                
                val finalProject = newProject
                
                if (finalProject != null) {
                    openedProject = finalProject
                } else {
                    Messages.showErrorDialog(
                        "Failed to open the created project",
                        "Project Creation Error"
                    )
                }
            } catch (e: com.intellij.openapi.progress.ProcessCanceledException) {
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
                Messages.showErrorDialog(
                    "Error opening project: ${e.message}",
                    "Project Creation Error"
                )
            }
        }
    }
    
    /**
     * Configure project after opening.
     * This runs on EDT after project is opened.
     */
    private fun configureProjectAfterOpen(
        project: com.intellij.openapi.project.Project,
        projectPath: String,
        root: com.intellij.openapi.vfs.VirtualFile
    ) {
        try {
            // CRITICAL: Enable all Gradle tasks (not just test tasks)
            // By default AS sets SKIP_GRADLE_TASKS_LIST=true which shows only test tasks
            try {
                val experimentalSettings = Class.forName("com.android.tools.idea.gradle.project.GradleExperimentalSettings")
                val getInstance = experimentalSettings.getMethod("getInstance")
                val instance = getInstance.invoke(null)
                val skipTasksField = experimentalSettings.getField("SKIP_GRADLE_TASKS_LIST")
                skipTasksField.setBoolean(instance, false)
            } catch (e: Exception) {
                // If we can't set this (e.g., in IntelliJ IDEA), it's not critical
            }
            
            // VFS refresh to ensure all files are visible
            root.refresh(false, true)
            
            // Initialize Git in background thread (NEVER on EDT!)
            if (shouldInitGit) {
                ApplicationManager.getApplication().executeOnPooledThread {
                    try {
                        // Step 1: Initialize Git repository using git command directly
                        // We DON'T use GitRepositoryInitializer because it adds unwanted
                        // "Project exclude paths" to .gitignore (like /composeApp/, /gradle/)
                        // Our .gitignore was already created by ProjectCreator with correct content
                        initGitDirectly(projectPath)
                        
                        // Step 2: Add all files using git command
                        // This makes files appear as "green" (staged) in the IDE
                        addFilesToGit(projectPath)
                        
                        // Step 3: Refresh VFS to update file statuses in IDE
                        ApplicationManager.getApplication().invokeLater {
                            root.refresh(false, true)
                        }
                    } catch (e: Exception) {
                        thisLogger().error("Failed to initialize Git", e)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    override fun initializeGit(projectPath: String) {
        // Git initialization is handled in openProject() for AS
        // This is necessary because GitRepositoryInitializer requires an open project
    }
    
    /**
     * Initialize Git repository using git command directly.
     * We avoid GitRepositoryInitializer because it pollutes .gitignore with unwanted exclude paths.
     */
    private fun initGitDirectly(projectPath: String): Boolean {
        return try {
            val projectDir = File(projectPath)
            val initProcess = Runtime.getRuntime().exec(
                arrayOf("git", "init"),
                null,
                projectDir
            )
            val exitCode = initProcess.waitFor()
            if (exitCode == 0) {
                thisLogger().info("Git repository initialized successfully")
                true
            } else {
                thisLogger().warn("Git init failed with exit code $exitCode")
                false
            }
        } catch (e: Exception) {
            thisLogger().error("Failed to initialize Git repository", e)
            false
        }
    }
    
    /**
     * Add all project files to Git staging area.
     * This makes files appear as "green" (staged) in the IDE.
     */
    private fun addFilesToGit(projectPath: String): Boolean {
        return try {
            val projectDir = File(projectPath)
            val addProcess = Runtime.getRuntime().exec(
                arrayOf("git", "add", "."),
                null,
                projectDir
            )
            val exitCode = addProcess.waitFor()
            if (exitCode == 0) {
                thisLogger().info("All files staged successfully")
                true
            } else {
                thisLogger().warn("Git add failed with exit code $exitCode")
                false
            }
        } catch (e: Exception) {
            thisLogger().error("Failed to stage files", e)
            false
        }
    }
    
    override fun handleCreationError(
        projectPath: String,
        projectName: String,
        errorMessage: String
    ) {
        super.handleCreationError(projectPath, projectName, errorMessage)
        
        ApplicationManager.getApplication().invokeLater {
            Messages.showErrorDialog(
                "Failed to create project '$projectName'\n\nReason: $errorMessage",
                "Project Creation Error"
            )
        }
    }
}

