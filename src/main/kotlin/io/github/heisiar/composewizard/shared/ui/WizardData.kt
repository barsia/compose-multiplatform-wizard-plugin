package io.github.heisiar.composewizard.shared.ui

data class WizardData(
    val projectName: String = "MyComposeApp",
    val projectLocation: String = System.getProperty("user.home"),
    val packageName: String = "com.example.myapp",
    val composeVersion: String = "1.7.1",
    val includeAndroid: Boolean = true,
    val includeIos: Boolean = true,
    val includeDesktop: Boolean = true,
    val includeWeb: Boolean = false,
    val includeTests: Boolean = true,
    val enableDevVersions: Boolean = false
) {
    fun isValid(): Boolean {
        return projectName.isNotBlank() 
            && packageName.isNotBlank()
            && (includeAndroid || includeIos || includeDesktop || includeWeb)
    }
}

