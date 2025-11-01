package io.github.heisiar.composewizard.shared.models

import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.openapi.module.EmptyModuleType
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.util.IconLoader
import io.github.heisiar.composewizard.shared.TemplateProcessor
import javax.swing.Icon

// Forward declaration - avoid circular dependency
// ComposeMultiplatformWizardStep will import this class

data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList()
) {
    fun hasError(message: String): Boolean {
        return errors.any { it.contains(message, ignoreCase = true) }
    }
}

class ComposeMultiplatformModuleBuilder : ModuleBuilder() {

    var projectName: String = "ComposeProject"
    var projectId: String = "org.example.project"
    var composeVersion: String = "1.7.1" // Default Compose version
    var targetDesktop: Boolean = true
    var targetAndroid: Boolean = true
    var targetIOS: Boolean = true
    var targetWeb: Boolean = true
    var initGit: Boolean = true
    var includeTests: Boolean = false
    var enableDevVersions: Boolean = false

    override fun getModuleType(): ModuleType<*> = EmptyModuleType.getInstance()

    override fun getPresentableName(): String = "Compose Multiplatform"

    override fun getDescription(): String =
        "Create a Compose Multiplatform project for desktop, Android, iOS and web"

    override fun getNodeIcon(): Icon = IconLoader.getIcon("/META-INF/compose.svg", ComposeMultiplatformModuleBuilder::class.java)

    override fun getGroupName(): String = "Compose Multiplatform"

    override fun getBuilderId(): String = "COMPOSE_MULTIPLATFORM"

    override fun getWeight(): Int = 1

    override fun setupRootModel(modifiableRootModel: ModifiableRootModel) {
        val path = contentEntryPath ?: return
        val rootPath = FileUtil.toSystemIndependentName(path)

        doAddContentEntry(modifiableRootModel)

        createProjectStructure(path, projectName)

        val root = LocalFileSystem.getInstance().refreshAndFindFileByPath(rootPath)
        root?.refresh(false, true)
    }

    override fun getCustomOptionsStep(context: com.intellij.ide.util.projectWizard.WizardContext?, parentDisposable: com.intellij.openapi.Disposable?): com.intellij.ide.util.projectWizard.ModuleWizardStep {
        return io.github.heisiar.composewizard.shared.ui.ComposeMultiplatformWizardStep(this)
    }

    override fun createWizardSteps(context: com.intellij.ide.util.projectWizard.WizardContext, modulesProvider: com.intellij.openapi.roots.ui.configuration.ModulesProvider): Array<com.intellij.ide.util.projectWizard.ModuleWizardStep> {
        return emptyArray()
    }

    override fun createFinishingSteps(wizardContext: com.intellij.ide.util.projectWizard.WizardContext, modulesProvider: com.intellij.openapi.roots.ui.configuration.ModulesProvider): Array<com.intellij.ide.util.projectWizard.ModuleWizardStep> {
        return emptyArray()
    }

    override fun modifySettingsStep(settingsStep: com.intellij.ide.util.projectWizard.SettingsStep): com.intellij.ide.util.projectWizard.ModuleWizardStep? {
        return null
    }

    override fun modifyProjectTypeStep(settingsStep: com.intellij.ide.util.projectWizard.SettingsStep): com.intellij.ide.util.projectWizard.ModuleWizardStep? {
        return null
    }

    override fun isTemplateBased(): Boolean = false

    override fun isAvailable(): Boolean = true

    fun createProjectStructure(rootPath: String, projectName: String) {
        println("=== Creating project structure ===")
        println("Root path: $rootPath")
        println("Project name: $projectName")
        println("Project ID: $projectId")
        println("Targets: Desktop=$targetDesktop, Android=$targetAndroid, iOS=$targetIOS, Web=$targetWeb")
        println("Include tests: $includeTests")
        
            val processor = TemplateProcessor(
                projectName = projectName,
                projectId = projectId,
                composeVersion = composeVersion,
                includeTests = includeTests,
                targetDesktop = targetDesktop,
                targetAndroid = targetAndroid,
                targetIOS = targetIOS,
                targetWeb = targetWeb,
                enableDevVersions = enableDevVersions
            )
        
        println("Determined template: ${processor.determineTemplate()}")
        processor.copyTemplateToProject(rootPath)
        
        println("Files in root path after copy:")
        java.io.File(rootPath).listFiles()?.forEach { 
            println("  - ${it.name}")
        }
        
        println("=== Project structure created ===")
    }

    private fun createGradleFiles(rootPath: String, projectName: String) {
        val settingsGradle = """
            rootProject.name = "$projectName"

            pluginManagement {
                repositories {
                    google()
                    gradlePluginPortal()
                    mavenCentral()
                }
            }

            dependencyResolutionManagement {
                repositories {
                    google()
                    mavenCentral()
                }
            }
        """.trimIndent()

        val buildGradleKts = buildString {
            appendLine("plugins {")
            appendLine("    kotlin(\"multiplatform\") version \"2.1.0\"")
            appendLine("    id(\"org.jetbrains.compose\") version \"1.10.0-alpha03\"")
            appendLine("    id(\"org.jetbrains.kotlin.plugin.compose\") version \"2.1.0\"")

            if (targetAndroid) {
                appendLine("    id(\"com.android.application\") version \"8.7.3\"")
            }

            appendLine("}")
            appendLine()
            appendLine("kotlin {")

            // Add targets based on selection
            if (targetDesktop) {
                appendLine("    jvm(\"desktop\")")
            }

            if (targetAndroid) {
                appendLine("    androidTarget {")
                appendLine("        compilations.all {")
                appendLine("            kotlinOptions {")
                appendLine("                jvmTarget = \"11\"")
                appendLine("            }")
                appendLine("        }")
                appendLine("    }")
            }

            if (targetIOS) {
                appendLine("    listOf(")
                appendLine("        iosX64(),")
                appendLine("        iosArm64(),")
                appendLine("        iosSimulatorArm64()")
                appendLine("    ).forEach { iosTarget ->")
                appendLine("        iosTarget.binaries.framework {")
                appendLine("            baseName = \"$projectName\"")
                appendLine("            isStatic = true")
                appendLine("        }")
                appendLine("    }")
            }

            if (targetWeb) {
                appendLine("    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)")
                appendLine("    wasmJs {")
                appendLine("        browser()")
                appendLine("        binaries.executable()")
                appendLine("    }")
            }

            appendLine()
            appendLine("    sourceSets {")
            appendLine("        commonMain.dependencies {")
            appendLine("            implementation(compose.runtime)")
            appendLine("            implementation(compose.foundation)")
            appendLine("            implementation(compose.material3)")
            appendLine("            implementation(compose.ui)")
            appendLine("            implementation(compose.components.resources)")
            appendLine("        }")
            appendLine()

            if (targetDesktop) {
                appendLine("        val desktopMain by getting {")
                appendLine("            dependencies {")
                appendLine("                implementation(compose.desktop.currentOs)")
                appendLine("            }")
                appendLine("        }")
                appendLine()
            }

            if (targetAndroid) {
                appendLine("        androidMain.dependencies {")
                appendLine("            implementation(\"androidx.activity:activity-compose:1.9.3\")")
                appendLine("        }")
                appendLine()
            }

            appendLine("    }")
            appendLine("}")
            appendLine()

            if (targetAndroid) {
                appendLine("android {")
                appendLine("    namespace = \"com.example.$projectName\"")
                appendLine("    compileSdk = 35")
                appendLine()
                appendLine("    defaultConfig {")
                appendLine("        minSdk = 24")
                appendLine("        targetSdk = 35")
                appendLine("    }")
                appendLine()
                appendLine("    compileOptions {")
                appendLine("        sourceCompatibility = JavaVersion.VERSION_11")
                appendLine("        targetCompatibility = JavaVersion.VERSION_11")
                appendLine("    }")
                appendLine("}")
                appendLine()
            }

            if (targetDesktop) {
                appendLine("compose.desktop {")
                appendLine("    application {")
                appendLine("        mainClass = \"MainKt\"")
                appendLine("    }")
                appendLine("}")
            }
        }

        val gradleProperties = """
            kotlin.code.style=official
            org.gradle.jvmargs=-Xmx2048M -Dfile.encoding=UTF-8 -Dkotlin.daemon.jvm.options=-Xmx2048M
        """.trimIndent()

        FileUtil.writeToFile(java.io.File(rootPath, "settings.gradle.kts"), settingsGradle)
        FileUtil.writeToFile(java.io.File(rootPath, "build.gradle.kts"), buildGradleKts)
        FileUtil.writeToFile(java.io.File(rootPath, "gradle.properties"), gradleProperties)
    }

    private fun createSourceDirectories(rootPath: String, projectName: String) {
        val commonMainDir = java.io.File(rootPath, "src/commonMain/kotlin")
        commonMainDir.mkdirs()

        val appKt = """
            import androidx.compose.foundation.layout.*
            import androidx.compose.material3.*
            import androidx.compose.runtime.*
            import androidx.compose.ui.Alignment
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.dp

            @Composable
            fun App() {
                MaterialTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            var count by remember { mutableStateOf(0) }

                            Text(
                                text = "Welcome to Compose Multiplatform!",
                                style = MaterialTheme.typography.headlineMedium
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Count: ${"$"}count",
                                style = MaterialTheme.typography.bodyLarge
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(onClick = { count++ }) {
                                Text("Click me!")
                            }
                        }
                    }
                }
            }
        """.trimIndent()

        FileUtil.writeToFile(java.io.File(commonMainDir, "App.kt"), appKt)

        // Create Desktop entry point
        if (targetDesktop) {
            val desktopMainDir = java.io.File(rootPath, "src/desktopMain/kotlin")
            desktopMainDir.mkdirs()

            val mainKt = """
                import androidx.compose.ui.window.Window
                import androidx.compose.ui.window.application

                fun main() = application {
                    Window(
                        onCloseRequest = ::exitApplication,
                        title = "$projectName"
                    ) {
                        App()
                    }
                }
            """.trimIndent()

            FileUtil.writeToFile(java.io.File(desktopMainDir, "Main.kt"), mainKt)
        }

        // Create Android entry point
        if (targetAndroid) {
            val androidMainDir = java.io.File(rootPath, "src/androidMain/kotlin")
            androidMainDir.mkdirs()

            val androidMainKt = """
                import android.os.Bundle
                import androidx.activity.ComponentActivity
                import androidx.activity.compose.setContent

                class MainActivity : ComponentActivity() {
                    override fun onCreate(savedInstanceState: Bundle?) {
                        super.onCreate(savedInstanceState)
                        setContent {
                            App()
                        }
                    }
                }
            """.trimIndent()

            FileUtil.writeToFile(java.io.File(androidMainDir, "MainActivity.kt"), androidMainKt)

            // Create Android manifest
            val androidMainResDir = java.io.File(rootPath, "src/androidMain")
            androidMainResDir.mkdirs()

            val androidManifest = """
                <?xml version="1.0" encoding="utf-8"?>
                <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                    <application
                        android:allowBackup="true"
                        android:icon="@android:drawable/ic_dialog_info"
                        android:label="$projectName"
                        android:theme="@android:style/Theme.Material.Light.NoActionBar">
                        <activity
                            android:name=".MainActivity"
                            android:exported="true">
                            <intent-filter>
                                <action android:name="android.intent.action.MAIN" />
                                <category android:name="android.intent.category.LAUNCHER" />
                            </intent-filter>
                        </activity>
                    </application>
                </manifest>
            """.trimIndent()

            FileUtil.writeToFile(java.io.File(androidMainResDir, "AndroidManifest.xml"), androidManifest)
        }

        // Create iOS entry point
        if (targetIOS) {
            val iosMainDir = java.io.File(rootPath, "src/iosMain/kotlin")
            iosMainDir.mkdirs()

            val iosMainKt = """
                import androidx.compose.ui.window.ComposeUIViewController

                fun MainViewController() = ComposeUIViewController {
                    App()
                }
            """.trimIndent()

            FileUtil.writeToFile(java.io.File(iosMainDir, "Main.kt"), iosMainKt)
        }

        // Create Web entry point
        if (targetWeb) {
            val wasmJsMainDir = java.io.File(rootPath, "src/wasmJsMain/kotlin")
            wasmJsMainDir.mkdirs()

            val webMainKt = """
                import androidx.compose.ui.ExperimentalComposeUiApi
                import androidx.compose.ui.window.CanvasBasedWindow

                @OptIn(ExperimentalComposeUiApi::class)
                fun main() {
                    CanvasBasedWindow(canvasElementId = "ComposeTarget") {
                        App()
                    }
                }
            """.trimIndent()

            FileUtil.writeToFile(java.io.File(wasmJsMainDir, "Main.kt"), webMainKt)

            // Create HTML file for web
            val webResourcesDir = java.io.File(rootPath, "src/wasmJsMain/resources")
            webResourcesDir.mkdirs()

            val indexHtml = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>$projectName</title>
                    <style>
                        body { margin: 0; padding: 0; }
                        #ComposeTarget { width: 100vw; height: 100vh; }
                    </style>
                </head>
                <body>
                    <canvas id="ComposeTarget"></canvas>
                    <script src="$projectName.js"></script>
                </body>
                </html>
            """.trimIndent()

            FileUtil.writeToFile(java.io.File(webResourcesDir, "index.html"), indexHtml)
        }
    }

    private fun createGitIgnore(rootPath: String) {
        val gitIgnore = """
            .gradle
            build/
            !gradle/wrapper/gradle-wrapper.jar
            !**/src/main/**/build/
            !**/src/test/**/build/
            
            ### IntelliJ IDEA ###
            .idea
            *.iws
            *.iml
            *.ipr
            out/
            !**/src/main/**/out/
            !**/src/test/**/out/
            
            ### Eclipse ###
            .apt_generated
            .classpath
            .factorypath
            .project
            .settings
            .springBeans
            .sts4-cache
            bin/
            !**/src/main/**/bin/
            !**/src/test/**/bin/
            
            ### Mac OS ###
            .DS_Store
        """.trimIndent()

        FileUtil.writeToFile(java.io.File(rootPath, ".gitignore"), gitIgnore)
    }

    private fun createGradleWrapper(rootPath: String) {
        val gradleWrapperDir = java.io.File(rootPath, "gradle/wrapper")
        gradleWrapperDir.mkdirs()

        val gradleWrapperProperties = """
            distributionBase=GRADLE_USER_HOME
            distributionPath=wrapper/dists
            distributionUrl=https\://services.gradle.org/distributions/gradle-8.9-bin.zip
            networkTimeout=10000
            validateDistributionUrl=true
            zipStoreBase=GRADLE_USER_HOME
            zipStorePath=wrapper/dists
        """.trimIndent()

        FileUtil.writeToFile(java.io.File(gradleWrapperDir, "gradle-wrapper.properties"), gradleWrapperProperties)
    }

    private fun initializeGitRepository(rootPath: String) {
        try {
            val processBuilder = ProcessBuilder("git", "init")
            processBuilder.directory(java.io.File(rootPath))
            processBuilder.redirectErrorStream(true)
            val process = processBuilder.start()
            process.waitFor()
        } catch (_: Exception) {
            // Silently ignore git initialization errors
        }
    }
    
    fun validateProjectId(id: String): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (id.isBlank()) {
            errors.add("Project ID must not be empty")
            return ValidationResult(false, errors)
        }
        
        if (!id.contains(".")) {
            errors.add("Project ID must contain at least one '.' separator")
            return ValidationResult(false, errors)
        }
        
        val lastChar = id.last()
        if (!lastChar.isLetterOrDigit() && lastChar != '_') {
            errors.add("Project ID must end with lowercase latin character, digit or '_'")
        }
        
        val parts = id.split(".")
        
        if (parts.any { it.isEmpty() }) {
            errors.add("Project ID must not contain empty parts (consecutive dots)")
        }
        
        parts.forEach { part ->
            if (part.isEmpty()) return@forEach
            
            if (!part[0].isLowerCase()) {
                errors.add("Each part of Project ID must start with lowercase letter")
            }
            
            if (!part.all { it.isLetterOrDigit() || it == '_' }) {
                errors.add("Project ID can only contain lowercase letters, digits, '_' and '.'")
            }
            
            if (part.any { it.isUpperCase() }) {
                errors.add("Project ID must contain only lowercase letters")
            }
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors.distinct()
        )
    }
}
