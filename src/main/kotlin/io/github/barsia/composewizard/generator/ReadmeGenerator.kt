package io.github.barsia.composewizard.generator

object ReadmeGenerator {
    
    fun generate(config: ProjectConfig): String = buildString {
        appendLine("# ${config.projectName}")
        appendLine()
        
        // Platform list - always in this order: Android, iOS, Desktop (JVM), Web
        val platforms = buildList {
            if (config.targetAndroid) add("Android")
            if (config.targetIOS) add("iOS")
            if (config.targetDesktop) add("Desktop (JVM)")
            if (config.targetWeb) add("Web")
        }
        appendLine("This is a Kotlin Multiplatform project targeting ${platforms.joinToString(", ")}.")
        appendLine()
        
        // Project structure
        appendLine("* [/composeApp](./composeApp/src) is for code that will be shared across your Compose Multiplatform applications.")
        appendLine("  It contains several subfolders:")
        appendLine("    - [commonMain](./composeApp/src/commonMain/kotlin) is for code that's common for all targets.")
        appendLine("    - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.")
        
        // Generic platform-specific folder examples (always shown)
        appendLine("      For example, if you want to use Apple's CoreCrypto for the iOS part of your Kotlin app,")
        appendLine("      the [iosMain](./composeApp/src/iosMain/kotlin) folder would be the right place for such calls.")
        appendLine("      Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./composeApp/src/jvmMain/kotlin)")
        appendLine("      folder is the appropriate location.")
        
        // iOS app entry point
        if (config.targetIOS) {
            appendLine()
            appendLine("* [/iosApp](./iosApp/iosApp) contains iOS applications. Even if you're sharing your UI with Compose Multiplatform,")
            appendLine("  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.")
        }
        
        // Platform-specific build instructions - always in order: Android, iOS, Desktop, Web
        if (config.targetAndroid) {
            appendLine()
            appendLine("### Build and Run Android Application")
            appendLine()
            appendLine("To build and run the development version of the Android app, use the run configuration from the run widget")
            appendLine("in your IDE's toolbar or build it directly from the terminal:")
            appendLine("- on macOS/Linux")
            appendLine("  ```shell")
            appendLine("  ./gradlew :composeApp:assembleDebug")
            appendLine("  ```")
            appendLine("- on Windows")
            appendLine("  ```shell")
            appendLine("  .\\gradlew.bat :composeApp:assembleDebug")
            appendLine("  ```")
        }
        
        if (config.targetIOS) {
            appendLine()
            appendLine("### Build and Run iOS Application")
            appendLine()
            appendLine("To build and run the development version of the iOS app, use the run configuration from the run widget")
            appendLine("in your IDE's toolbar or open the [/iosApp](./iosApp) directory in Xcode and run it from there.")
        }
        
        if (config.targetDesktop) {
            appendLine()
            appendLine("### Build and Run Desktop (JVM) Application")
            appendLine()
            appendLine("To build and run the development version of the desktop app, use the run configuration from the run widget")
            appendLine("in your IDE's toolbar or run it directly from the terminal:")
            appendLine("- on macOS/Linux")
            appendLine("  ```shell")
            appendLine("  ./gradlew :composeApp:run")
            appendLine("  ```")
            appendLine("- on Windows")
            appendLine("  ```shell")
            appendLine("  .\\gradlew.bat :composeApp:run")
            appendLine("  ```")
        }
        
        if (config.targetWeb) {
            appendLine()
            appendLine("### Build and Run Web Application")
            appendLine()
            appendLine("To build and run the development version of the web app, use the run configuration from the run widget")
            appendLine("in your IDE's toolbar or run it directly from the terminal:")
            appendLine("- for the Wasm target (faster, modern browsers):")
            appendLine("  - on macOS/Linux")
            appendLine("    ```shell")
            appendLine("    ./gradlew :composeApp:wasmJsBrowserDevelopmentRun")
            appendLine("    ```")
            appendLine("  - on Windows")
            appendLine("    ```shell")
            appendLine("    .\\gradlew.bat :composeApp:wasmJsBrowserDevelopmentRun")
            appendLine("    ```")
            appendLine("- for the JS target (slower, supports older browsers):")
            appendLine("  - on macOS/Linux")
            appendLine("    ```shell")
            appendLine("    ./gradlew :composeApp:jsBrowserDevelopmentRun")
            appendLine("    ```")
            appendLine("  - on Windows")
            appendLine("    ```shell")
            appendLine("    .\\gradlew.bat :composeApp:jsBrowserDevelopmentRun")
            appendLine("    ```")
            appendLine()
            appendLine("We would appreciate your feedback on Compose/Web and Kotlin/Wasm in the public Slack channel [#compose-web](https://slack-chats.kotlinlang.org/c/compose-web).")
            appendLine("If you face any issues, please report them on [YouTrack](https://youtrack.jetbrains.com/newIssue?project=CMP).")
        }
        
        // Footer
        appendLine()
        appendLine("---")
        appendLine()
        appendLine("Learn more about [Compose Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-multiplatform.html).")
    }
}

