package org.example.desktop.web

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform