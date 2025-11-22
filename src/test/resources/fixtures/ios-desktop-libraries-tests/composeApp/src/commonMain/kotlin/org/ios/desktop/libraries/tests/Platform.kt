package org.ios.desktop.libraries.tests

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform