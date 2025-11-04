package org.example.all.platforms

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform