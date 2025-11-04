package io.github.heisiar.composewizard.generator

class SettingsGradleGenerator {
    
    fun generate(config: ProjectConfig): String {
        return buildString {
            appendLine("rootProject.name = \"${config.projectName}\"")
            appendLine("enableFeaturePreview(\"TYPESAFE_PROJECT_ACCESSORS\")")
            appendLine()
            
            appendLine("pluginManagement {")
            appendLine("    repositories {")
            appendLine("        google {")
            appendLine("            mavenContent {")
            appendLine("                includeGroupAndSubgroups(\"androidx\")")
            appendLine("                includeGroupAndSubgroups(\"com.android\")")
            appendLine("                includeGroupAndSubgroups(\"com.google\")")
            appendLine("            }")
            appendLine("        }")
            appendLine("        mavenCentral()")
            appendLine("        gradlePluginPortal()")
            appendLine("    }")
            appendLine("}")
            appendLine()
            
            appendLine("dependencyResolutionManagement {")
            appendLine("    repositories {")
            appendLine("        google {")
            appendLine("            mavenContent {")
            appendLine("                includeGroupAndSubgroups(\"androidx\")")
            appendLine("                includeGroupAndSubgroups(\"com.android\")")
            appendLine("                includeGroupAndSubgroups(\"com.google\")")
            appendLine("            }")
            appendLine("        }")
            appendLine("        mavenCentral()")
            appendLine("    }")
            appendLine("}")
            
            if (config.targetDesktop) {
                appendLine()
                appendLine("plugins {")
                appendLine("    id(\"org.gradle.toolchains.foojay-resolver-convention\") version \"1.0.0\"")
                appendLine("}")
            }
            
            appendLine()
            append("include(\":composeApp\")")
        }
    }
}

