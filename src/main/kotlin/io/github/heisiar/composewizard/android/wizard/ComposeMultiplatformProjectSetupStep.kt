package io.github.heisiar.composewizard.android.wizard

import com.android.tools.adtui.util.FormScalingUtil
import com.android.tools.idea.npw.model.NewProjectModel
import com.android.tools.idea.observable.core.BoolValueProperty
import com.android.tools.idea.observable.core.ObservableBool
import com.android.tools.idea.wizard.model.ModelWizardStep
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.io.FileUtil
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import icons.StudioIcons
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.io.File
import javax.swing.*

/**
 * Wizard step for configuring Compose Multiplatform project.
 */
class ComposeMultiplatformProjectSetupStep(
  private val model: NewProjectModel
) : ModelWizardStep<NewProjectModel>(model, "Configure Compose Multiplatform Project") {

  private val projectNameField = JBTextField("ComposeProject")
  private val projectLocationField = TextFieldWithBrowseButton()
  private val packageNameField = JBTextField("com.example.project")
  
  private val enableIosCheckbox = JBCheckBox("iOS", true)
  private val enableDesktopCheckbox = JBCheckBox("Desktop (JVM)", true)
  private val enableWebCheckbox = JBCheckBox("Web (Wasm)", false)
  
  private val canGoForward = BoolValueProperty(true)
  
  private val rootPanel: JPanel
  
  init {
    rootPanel = createContentPanel()
    FormScalingUtil.scaleComponentTree(this.javaClass, rootPanel)
    
    // Set default location
    projectLocationField.text = getDefaultProjectLocation() + "/ComposeProject"
    
    // Update location when name changes
    projectNameField.document.addDocumentListener(object : javax.swing.event.DocumentListener {
      override fun insertUpdate(e: javax.swing.event.DocumentEvent?) = updateLocation()
      override fun removeUpdate(e: javax.swing.event.DocumentEvent?) = updateLocation()
      override fun changedUpdate(e: javax.swing.event.DocumentEvent?) = updateLocation()
      
      private fun updateLocation() {
        val name = projectNameField.text.takeIf { it.isNotBlank() } ?: "ComposeProject"
        projectLocationField.text = getDefaultProjectLocation() + "/$name"
      }
    })
  }
  
  private fun createContentPanel(): JPanel {
    val panel = JPanel(GridBagLayout())
    val c = GridBagConstraints()
    
    c.fill = GridBagConstraints.HORIZONTAL
    c.insets = JBUI.insets(4)
    c.gridx = 0
    c.gridy = 0
    c.weightx = 0.0
    
    // Project Name
    panel.add(JBLabel("Project name:"), c)
    c.gridx = 1
    c.weightx = 1.0
    panel.add(projectNameField, c)
    
    // Project Location
    c.gridx = 0
    c.gridy++
    c.weightx = 0.0
    panel.add(JBLabel("Project location:"), c)
    c.gridx = 1
    c.weightx = 1.0
    projectLocationField.addBrowseFolderListener(
      "Select Project Location",
      "Choose directory for new project",
      null,
      FileChooserDescriptorFactory.createSingleFolderDescriptor()
    )
    panel.add(projectLocationField, c)
    
    // Package Name
    c.gridx = 0
    c.gridy++
    c.weightx = 0.0
    panel.add(JBLabel("Package name:"), c)
    c.gridx = 1
    c.weightx = 1.0
    panel.add(packageNameField, c)
    
    // Separator
    c.gridx = 0
    c.gridy++
    c.gridwidth = 2
    panel.add(JSeparator(), c)
    
    // Target Platforms Label
    c.gridy++
    c.gridwidth = 2
    val platformsLabel = JBLabel("Target Platforms:").apply {
      font = font.deriveFont(font.style or java.awt.Font.BOLD)
    }
    panel.add(platformsLabel, c)
    
    // Android (always enabled)
    c.gridy++
    val androidCheckbox = JBCheckBox("Android", true).apply {
      isEnabled = false
      toolTipText = "Android platform is always included"
    }
    panel.add(androidCheckbox, c)
    
    // iOS
    c.gridy++
    enableIosCheckbox.toolTipText = "Include iOS targets (iosArm64, iosX64, iosSimulatorArm64)"
    panel.add(enableIosCheckbox, c)
    
    // Desktop
    c.gridy++
    enableDesktopCheckbox.toolTipText = "Include Desktop JVM target"
    panel.add(enableDesktopCheckbox, c)
    
    // Web
    c.gridy++
    enableWebCheckbox.toolTipText = "Include Web target with WebAssembly (experimental)"
    panel.add(enableWebCheckbox, c)
    
    // Info panel
    c.gridy++
    c.insets = JBUI.insets(12, 4, 4, 4)
    val infoPanel = JPanel().apply {
      layout = BoxLayout(this, BoxLayout.X_AXIS)
      border = JBUI.Borders.empty(8)
      background = JBUI.CurrentTheme.Banner.INFO_BACKGROUND
      
      add(JLabel(StudioIcons.Common.INFO))
      add(Box.createHorizontalStrut(8))
      add(JLabel("<html>This will create a Compose Multiplatform project<br>" +
                  "with shared UI code across selected platforms.</html>"))
    }
    panel.add(infoPanel, c)
    
    // Spacer
    c.gridy++
    c.weighty = 1.0
    panel.add(Box.createVerticalGlue(), c)
    
    return panel
  }
  
  private fun getDefaultProjectLocation(): String {
    val userHome = System.getProperty("user.home")
    return FileUtil.toSystemIndependentName("$userHome/AndroidStudioProjects")
  }
  
  override fun onProceeding() {
    val projectName = projectNameField.text.takeIf { it.isNotBlank() } ?: "ComposeProject"
    val projectPath = projectLocationField.text
    val packageName = packageNameField.text.takeIf { it.isNotBlank() } ?: "com.example.project"
    
    val enableIos = enableIosCheckbox.isSelected
    val enableDesktop = enableDesktopCheckbox.isSelected
    val enableWeb = enableWebCheckbox.isSelected
    
    createProjectStructure(projectName, projectPath, packageName, enableIos, enableDesktop, enableWeb)
  }
  
  private fun createProjectStructure(
    projectName: String,
    projectPath: String,
    packageName: String,
    enableIos: Boolean,
    enableDesktop: Boolean,
    enableWeb: Boolean
  ) {
    val projectDir = File(projectPath)
    if (!projectDir.exists()) {
      projectDir.mkdirs()
    }
    
    // Create settings.gradle.kts
    File(projectDir, "settings.gradle.kts").writeText("""
rootProject.name = "$projectName"
include(":composeApp")
    """.trimIndent())
    
    // Create gradle.properties
    File(projectDir, "gradle.properties").writeText("""
kotlin.code.style=official
android.useAndroidX=true
org.jetbrains.compose.experimental.uikit.enabled=true
    """.trimIndent())
    
    // Create composeApp directory
    val composeAppDir = File(projectDir, "composeApp")
    composeAppDir.mkdirs()
    
    // Create build.gradle.kts for composeApp
    val iosTargets = if (enableIos) {
      """
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
      """.trimIndent()
    } else ""
    
    val desktopTarget = if (enableDesktop) "jvm(\"desktop\")" else ""
    val webTarget = if (enableWeb) """
    wasmJs {
        browser()
        binaries.executable()
    }
    """.trimIndent() else ""
    
    File(composeAppDir, "build.gradle.kts").writeText("""
plugins {
    kotlin("multiplatform")
    id("com.android.application")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    androidTarget()
    
    $iosTargets
    
    $desktopTarget
    
    $webTarget
    
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
        }
        
        androidMain.dependencies {
            implementation("androidx.activity:activity-compose:1.9.0")
        }
    }
}

android {
    namespace = "$packageName"
    compileSdk = 34
    
    defaultConfig {
        applicationId = "$packageName"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    
    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
    """.trimIndent())
    
    // Create source directories
    val packagePath = packageName.replace('.', '/')
    
    // commonMain
    val commonMainDir = File(composeAppDir, "src/commonMain/kotlin/$packagePath")
    commonMainDir.mkdirs()
    
    File(commonMainDir, "App.kt").writeText("""
package $packageName

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
                Text(
                    text = "Hello, Compose Multiplatform!",
                    style = MaterialTheme.typography.headlineMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(onClick = { }) {
                    Text("Click me!")
                }
            }
        }
    }
}
    """.trimIndent())
    
    // androidMain
    val androidMainDir = File(composeAppDir, "src/androidMain/kotlin/$packagePath")
    androidMainDir.mkdirs()
    
    File(androidMainDir, "MainActivity.kt").writeText("""
package $packageName

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
    """.trimIndent())
    
    // AndroidManifest.xml
    val androidMainResDir = File(composeAppDir, "src/androidMain")
    androidMainResDir.mkdirs()
    
    File(androidMainResDir, "AndroidManifest.xml").writeText("""
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:allowBackup="true"
        android:supportsRtl="true"
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
    """.trimIndent())
    
    println("✅ Compose Multiplatform project created at: ${projectDir.absolutePath}")
  }
  
  override fun canGoForward(): ObservableBool = canGoForward
  
  override fun getComponent(): JComponent = rootPanel
  
  override fun getPreferredFocusComponent(): JComponent = projectNameField
}
