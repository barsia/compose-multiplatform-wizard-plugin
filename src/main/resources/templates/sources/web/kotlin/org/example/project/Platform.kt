package $PACKAGE_ID$

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform