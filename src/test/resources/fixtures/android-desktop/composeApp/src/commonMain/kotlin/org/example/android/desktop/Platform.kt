package org.example.android.desktop

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform