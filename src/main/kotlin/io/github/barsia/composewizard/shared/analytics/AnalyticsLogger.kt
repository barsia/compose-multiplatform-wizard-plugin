package io.github.barsia.composewizard.shared.analytics

/**
 * Legacy no-op logger kept to avoid touching unrelated UI flows.
 * Analytics is intentionally disabled in public builds.
 */
object AnalyticsLogger {
    fun logWizardOpened() = Unit
    fun logWizardCompleted(
        platformsCount: Int,
        includeTests: Boolean,
        includeGit: Boolean,
        usedDevVersions: Boolean,
        timeSpentMs: Long
    ) = Unit
    fun logPlatformToggled(platform: String, selected: Boolean) = Unit
    fun logTestsToggled(enabled: Boolean) = Unit
    fun logGitToggled(enabled: Boolean) = Unit
    fun logDevVersionsUnlocked() = Unit
    fun logDevVersionsToggled(enabled: Boolean) = Unit
    fun logVersionDropdownOpened() = Unit
    fun logVersionRefreshClicked() = Unit
    fun logComposeVersionSelected(version: String, isDevVersion: Boolean) = Unit
    fun logFieldEdited(field: String, hasContent: Boolean) = Unit
    fun logValidationError(field: String, errorType: String) = Unit
    fun logRepositorySourceToggled(useJetBrainsMaven: Boolean) = Unit
    fun logLibraryToggled(libraryName: String, version: String, selected: Boolean) = Unit
    fun logLibraryVersionSelected(libraryName: String, version: String) = Unit
    fun logBugReportIconClicked() = Unit
    fun logFeatureRequestIconClicked() = Unit
    fun logBugReportLinkClicked() = Unit
    fun logFeatureRequestLinkClicked() = Unit
    fun logTripleClickVersionDiscovery() = Unit
}
