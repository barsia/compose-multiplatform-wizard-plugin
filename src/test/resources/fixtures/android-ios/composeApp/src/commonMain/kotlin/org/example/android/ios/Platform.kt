package org.example.android.ios

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform