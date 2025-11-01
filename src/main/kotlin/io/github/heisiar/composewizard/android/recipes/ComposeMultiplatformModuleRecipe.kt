package io.github.heisiar.composewizard.android.recipes

import com.android.tools.idea.wizard.template.ModuleTemplateData
import com.android.tools.idea.wizard.template.RecipeExecutor
import java.io.File

fun RecipeExecutor.generateComposeMultiplatformModule(
  data: ModuleTemplateData,
  enableAndroid: Boolean,
  enableIos: Boolean,
  enableDesktop: Boolean,
  enableWeb: Boolean
) {
  val packageName = data.packageName
  val moduleName = data.name
  
  createDirectory(data.rootDir)
  addIncludeToSettings(moduleName)
  
  save(
    buildComposeMultiplatformGradle(
      packageName = packageName,
      moduleName = moduleName,
      enableAndroid = enableAndroid,
      enableIos = enableIos,
      enableDesktop = enableDesktop,
      enableWeb = enableWeb
    ),
    data.rootDir.resolve("build.gradle.kts")
  )
  
  save(gitignore(), data.rootDir.resolve(".gitignore"))
  
  if (enableAndroid) {
    createAndroidSource(packageName, data.rootDir)
  }
  
  createCommonSource(packageName, data.rootDir)
  
  if (enableIos) {
    createIosSource(packageName, data.rootDir)
  }
  
  if (enableDesktop) {
    createDesktopSource(packageName, data.rootDir)
  }
  
  if (enableWeb) {
    createWebSource(packageName, data.rootDir)
  }
}

private fun RecipeExecutor.createAndroidSource(packageName: String, rootDir: File) {
  val androidMainDir = rootDir.resolve("src/androidMain/kotlin/${packageName.replace('.', '/')}")
  createDirectory(androidMainDir)
  
  save(
    androidMainActivity(packageName),
    androidMainDir.resolve("MainActivity.kt")
  )
  
  val androidManifest = rootDir.resolve("src/androidMain/AndroidManifest.xml")
  save(androidManifestXml(packageName), androidManifest)
}

private fun RecipeExecutor.createCommonSource(packageName: String, rootDir: File) {
  val commonMainDir = rootDir.resolve("src/commonMain/kotlin/${packageName.replace('.', '/')}")
  createDirectory(commonMainDir)
  
  save(
    composeApp(packageName),
    commonMainDir.resolve("App.kt")
  )
  
  save(
    composePlatform(packageName),
    commonMainDir.resolve("Platform.kt")
  )
  
  save(
    composeGreeting(packageName),
    commonMainDir.resolve("Greeting.kt")
  )
}

private fun RecipeExecutor.createIosSource(packageName: String, rootDir: File) {
  val iosMainDir = rootDir.resolve("src/iosMain/kotlin/${packageName.replace('.', '/')}")
  createDirectory(iosMainDir)
  
  save(
    iosPlatform(packageName),
    iosMainDir.resolve("Platform.ios.kt")
  )
}

private fun RecipeExecutor.createDesktopSource(packageName: String, rootDir: File) {
  val desktopMainDir = rootDir.resolve("src/desktopMain/kotlin/${packageName.replace('.', '/')}")
  createDirectory(desktopMainDir)
  
  save(
    desktopMain(packageName),
    desktopMainDir.resolve("main.kt")
  )
}

private fun RecipeExecutor.createWebSource(packageName: String, rootDir: File) {
  val wasmJsMainDir = rootDir.resolve("src/wasmJsMain/kotlin/${packageName.replace('.', '/')}")
  createDirectory(wasmJsMainDir)
  
  save(
    webMain(packageName),
    wasmJsMainDir.resolve("main.kt")
  )
}

private fun gitignore() = """
.gradle
build/
local.properties
.idea
*.iml
.DS_Store
""".trimIndent()

