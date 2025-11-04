package org.example.project.android.desktop.ios.tests

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform