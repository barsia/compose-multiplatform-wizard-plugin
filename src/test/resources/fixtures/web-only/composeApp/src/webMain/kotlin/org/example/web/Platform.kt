package org.example.web

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform