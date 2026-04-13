package io.github.barsia.composewizard.shared.analytics

/**
 * Helper object for logging analytics events.
 * Provides convenient methods matching the old ComposeWizardUsageCollector API.
 */
object AnalyticsLogger {
    
    private val analytics: AnalyticsService
        get() = AnalyticsService.getInstance()
    
    fun logWizardOpened() {
        analytics.logEvent(
            AnalyticsService.CATEGORY_WIZARD,
            AnalyticsService.ACTION_WIZARD_OPENED
        )
    }
    
    fun logWizardCompleted(
        platformsCount: Int,
        includeTests: Boolean,
        includeGit: Boolean,
        usedDevVersions: Boolean,
        timeSpentMs: Long
    ) {
        analytics.logEvent(
            AnalyticsService.CATEGORY_WIZARD,
            AnalyticsService.ACTION_WIZARD_COMPLETED,
            label = "platforms_$platformsCount",
            value = (timeSpentMs / 1000).toInt()  // Convert to seconds
        )
    }
    
    fun logPlatformToggled(platform: String, selected: Boolean) {
        analytics.logEvent(
            AnalyticsService.CATEGORY_PLATFORM,
            AnalyticsService.ACTION_PLATFORM_TOGGLED,
            label = "$platform:${if (selected) "on" else "off"}"
        )
    }
    
    fun logTestsToggled(enabled: Boolean) {
        analytics.logEvent(
            AnalyticsService.CATEGORY_OPTIONS,
            AnalyticsService.ACTION_TESTS_TOGGLED,
            label = if (enabled) "enabled" else "disabled"
        )
    }
    
    fun logGitToggled(enabled: Boolean) {
        analytics.logEvent(
            AnalyticsService.CATEGORY_OPTIONS,
            AnalyticsService.ACTION_GIT_TOGGLED,
            label = if (enabled) "enabled" else "disabled"
        )
    }
    
    fun logDevVersionsUnlocked() {
        analytics.logEvent(
            AnalyticsService.CATEGORY_OPTIONS,
            AnalyticsService.ACTION_DEV_VERSIONS_UNLOCKED
        )
    }
    
    fun logDevVersionsToggled(enabled: Boolean) {
        analytics.logEvent(
            AnalyticsService.CATEGORY_OPTIONS,
            AnalyticsService.ACTION_DEV_VERSIONS_TOGGLED,
            label = if (enabled) "enabled" else "disabled"
        )
    }
    
    fun logVersionDropdownOpened() {
        analytics.logEvent(
            AnalyticsService.CATEGORY_VERSION,
            AnalyticsService.ACTION_VERSION_DROPDOWN_OPENED
        )
    }
    
    fun logVersionRefreshClicked() {
        analytics.logEvent(
            AnalyticsService.CATEGORY_VERSION,
            AnalyticsService.ACTION_VERSION_REFRESH_CLICKED
        )
    }
    
    fun logComposeVersionSelected(version: String, isDevVersion: Boolean) {
        analytics.logEvent(
            AnalyticsService.CATEGORY_VERSION,
            AnalyticsService.ACTION_VERSION_SELECTED,
            label = if (isDevVersion) "dev:$version" else "stable:$version"
        )
    }
    
    fun logFieldEdited(field: String, hasContent: Boolean) {
        analytics.logEvent(
            AnalyticsService.CATEGORY_FIELD,
            AnalyticsService.ACTION_FIELD_EDITED,
            label = "$field:${if (hasContent) "filled" else "empty"}"
        )
    }
    
    fun logValidationError(field: String, errorType: String) {
        analytics.logEvent(
            AnalyticsService.CATEGORY_VALIDATION,
            AnalyticsService.ACTION_VALIDATION_ERROR,
            label = "$field:$errorType"
        )
    }
    
    // ========== New Analytics Events ==========
    
    /**
     * Log when user toggles between JetBrains Maven and Maven Central repositories
     * @param useJetBrainsMaven true if JetBrains Maven is enabled (dev versions)
     */
    fun logRepositorySourceToggled(useJetBrainsMaven: Boolean) {
        analytics.logEvent(
            AnalyticsService.CATEGORY_OPTIONS,
            AnalyticsService.ACTION_REPOSITORY_TOGGLED,
            label = if (useJetBrainsMaven) "jetbrains_maven" else "maven_central"
        )
    }
    
    /**
     * Log when user selects/deselects a library
     * @param libraryName Name of the library (e.g., "material3", "navigation")
     * @param version Selected version of the library
     * @param selected Whether the library was selected or deselected
     */
    fun logLibraryToggled(libraryName: String, version: String, selected: Boolean) {
        analytics.logEvent(
            AnalyticsService.CATEGORY_LIBRARY,
            AnalyticsService.ACTION_LIBRARY_TOGGLED,
            label = "$libraryName:$version:${if (selected) "on" else "off"}"
        )
    }
    
    /**
     * Log when user changes library version
     * @param libraryName Name of the library
     * @param version New version selected
     */
    fun logLibraryVersionSelected(libraryName: String, version: String) {
        analytics.logEvent(
            AnalyticsService.CATEGORY_LIBRARY,
            AnalyticsService.ACTION_LIBRARY_VERSION_SELECTED,
            label = "$libraryName:$version"
        )
    }
    
    /**
     * Log when user clicks the bug report icon in the footer
     */
    fun logBugReportIconClicked() {
        analytics.logEvent(
            AnalyticsService.CATEGORY_UI,
            AnalyticsService.ACTION_BUG_ICON_CLICKED
        )
    }
    
    /**
     * Log when user clicks the feature request icon in the footer
     */
    fun logFeatureRequestIconClicked() {
        analytics.logEvent(
            AnalyticsService.CATEGORY_UI,
            AnalyticsService.ACTION_FEATURE_ICON_CLICKED
        )
    }
    
    /**
     * Log when user opens bug report from Settings -> Plugins page
     */
    fun logBugReportLinkClicked() {
        analytics.logEvent(
            AnalyticsService.CATEGORY_UI,
            AnalyticsService.ACTION_BUG_LINK_CLICKED
        )
    }
    
    /**
     * Log when user opens feature request from Settings -> Plugins page
     */
    fun logFeatureRequestLinkClicked() {
        analytics.logEvent(
            AnalyticsService.CATEGORY_UI,
            AnalyticsService.ACTION_FEATURE_LINK_CLICKED
        )
    }
    
    /**
     * Log when user discovers dev/release toggle via triple-click on version
     * This is a hidden feature that users need to discover
     */
    fun logTripleClickVersionDiscovery() {
        analytics.logEvent(
            AnalyticsService.CATEGORY_UI,
            AnalyticsService.ACTION_TRIPLE_CLICK_VERSION
        )
    }
}

