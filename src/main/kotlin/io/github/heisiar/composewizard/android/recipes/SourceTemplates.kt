package io.github.heisiar.composewizard.android.recipes

fun composeApp(packageName: String) = """
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
                val greeting = Greeting().greet()
                Text(
                    text = greeting,
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
""".trimIndent()

fun composeGreeting(packageName: String) = """
package $packageName

class Greeting {
    private val platform = getPlatform()

    fun greet(): String {
        return "Hello from" + "\n" + platform.name + "!"
    }
}
""".trimIndent()

fun composePlatform(packageName: String) = """
package $packageName

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
""".trimIndent()

fun androidMainActivity(packageName: String) = """
package $packageName

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}

actual fun getPlatform(): Platform = AndroidPlatform()

class AndroidPlatform : Platform {
    override val name: String = "Android" + " " + android.os.Build.VERSION.SDK_INT
}
""".trimIndent()

fun androidManifestXml(packageName: String) = """
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:allowBackup="true"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.Material.Light.NoActionBar">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:configChanges="orientation|screenSize|screenLayout|keyboardHidden">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
""".trimIndent()

fun iosPlatform(packageName: String) = """
package $packageName

import platform.UIKit.UIDevice

actual fun getPlatform(): Platform = IOSPlatform()

class IOSPlatform : Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}
""".trimIndent()

fun desktopMain(packageName: String) = """
package $packageName

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Compose Multiplatform App",
    ) {
        App()
    }
}

actual fun getPlatform(): Platform = DesktopPlatform()

class DesktopPlatform : Platform {
    override val name: String = "Java " + System.getProperty("java.version")
}
""".trimIndent()

fun webMain(packageName: String) = """
package $packageName

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    CanvasBasedWindow(canvasElementId = "ComposeTarget") {
        App()
    }
}

actual fun getPlatform(): Platform = WasmPlatform()

class WasmPlatform : Platform {
    override val name: String = "Web with Kotlin/Wasm"
}
""".trimIndent()

