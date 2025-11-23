package io.github.heisiar.composewizard.shared.ui

import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertTrue

/**
 * Test to verify all links in the plugin point to correct URLs.
 * 
 * Checked links:
 * 1. Report a bug icon in wizard footer
 * 2. Request a feature icon in wizard footer
 * 3. Privacy Policy link in Settings
 * 4. Release Notes link for Compose Version
 * 5. Privacy Policy link in plugin.xml
 * 6. Report a bug link in plugin.xml
 * 7. Request a feature link in plugin.xml
 */
class WizardLinksTest {
    
    private val projectRoot = File(System.getProperty("user.dir"))
    
    @Test
    fun `wizard footer bug icon has correct link and capitalization`() {
        val footerFile = File(projectRoot, "src/main/kotlin/io/github/heisiar/composewizard/shared/ui/WizardFooter.kt")
        assertTrue(footerFile.exists(), "WizardFooter.kt should exist")
        
        val content = footerFile.readText()
        
        // Check tooltip capitalization
        assertTrue(
            content.contains("""Tooltip(tooltip = { Text("Report a bug") })"""),
            "Bug icon tooltip should be 'Report a bug' (capital R, lowercase bug)"
        )
        
        // Check URL
        assertTrue(
            content.contains("https://github.com/heisiar/compose-multiplatform-wizard-project/issues/new?template=bug_report.yml"),
            "Bug icon should link to bug_report.yml template"
        )
        
        // Check content description
        assertTrue(
            content.contains("""contentDescription = "Report a bug""""),
            "Bug icon content description should be 'Report a bug'"
        )
    }
    
    @Test
    fun `wizard footer feature icon has correct link and capitalization`() {
        val footerFile = File(projectRoot, "src/main/kotlin/io/github/heisiar/composewizard/shared/ui/WizardFooter.kt")
        assertTrue(footerFile.exists(), "WizardFooter.kt should exist")
        
        val content = footerFile.readText()
        
        // Check tooltip capitalization
        assertTrue(
            content.contains("""Tooltip(tooltip = { Text("Request a feature") })"""),
            "Feature icon tooltip should be 'Request a feature' (capital R, lowercase feature)"
        )
        
        // Check URL
        assertTrue(
            content.contains("https://github.com/heisiar/compose-multiplatform-wizard-project/issues/new?template=feature_request.yml"),
            "Feature icon should link to feature_request.yml template"
        )
        
        // Check content description
        assertTrue(
            content.contains("""contentDescription = "Request a feature""""),
            "Feature icon content description should be 'Request a feature'"
        )
    }
    
    @Test
    fun `settings privacy policy link is correct`() {
        val settingsFile = File(projectRoot, "src/main/kotlin/io/github/heisiar/composewizard/shared/settings/WizardSettingsConfigurable.kt")
        assertTrue(settingsFile.exists(), "WizardSettingsConfigurable.kt should exist")
        
        val content = settingsFile.readText()
        
        // Check link text
        assertTrue(
            content.contains("""HyperlinkLabel("Privacy Policy")"""),
            "Settings should have 'Privacy Policy' link (capital P)"
        )
        
        // Check URL
        assertTrue(
            content.contains("https://github.com/heisiar/compose-multiplatform-wizard-project/blob/main/PRIVACY.md"),
            "Settings Privacy Policy should link to wizard-project PRIVACY.md"
        )
    }
    
    @Test
    fun `consent dialog privacy policy link is correct`() {
        val dialogFile = File(projectRoot, "src/main/kotlin/io/github/heisiar/composewizard/shared/analytics/AnalyticsConsentDialog.kt")
        assertTrue(dialogFile.exists(), "AnalyticsConsentDialog.kt should exist")
        
        val content = dialogFile.readText()
        
        // Check link text
        assertTrue(
            content.contains(""""Privacy Policy""""),
            "Consent dialog should have 'Privacy Policy' link"
        )
        
        // Check URL
        assertTrue(
            content.contains("https://github.com/heisiar/compose-multiplatform-wizard-project/blob/main/PRIVACY.md"),
            "Consent dialog Privacy Policy should link to wizard-project PRIVACY.md"
        )
    }
    
    @Test
    fun `compose version release notes link is correct`() {
        val versionFieldFile = File(projectRoot, "src/main/kotlin/io/github/heisiar/composewizard/shared/ui/WizardVersionField.kt")
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
            content.contains("""<a href="https://github.com/heisiar/compose-multiplatform-wizard-project/blob/main/PRIVACY.md">Privacy Policy</a>"""),
            "plugin.xml should have Privacy Policy link to wizard-project"
        )
    }
    
    @Test
    fun `plugin xml has correct bug report link`() {
        val pluginXml = File(projectRoot, "src/main/resources/META-INF/plugin.xml")
        assertTrue(pluginXml.exists(), "plugin.xml should exist")
        
        val content = pluginXml.readText()
        
        // Check link text and URL
        assertTrue(
            content.contains("""<a href="https://github.com/heisiar/compose-multiplatform-wizard-project/issues/new?template=bug_report.yml">Report a bug</a>"""),
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
            content.contains("""<a href="https://github.com/heisiar/compose-multiplatform-wizard-project/issues/new?template=feature_request.yml">Request a feature</a>"""),
            "plugin.xml should have 'Request a feature' link to feature_request.yml template"
        )
    }
}

