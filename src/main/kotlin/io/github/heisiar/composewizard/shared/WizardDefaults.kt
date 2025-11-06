package io.github.heisiar.composewizard.shared

/**
 * Single source of truth for ALL default values in Compose Multiplatform Wizard.
 * 
 * This object ensures consistency across:
 * - IntelliJ IDEA wizard (Compose UI)
 * - Android Studio Action (Compose UI)
 * - Android Studio Template wizard (native AS wizard)
 * - Module builder
 * 
 * ⚠️ CRITICAL: When changing defaults, update ONLY this file!
 * Any duplication of these values is a bug.
 */
object WizardDefaults {
    
    // ========== Project Identity ==========
    
    /**
     * Default project name (without spaces for file system compatibility)
     */
    const val PROJECT_NAME = "MyApplication"
    
    /**
     * Default project name with spaces (for display in UI)
     */
    const val PROJECT_NAME_DISPLAY = "My Application"
    
    /**
     * Default package name
     */
    const val PACKAGE_NAME = "com.example.myapplication"
    
    /**
     * Get user home directory, avoiding Gradle cache paths
     * 
     * When IDE is launched via gradle runIde, System.getProperty("user.home") 
     * points to .gradle/caches instead of the real user home.
     * We detect this and fall back to HOME environment variable.
     */
    private fun getUserHome(): String {
        val systemHome = com.intellij.util.SystemProperties.getUserHome()
        val envHome = System.getenv("HOME")
        
        println("[WizardDefaults] System.getProperty('user.home') = $systemHome")
        println("[WizardDefaults] System.getenv('HOME') = $envHome")
        
        // If user.home points to .gradle/caches, use HOME environment variable instead
        val result = if (systemHome.contains(".gradle") || systemHome.contains("caches")) {
            println("[WizardDefaults] Detected .gradle/caches in user.home, using HOME env var")
            envHome ?: systemHome
        } else {
            println("[WizardDefaults] Using user.home as is")
            systemHome
        }
        
        println("[WizardDefaults] Final getUserHome() = $result")
        return result
    }
    
    /**
     * Default project location for Android Studio
     * 
     * Note: Use SystemProperties.getUserHome() instead of System.getProperty("user.home")
     * because when IDE is launched from Gradle, user.home can point to .gradle/caches
     */
    val PROJECT_PATH_ANDROID_STUDIO: String
        get() = getUserHome() + "/AndroidStudioProjects"
    
    /**
     * Default project location for IntelliJ IDEA
     * 
     * Note: Use SystemProperties.getUserHome() instead of System.getProperty("user.home")
     * because when IDE is launched from Gradle, user.home can point to .gradle/caches
     */
    val PROJECT_PATH_IDEA: String
        get() = getUserHome() + "/IdeaProjects"
    
    /**
     * Get default project path based on current platform
     */
    fun getDefaultProjectPath(): String {
        return if (PlatformDetector.isAndroidStudio) {
            PROJECT_PATH_ANDROID_STUDIO
        } else {
            PROJECT_PATH_IDEA
        }
    }
    
    // ========== Target Platforms ==========
    
    /**
     * Default: Include Desktop (JVM) target.
     * Most developers want cross-platform support.
     */
    const val TARGET_DESKTOP = true
    
    /**
     * Default: Include Android target.
     * Android is a primary Compose Multiplatform target.
     */
    const val TARGET_ANDROID = true
    
    /**
     * Default: Include iOS target.
     * iOS is a primary Compose Multiplatform target.
     */
    const val TARGET_IOS = true
    
    /**
     * Default: Include Web (Wasm) target.
     * Web support is stable and useful for demos.
     */
    const val TARGET_WEB = true
    
    // ========== Project Options ==========
    
    /**
     * Default: Include tests in generated project.
     * 
     * Set to false to reduce initial project complexity for beginners.
     * Users can easily add tests later if needed.
     */
    const val INCLUDE_TESTS = false
    
    /**
     * Default: Initialize Git repository.
     * 
     * Most developers use Git, so enabled by default.
     * This creates .git directory and .gitignore file.
     */
    const val INIT_GIT = true
    
    /**
     * Default: Use production versions (not dev-maven).
     * 
     * Dev versions are for advanced users and early adopters only.
     * Hidden by default, can be enabled via easter egg (triple-click version).
     */
    const val ENABLE_DEV_VERSIONS = false
    
    // ========== Compose Version ==========
    
    /**
     * Default Compose Multiplatform version.
     * Delegates to ComposeVersions for single source of truth.
     */
    val COMPOSE_VERSION: String
        get() = ComposeVersions.DEFAULT_VERSION
    
    // ========== Helper Functions ==========
    
    /**
     * Converts project name to file system safe name.
     * 
     * Removes spaces and special characters that are not allowed in file names.
     * Example: "My Application" -> "MyApplication"
     * 
     * Android Studio uses: [a-zA-Z0-9_] only
     */
    fun sanitizeProjectName(name: String): String {
        // Android Studio removes all chars except alphanumeric and underscore
        return name.replace(Regex("[^a-zA-Z0-9_]"), "")
    }
    
    /**
     * Finds unique project location by appending counter if directory exists.
     * 
     * This mimics Android Studio's behavior:
     * - MyApp (if exists) -> MyApp2
     * - MyApp2 (if exists) -> MyApp3
     * - etc.
     * 
     * Example: findUniqueProjectLocation("My Application", "~/Projects")
     *          -> "~/Projects/MyApplication" (if free)
     *          -> "~/Projects/MyApplication2" (if MyApplication exists)
     */
    fun findUniqueProjectLocation(projectName: String, basePath: String): String {
        val sanitized = sanitizeProjectName(projectName)
        val expandedBase = expandPath(basePath)
        val baseDir = java.io.File(expandedBase)
        
        var projectDir = java.io.File(baseDir, sanitized)
        var counter = 2
        
        // Try appName, appName2, appName3, ...
        while (projectDir.exists()) {
            projectDir = java.io.File(baseDir, "$sanitized$counter")
            counter++
        }
        
        return projectDir.path
    }
    
    /**
     * Collapses user home directory to tilde (~) for display.
     * 
     * Example: "/Users/username/Projects" -> "~/Projects"
     */
    fun collapsePath(path: String): String {
        val userHome = System.getProperty("user.home")
        return if (path.startsWith(userHome)) {
            path.replaceFirst(userHome, "~")
        } else {
            path
        }
    }
    
    /**
     * Expands tilde (~) to user home directory.
     * 
     * Example: "~/Projects" -> "/Users/username/Projects"
     */
    fun expandPath(path: String): String {
        return if (path.startsWith("~")) {
            path.replaceFirst("~", System.getProperty("user.home"))
        } else {
            path
        }
    }
}

