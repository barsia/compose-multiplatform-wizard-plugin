package org.example.project.android.desktop.ios.tests

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "AndroidDesktopIosTests",
    ) {
        App()
    }
}