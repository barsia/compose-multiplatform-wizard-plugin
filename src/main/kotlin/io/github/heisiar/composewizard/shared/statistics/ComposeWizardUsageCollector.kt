package io.github.heisiar.composewizard.shared.statistics

import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.service.fus.collectors.CounterUsagesCollector
import com.intellij.openapi.application.ApplicationInfo

object ComposeWizardUsageCollector : CounterUsagesCollector() {
    
    private val GROUP = EventLogGroup("compose.wizard", 2)
    
    // Common field for all events - IDE product code (AI, IU, IC, PS, WS, PY, CL, GO, RD, RM, etc.)
    private val IDE_TYPE = EventFields.String("ide_type", listOf("AI", "IU", "IC", "PS", "WS", "PY", "CL", "GO", "RD", "RM"))

    // Wizard lifecycle
    private val WIZARD_OPENED = GROUP.registerEvent(
        "wizard.opened",
        IDE_TYPE,
        "Compose Multiplatform wizard dialog opened"
    )
    
    // Fields for vararg events
    private val PLATFORMS_COUNT = EventFields.Int("platforms_count")
    private val INCLUDE_TESTS = EventFields.Boolean("include_tests")
    private val INCLUDE_GIT = EventFields.Boolean("include_git")
    private val USED_DEV_VERSIONS = EventFields.Boolean("used_dev_versions")
    private val TIME_SPENT_MS = EventFields.Long("time_spent_ms")
    
    private val WIZARD_COMPLETED = GROUP.registerVarargEvent(
        "wizard.completed",
        "Compose Multiplatform wizard completed with project configuration",
        IDE_TYPE,
        PLATFORMS_COUNT,
        INCLUDE_TESTS,
        INCLUDE_GIT,
        USED_DEV_VERSIONS,
        TIME_SPENT_MS
    )
    
    // Platform selection
    private val PLATFORM_TOGGLED = GROUP.registerEvent(
        "platform.toggled",
        IDE_TYPE,
        EventFields.String("platform", listOf("Android", "iOS", "Desktop", "Web")),
        EventFields.Boolean("selected"),
        "Platform selection toggled in wizard"
    )
    
    // Options
    private val TESTS_TOGGLED = GROUP.registerEvent(
        "tests.toggled",
        IDE_TYPE,
        EventFields.Boolean("enabled"),
        "Include tests option toggled"
    )
    
    private val GIT_TOGGLED = GROUP.registerEvent(
        "git.toggled",
        IDE_TYPE,
        EventFields.Boolean("enabled"),
        "Include git option toggled"
    )
    
    // Dev versions feature
    private val DEV_VERSIONS_UNLOCKED = GROUP.registerEvent(
        "dev.versions.unlocked",
        IDE_TYPE,
        "Developer versions feature unlocked"
    )
    
    private val DEV_VERSIONS_VISIBILITY_TOGGLED = GROUP.registerEvent(
        "dev.versions.visibility.toggled",
        IDE_TYPE,
        EventFields.Boolean("visible"),
        EventFields.Int("toggle_count"),
        "Developer versions visibility toggled"
    )
    
    private val DEV_VERSIONS_TOGGLED = GROUP.registerEvent(
        "dev.versions.toggled",
        IDE_TYPE,
        EventFields.Boolean("enabled"),
        "Use developer versions option toggled"
    )
    
    // Compose version selection
    private val VERSION_DROPDOWN_OPENED = GROUP.registerEvent(
        "version.dropdown.opened",
        IDE_TYPE,
        "Compose version dropdown opened"
    )
    
    private val VERSION_REFRESH_CLICKED = GROUP.registerEvent(
        "version.refresh.clicked",
        IDE_TYPE,
        "Compose version refresh button clicked"
    )
    
    private val COMPOSE_VERSION = EventFields.StringValidatedByInlineRegexp("version", ".*")
    private val IS_DEV_VERSION = EventFields.Boolean("is_dev_version")
    
    private val COMPOSE_VERSION_SELECTED = GROUP.registerVarargEvent(
        "compose.version.selected",
        "Compose version selected from dropdown",
        IDE_TYPE,
        COMPOSE_VERSION,
        IS_DEV_VERSION
    )
    
    // Field interactions
    private val FIELD_EDITED = GROUP.registerEvent(
        "field.edited",
        IDE_TYPE,
        EventFields.String("field", listOf("project_name", "project_location", "project_id")),
        EventFields.Boolean("has_content"),
        "Text field edited in wizard"
    )
    
    // Validation errors
    private val VALIDATION_ERROR = GROUP.registerEvent(
        "validation.error",
        IDE_TYPE,
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
        )),
        "Validation error occurred"
    )

    // Public logging methods
    
    private fun getIdeType(): String {
        // Return product code: AI (Android Studio), IU/IC (IntelliJ IDEA), PS (PhpStorm), etc.
        return ApplicationInfo.getInstance().build.productCode ?: "UNKNOWN"
    }

    fun logWizardOpened() {
        WIZARD_OPENED.log(getIdeType())
    }

    fun logWizardCompleted(
        platformsCount: Int,
        includeTests: Boolean,
        includeGit: Boolean,
        usedDevVersions: Boolean,
        timeSpentMs: Long
    ) {
        WIZARD_COMPLETED.log(
            IDE_TYPE.with(getIdeType()),
            PLATFORMS_COUNT.with(platformsCount),
            INCLUDE_TESTS.with(includeTests),
            INCLUDE_GIT.with(includeGit),
            USED_DEV_VERSIONS.with(usedDevVersions),
            TIME_SPENT_MS.with(timeSpentMs)
        )
    }

    fun logPlatformToggled(platform: String, selected: Boolean) {
        PLATFORM_TOGGLED.log(getIdeType(), platform, selected)
    }

    fun logTestsToggled(enabled: Boolean) {
        TESTS_TOGGLED.log(getIdeType(), enabled)
    }

    fun logGitToggled(enabled: Boolean) {
        GIT_TOGGLED.log(getIdeType(), enabled)
    }

    fun logDevVersionsUnlocked() {
        DEV_VERSIONS_UNLOCKED.log(getIdeType())
    }
    
    fun logDevVersionsVisibilityToggled(visible: Boolean, toggleCount: Int) {
        DEV_VERSIONS_VISIBILITY_TOGGLED.log(getIdeType(), visible, toggleCount)
    }

    fun logDevVersionsToggled(enabled: Boolean) {
        DEV_VERSIONS_TOGGLED.log(getIdeType(), enabled)
    }
    
    fun logVersionDropdownOpened() {
        VERSION_DROPDOWN_OPENED.log(getIdeType())
    }
    
    fun logVersionRefreshClicked() {
        VERSION_REFRESH_CLICKED.log(getIdeType())
    }

    fun logComposeVersionSelected(version: String, isDevVersion: Boolean) {
        COMPOSE_VERSION_SELECTED.log(
            IDE_TYPE.with(getIdeType()),
            COMPOSE_VERSION.with(version),
            IS_DEV_VERSION.with(isDevVersion)
        )
    }

    fun logFieldEdited(field: String, hasContent: Boolean) {
        FIELD_EDITED.log(getIdeType(), field, hasContent)
    }

    fun logValidationError(field: String, errorType: String) {
        VALIDATION_ERROR.log(getIdeType(), field, errorType)
    }

    override fun getGroup(): EventLogGroup = GROUP
}

