package io.github.heisiar.composewizard.shared.statistics

import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.service.fus.collectors.CounterUsagesCollector

object ComposeWizardUsageCollector : CounterUsagesCollector() {
    
    private val GROUP = EventLogGroup("compose.wizard", 1)

    // Wizard lifecycle
    private val WIZARD_OPENED = GROUP.registerEvent("wizard.opened")
    
    // Fields for vararg events
    private val PLATFORMS_COUNT = EventFields.Int("platforms_count")
    private val INCLUDE_TESTS = EventFields.Boolean("include_tests")
    private val INCLUDE_GIT = EventFields.Boolean("include_git")
    private val USED_DEV_VERSIONS = EventFields.Boolean("used_dev_versions")
    private val TIME_SPENT_MS = EventFields.Long("time_spent_ms")
    
    private val WIZARD_COMPLETED = GROUP.registerVarargEvent(
        "wizard.completed",
        PLATFORMS_COUNT,
        INCLUDE_TESTS,
        INCLUDE_GIT,
        USED_DEV_VERSIONS,
        TIME_SPENT_MS
    )
    
    // Platform selection
    private val PLATFORM_TOGGLED = GROUP.registerEvent(
        "platform.toggled",
        EventFields.String("platform", listOf("Android", "iOS", "Desktop", "Web")),
        EventFields.Boolean("selected")
    )
    
    // Options
    private val TESTS_TOGGLED = GROUP.registerEvent(
        "tests.toggled",
        EventFields.Boolean("enabled")
    )
    
    private val GIT_TOGGLED = GROUP.registerEvent(
        "git.toggled",
        EventFields.Boolean("enabled")
    )
    
    // Dev versions feature
    private val DEV_VERSIONS_UNLOCKED = GROUP.registerEvent("dev.versions.unlocked")
    
    private val DEV_VERSIONS_TOGGLED = GROUP.registerEvent(
        "dev.versions.toggled",
        EventFields.Boolean("enabled")
    )
    
    // Compose version selection
    private val COMPOSE_VERSION = EventFields.StringValidatedByInlineRegexp("version", ".*")
    private val IS_DEV_VERSION = EventFields.Boolean("is_dev_version")
    
    private val COMPOSE_VERSION_SELECTED = GROUP.registerVarargEvent(
        "compose.version.selected",
        COMPOSE_VERSION,
        IS_DEV_VERSION
    )
    
    // Field interactions
    private val FIELD_EDITED = GROUP.registerEvent(
        "field.edited",
        EventFields.String("field", listOf("project_name", "project_location", "project_id")),
        EventFields.Boolean("has_content")
    )
    
    // Validation errors
    private val VALIDATION_ERROR = GROUP.registerEvent(
        "validation.error",
        EventFields.String("field", listOf("project_name", "project_location", "project_id", "platforms")),
        EventFields.String("error_type", listOf(
            "empty",
            "invalid_chars",
            "reserved_name",
            "invalid_path",
            "no_write_access",
            "already_open",
            "directory_not_empty",
            "invalid_package_format",
            "no_platform_selected"
        ))
    )

    // Public logging methods

    fun logWizardOpened() {
        WIZARD_OPENED.log()
    }

    fun logWizardCompleted(
        platformsCount: Int,
        includeTests: Boolean,
        includeGit: Boolean,
        usedDevVersions: Boolean,
        timeSpentMs: Long
    ) {
        WIZARD_COMPLETED.log(
            PLATFORMS_COUNT.with(platformsCount),
            INCLUDE_TESTS.with(includeTests),
            INCLUDE_GIT.with(includeGit),
            USED_DEV_VERSIONS.with(usedDevVersions),
            TIME_SPENT_MS.with(timeSpentMs)
        )
    }

    fun logPlatformToggled(platform: String, selected: Boolean) {
        PLATFORM_TOGGLED.log(platform, selected)
    }

    fun logTestsToggled(enabled: Boolean) {
        TESTS_TOGGLED.log(enabled)
    }

    fun logGitToggled(enabled: Boolean) {
        GIT_TOGGLED.log(enabled)
    }

    fun logDevVersionsUnlocked() {
        DEV_VERSIONS_UNLOCKED.log()
    }

    fun logDevVersionsToggled(enabled: Boolean) {
        DEV_VERSIONS_TOGGLED.log(enabled)
    }

    fun logComposeVersionSelected(version: String, isDevVersion: Boolean) {
        COMPOSE_VERSION_SELECTED.log(
            COMPOSE_VERSION.with(version),
            IS_DEV_VERSION.with(isDevVersion)
        )
    }

    fun logFieldEdited(field: String, hasContent: Boolean) {
        FIELD_EDITED.log(field, hasContent)
    }

    fun logValidationError(field: String, errorType: String) {
        VALIDATION_ERROR.log(field, errorType)
    }

    override fun getGroup(): EventLogGroup = GROUP
}

