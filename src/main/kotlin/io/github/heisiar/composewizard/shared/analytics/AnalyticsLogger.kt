package io.github.heisiar.composewizard.shared.analytics

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
}

