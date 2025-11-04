package org.example.android.web.ios.git

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform