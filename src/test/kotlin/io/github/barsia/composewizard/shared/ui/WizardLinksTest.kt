package io.github.barsia.composewizard.shared.ui

import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertTrue

/**
 * Test to verify all links in the plugin point to correct URLs.
 * 
 * Checked links:
 * 1. Report a bug icon in wizard footer
 * 2. Request a feature icon in wizard footer
 * 3. Release Notes link for Compose Version
 * 4. Privacy Policy link in plugin.xml
 * 5. Bug report and feature request links in plugin.xml
 * 6. Open-source metadata in docs and descriptor
 */
class WizardLinksTest {
    
    private val projectRoot = File(System.getProperty("user.dir"))
    
    @Test
    fun `wizard footer bug icon has correct link and capitalization`() {
        val footerFile = File(projectRoot, "src/main/kotlin/io/github/barsia/composewizard/shared/ui/WizardFooter.kt")
        assertTrue(footerFile.exists(), "WizardFooter.kt should exist")
        
        val content = footerFile.readText()
        
        // Check tooltip capitalization
        assertTrue(
            content.contains("""Tooltip(tooltip = { Text("Report a bug") })"""),
            "Bug icon tooltip should be 'Report a bug' (capital R, lowercase bug)"
        )
        
        // Check URL
        assertTrue(
            content.contains("https://github.com/barsia/compose-multiplatform-wizard-plugin/issues/new?template=bug_report.yml"),
            "Bug icon should link to bug_report.yml template"
        )

        assertTrue(
            content.contains("Compose Multiplatform Wizard 0.1.2"),
            "Bug icon environment should use the current plugin version"
        )
        
        // Check content description
        assertTrue(
            content.contains("""contentDescription = "Report a bug""""),
            "Bug icon content description should be 'Report a bug'"
        )
    }
    
    @Test
    fun `wizard footer feature icon has correct link and capitalization`() {
        val footerFile = File(projectRoot, "src/main/kotlin/io/github/barsia/composewizard/shared/ui/WizardFooter.kt")
        assertTrue(footerFile.exists(), "WizardFooter.kt should exist")
        
        val content = footerFile.readText()
        
        // Check tooltip capitalization
        assertTrue(
            content.contains("""Tooltip(tooltip = { Text("Request a feature") })"""),
            "Feature icon tooltip should be 'Request a feature' (capital R, lowercase feature)"
        )
        
        // Check URL
        assertTrue(
            content.contains("https://github.com/barsia/compose-multiplatform-wizard-plugin/issues/new?template=feature_request.yml"),
            "Feature icon should link to feature_request.yml template"
        )
        
        // Check content description
        assertTrue(
            content.contains("""contentDescription = "Request a feature""""),
            "Feature icon content description should be 'Request a feature'"
        )
    }
    
    @Test
    fun `compose version release notes link is correct`() {
        val versionFieldFile = File(projectRoot, "src/main/kotlin/io/github/barsia/composewizard/shared/ui/WizardVersionField.kt")
        assertTrue(versionFieldFile.exists(), "WizardVersionField.kt should exist")
        
        val content = versionFieldFile.readText()
        
        // Check tooltip capitalization
        assertTrue(
            content.contains("""Tooltip(tooltip = { Text("Release Notes") })"""),
            "Release Notes tooltip should be 'Release Notes' (capital R and N)"
        )
        
        // Check URL pattern
        assertTrue(
            content.contains("https://github.com/JetBrains/compose-multiplatform/releases/tag/v"),
            "Release Notes should link to compose-multiplatform releases"
        )
        
        // Check content description
        assertTrue(
            content.contains("""contentDescription = "Release Notes""""),
            "Release Notes content description should be 'Release Notes'"
        )
    }
    
    @Test
    fun `plugin xml has correct privacy policy link`() {
        val pluginXml = File(projectRoot, "src/main/resources/META-INF/plugin.xml")
        assertTrue(pluginXml.exists(), "plugin.xml should exist")

        val content = pluginXml.readText()
        
        // Check link text
        assertTrue(
            content.contains("""<a href="https://github.com/barsia/compose-multiplatform-wizard-plugin/blob/main/PRIVACY.md">Privacy Policy</a>"""),
            "plugin.xml should have Privacy Policy link to the current repository"
        )
        assertTrue(
            content.contains("does not collect usage analytics or personal data"),
            "plugin.xml should describe the plugin as analytics-free"
        )
    }
    
    @Test
    fun `plugin xml has correct bug report link`() {
        val pluginXml = File(projectRoot, "src/main/resources/META-INF/plugin.xml")
        assertTrue(pluginXml.exists(), "plugin.xml should exist")
        
        val content = pluginXml.readText()
        
        // Check link text and URL
        assertTrue(
            content.contains("""<a href="https://github.com/barsia/compose-multiplatform-wizard-plugin/issues/new?template=bug_report.yml">Report a bug</a>"""),
            "plugin.xml should have 'Report a bug' link to bug_report.yml template"
        )
    }
    
    @Test
    fun `plugin xml has correct feature request link`() {
        val pluginXml = File(projectRoot, "src/main/resources/META-INF/plugin.xml")
        assertTrue(pluginXml.exists(), "plugin.xml should exist")
        
        val content = pluginXml.readText()
        
        // Check link text and URL
        assertTrue(
            content.contains("""<a href="https://github.com/barsia/compose-multiplatform-wizard-plugin/issues/new?template=feature_request.yml">Request a feature</a>"""),
            "plugin.xml should have 'Request a feature' link to feature_request.yml template"
        )
    }

    @Test
    fun `plugin xml declares open source metadata`() {
        val pluginXml = File(projectRoot, "src/main/resources/META-INF/plugin.xml")
        assertTrue(pluginXml.exists(), "plugin.xml should exist")

        val content = pluginXml.readText()

        assertTrue(
            content.contains("""<vendor url="https://barsia.github.io">Siarhei Baradulia</vendor>"""),
            "plugin.xml should expose the new vendor branding"
        )
        assertTrue(
            content.contains("""https://github.com/barsia/compose-multiplatform-wizard-plugin/blob/main/LICENSE"""),
            "plugin.xml should link to the Apache 2.0 license in the current repository"
        )
        assertTrue(
            content.contains("""https://github.com/barsia/compose-multiplatform-wizard-plugin/blob/main/EULA.md"""),
            "plugin.xml should link to the Developer EULA in the current repository"
        )
        assertTrue(
            content.contains("""https://github.com/barsia/compose-multiplatform-wizard-plugin"""),
            "plugin.xml should link to the public source repository"
        )
    }

    @Test
    fun `readme declares apache license`() {
        val readme = File(projectRoot, "README.md")
        assertTrue(readme.exists(), "README.md should exist")

        val content = readme.readText()

        assertTrue(
            content.contains("[Apache License 2.0](LICENSE)"),
            "README should point to the Apache 2.0 license"
        )
        assertTrue(
            !content.contains("proprietary and confidential"),
            "README should no longer describe the plugin as proprietary"
        )
    }

    @Test
    fun `plugin xml declares gradle plugin dependency for idea integration`() {
        val pluginXml = File(projectRoot, "src/main/resources/META-INF/plugin.xml")
        assertTrue(pluginXml.exists(), "plugin.xml should exist")

        val content = pluginXml.readText()

        assertTrue(
            content.contains("<depends>com.intellij.gradle</depends>"),
            "plugin.xml should declare com.intellij.gradle because IdeaWizardIntegration uses Gradle APIs"
        )
    }

    @Test
    fun `build script configures plugin verifier ide target via property`() {
        val buildScript = File(projectRoot, "build.gradle.kts")
        assertTrue(buildScript.exists(), "build.gradle.kts should exist")

        val content = buildScript.readText()

        assertTrue(
            content.contains("pluginVerification"),
            "build.gradle.kts should configure intellijPlatform.pluginVerification"
        )
        assertTrue(
            content.contains("pluginVerifierIdeVersion"),
            "build.gradle.kts should define a property-driven IDE version for Plugin Verifier"
        )
        assertTrue(
            content.contains("""group = "io.github.barsia""""),
            "build.gradle.kts should use the new barsia group for published metadata"
        )
    }

    @Test
    fun `apache license file exists`() {
        val license = File(projectRoot, "LICENSE")
        assertTrue(license.exists(), "LICENSE should exist")

        val content = license.readText()

        assertTrue(
            content.contains("Apache License"),
            "LICENSE should contain the Apache License text"
        )
        assertTrue(
            content.contains("Version 2.0, January 2004"),
            "LICENSE should contain the Apache 2.0 version header"
        )
    }

    @Test
    fun `developer eula file exists and points to apache license`() {
        val eula = File(projectRoot, "EULA.md")
        assertTrue(eula.exists(), "EULA.md should exist")

        val content = eula.readText()

        assertTrue(
            content.contains("Apache License, Version 2.0"),
            "EULA should reference the Apache 2.0 license"
        )
        assertTrue(
            content.contains("does not add restrictions beyond the Apache License 2.0"),
            "EULA should remain compatible with the open-source license"
        )
    }

    @Test
    fun `plugin no longer bundles analytics configuration or consent ui`() {
        val buildScript = File(projectRoot, "build.gradle.kts")
        val pluginXml = File(projectRoot, "src/main/resources/META-INF/plugin.xml")
        val settingsFile = File(projectRoot, "src/main/kotlin/io/github/barsia/composewizard/shared/settings/WizardSettingsConfigurable.kt")
        val consentDialog = File(projectRoot, "src/main/kotlin/io/github/barsia/composewizard/shared/analytics/AnalyticsConsentDialog.kt")
        val stateManager = File(projectRoot, "src/main/kotlin/io/github/barsia/composewizard/shared/ui/WizardStateManager.kt")

        assertTrue(
            !buildScript.readText().contains("generateAnalyticsConfig"),
            "build.gradle.kts should not generate analytics.properties anymore"
        )
        assertTrue(
            !buildScript.readText().contains("analytics.properties"),
            "build.gradle.kts should not package analytics.properties anymore"
        )
        assertTrue(
            !pluginXml.readText().contains("shared.analytics.AnalyticsService"),
            "plugin.xml should not register analytics service anymore"
        )
        assertTrue(
            !settingsFile.exists(),
            "settings UI should be removed together with analytics consent controls"
        )
        assertTrue(
            !stateManager.readText().contains("AnalyticsConsentDialog"),
            "wizard state manager should not show analytics consent anymore"
        )
        assertTrue(
            !consentDialog.exists(),
            "AnalyticsConsentDialog should be removed when analytics is disabled"
        )
    }
}
