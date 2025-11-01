package io.github.heisiar.composewizard.shared

import com.intellij.openapi.application.ApplicationInfo

object PlatformDetector {
    val isAndroidStudio: Boolean by lazy {
        ApplicationInfo.getInstance().build.productCode == "AI"
    }
    
    val isIntellijIdea: Boolean by lazy {
        val code = ApplicationInfo.getInstance().build.productCode
        code in listOf("IC", "IU")
    }
    
    fun hasAndroidSupport(): Boolean {
        return try {
            Class.forName("com.android.tools.idea.npw.module.ModuleDescriptionProvider")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }
    
    val platformName: String
        get() = when {
            isAndroidStudio -> "Android Studio"
            isIntellijIdea -> "IntelliJ IDEA"
            else -> "Unknown Platform"
        }
}

