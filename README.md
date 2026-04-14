# Compose Multiplatform Wizard Plugin

[![JetBrains Marketplace](https://img.shields.io/badge/JetBrains%20Marketplace-Compose%20Multiplatform%20Wizard-000000?logo=jetbrains)](https://plugins.jetbrains.com/plugin/29108-compose-multiplatform-wizard)

An IntelliJ Platform plugin that provides a project creation wizard for Compose Multiplatform projects. Works in both IntelliJ IDEA and Android Studio from a single distribution.

## Features

- **Dual IDE Support**: Single ZIP works in both IntelliJ IDEA and Android Studio
- **Multi-platform Templates**: Create projects for Desktop, Android, iOS, and Web
- **Smart Configuration**: Platform-specific integration that adapts to the IDE
- **Shared Core**: Common template processing logic across both IDEs

## Compatibility

- Minimum IntelliJ Platform build: 261+
- Verified targets:
  - IntelliJ IDEA 2026.1.1
  - Android Studio builds based on IntelliJ Platform 261+ (local installation recommended)

### Key Innovation: Shared Compose UI

Both IntelliJ IDEA and Android Studio use the **same Compose UI components** for the wizard:
- Built with Jetpack Compose Desktop and Jewel UI components
- Integrated via Swing's `ComposePanel` in `ComposeWizardStep`
- Consistent look and feel across platforms using Jewel theme bridge
- Single source of truth for wizard UI logic
- Reactive state management with Compose runtime

### Architecture Highlights

**State Management Pattern:**
- `LibrariesState` - Dedicated state manager for library version loading and caching
- Separation of concerns: UI components (presentation) vs. state logic (business logic)
- Reactive updates via Compose state and coroutines

**File Size Discipline:**
- All files strictly under 300 lines (excluding imports)
- Clear responsibility separation
- Easy navigation and maintenance

**Accessibility (A11y):**
- Keyboard navigation support for all interactive elements
- Arrow up/down navigation in all dropdowns (Compose version and library versions) with visual selection highlight
- `SelectableLazyListState` integration for proper visual feedback in dropdowns
- Focus preservation on Refresh icon after keyboard activation (Space/Enter)
- Proper focus management across wizard steps

**State Management:**
- Automatic library version updates when switching between Dev/Stable Compose versions
- Cache invalidation and reload on version type toggle
- Synchronized state across Compose version selector and Libraries section

## Library Version Resolution Logic

### Overview

The wizard automatically resolves compatible library versions for each selected Compose Multiplatform version. The resolution logic varies by library type and Compose version.

### Standard Libraries (Lifecycle, Material3, Navigation, etc.)

**Resolution Strategy (in order of priority):**

1. **Web UI Scraping** - Primary source
   - Fetch from `https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-compatibility-and-versioning.html`
   - Parses official JetBrains compatibility table
   - Most accurate for official Compose releases

2. **Bundled Versions** - Secondary fallback
   - If Web UI fetch succeeds but specific library is missing → use `LIBRARY_BUNDLES` (hardcoded)
   - Pre-validated version bundles embedded in plugin code

3. **Semantic Fallback** - Tertiary fallback
   - Try previous Compose versions (e.g., `1.10.0-beta01` → `1.10.0-alpha03` → `1.9.3`)
   - Check Web UI for each fallback version
   - Use first successful match

4. **Direct Maven Fallback** - Final fallback
   - Query Maven Central directly
   - Parse `maven-metadata.xml` for available versions
   - Use first available version

**Version Filtering for Dropdowns:**

For each library, the dropdown shows:
- **Current version** (first in list)
- **Bundled version** (if different from current)
- **Up to 3 newer published versions** from Maven Central (filtered by semantic version > current)
- Total: max 5 versions

### Hot Reload - Special Logic

Hot Reload has **different behavior** based on Compose version:

#### For Compose < 1.10.0-beta01 (Optional Library)

**Version Resolution:**
- Fixed version: `1.0.0-rc02` (hardcoded)
- No GitHub fetch
- No bundled indicator

**Checkbox:**
- `enabled = true` (can be disabled)

**Dropdown Filtering:**
- Shows only versions `<= 1.0.0-rc02` from Maven
- Filters using semantic version comparison

**Icon:**
- No lock icon (optional library)

#### For Compose >= 1.10.0-beta01 (Bundled Library)

**Version Resolution (in order):**

1. **GitHub Fetch** - Primary source
   ```
   https://raw.githubusercontent.com/JetBrains/compose-multiplatform/
   v${composeVersion}/gradle-plugins/gradle/libs.versions.toml
   ```
   - Parses `plugin-hot-reload = { prefer = "X.Y.Z" }`
   - This is the "aligned" version bundled with Compose
   - Cached separately in `hotReloadGithubVersions`

2. **Maven Fallback** - If GitHub 404
   - Query Maven Central for Hot Reload plugin
   - Use first available version (latest)

3. **Hardcoded Fallback** - If Maven fails
   - Use `1.0.0-rc02` as last resort

**Checkbox:**
- `enabled = false` (bundled, cannot be disabled)
- Tooltip: "Included in the base template and cannot be disabled"

**Dropdown Filtering:**
- Shows ALL versions from Maven (no upper limit)
- Can manually select any version

**Lock Icon Logic:**
```kotlin
showLock = (selectedVersion == githubVersion)
```

- 🔒 **Lock shown** when selected version matches GitHub version
- **Lock hidden** when user selects different version from dropdown
- 🔒 **Lock reappears** when user returns to GitHub version

### Caching Strategy

**Cache Layers:**

1. **In-Memory Cache** (`ComposeVersionCacheState`)
   - Compose versions (stable/dev)
   - Library versions per Compose version
   - Available Maven versions per library
   - Hot Reload GitHub versions
   - TTL: 1 hour for remote data

2. **Per-Version Cache** (LinkedHashMap)
   - Key: Compose version string
   - Value: Resolved library version
   - Max size: 200 entries (LRU eviction)

3. **Fallback Indicators** (`isFromFallback`)
   - Tracks if version came from fallback strategy
   - Shows pin icon (📌) in UI for non-primary sources

**Cache Invalidation:**
- Manual: "Refresh versions" button
- Automatic: After 1 hour
- Scope: All libraries + Compose versions

### Version Comparison

**Semantic Version Parsing:**
```
1.10.0-beta01+dev3245
│  │  │   │      │
│  │  │   │      └─ devNum (build number)
│  │  │   └──────── suffix + suffixNum (beta01)
│  │  └──────────── patch (0)
│  └─────────────── minor (10)
└────────────────── major (1)
```

**Comparison Order:**
1. Major → Minor → Patch (numeric)
2. Suffix (lexicographic): `stable` (zzz) > `rc` > `dev` > `beta` > `alpha`
3. Suffix number (numeric): `beta02` > `beta01`
4. Dev number (numeric): `+dev3245` > `+dev3194`

**Example Ordering (newest → oldest):**
```
1.10.0          (stable, suffix="zzz")
1.10.0-rc02
1.10.0-rc01
1.10.0-beta02+dev3245
1.10.0-beta02+dev3194
1.10.0-beta02
1.10.0-beta01
1.10.0-alpha03
```

### Error Handling

**Network Errors:**
- Web UI unreachable → fallback to bundled versions
- Maven unreachable → fallback to hardcoded versions
- GitHub unreachable (Hot Reload) → fallback to Maven

**Rate Limiting:**
- Detected via HTTP 429 or specific error patterns
- Skip remaining fallback attempts for same library
- Show cached version or empty state

**Invalid Versions:**
- Parse errors → version treated as "0.0.0-{original}"
- Comparison still works, but version ranks lowest
- User sees original string in UI

### UI Indicators

**Icons:**
- 🔒 **Lock** (`BundledLibraryIndicator`) - Hot Reload aligned with Compose (GitHub version)
- 📌 **Pin** (`PinnedVersionIndicator`) - Version from fallback strategy (non-primary source)

**States:**
- **Loading** - Skeleton shimmer animation
- **Loaded** - Normal dropdown with version
- **Error** - Empty or fallback version, no visual error indicator

## Dev/Stable Compose Versions Toggle

### Overview

The wizard displays a **Dev/Stable toggle** (switcher) above the Compose version dropdown, allowing users to switch between stable releases and development/preview versions.

### Visibility Logic

**For Regular Users (non-internal mode):**
- Toggle is **hidden by default**
- Can be **revealed** via triple-click on version footer (`v1.0.0` in bottom-left corner)
- Can be **hidden again** via triple-click (with restrictions - see below)

**For Internal Mode (`idea.is.internal=true`):**
- Toggle is **always visible**
- **Cannot be hidden** (triple-click is blocked)

### Triple-Click Behavior

**Activation Method:**
- Triple-click on version text in footer (`v1.0.0`)
- Clicks must be within 600ms of each other
- Counter resets after successful action or timeout

**For Regular Users:**

| Current State                      | Triple-Click Action | Result          | Feedback                                                                       |
|------------------------------------|---------------------|-----------------|--------------------------------------------------------------------------------|
| Toggle hidden                      | Triple-click        | ✅ Toggle shown  |                                                                                |
| Toggle shown + **Stable** selected | Triple-click        | ✅ Toggle hidden | Auto-switches to Stable mode + saves setting                                   |
| Toggle shown + **Dev** selected    | Triple-click        | ❌ **Blocked**   | Shake animation + Tooltip: "You can hide the toggle after switching to Stable" |

**For Internal Mode:**

| Current State          | Triple-Click Action | Result               | Feedback                                                                          |
|------------------------|---------------------|----------------------|-----------------------------------------------------------------------------------|
| Any state (Dev/Stable) | Triple-click        | ❌ **Always blocked** | Shake animation + Tooltip: "Dev/Stable toggle is always visible in internal mode" |

### Protection Logic

**Why block hiding toggle in Dev mode?**
- Prevents accidental loss of Dev versions when user has Dev mode active
- Forces explicit switch to Stable before hiding the toggle
- Ensures user is aware they're leaving Dev mode

**Why block hiding in internal mode?**
- Internal mode users need persistent access to Dev versions
- Consistency: internal mode = advanced features always available

### Visual Feedback

**Shake Animation:**
- Horizontal oscillation: ±8dp
- 4 repetitions, 50ms each direction
- Triggers when hide action is blocked

**Tooltip:**
- Auto-displays for 3 seconds
- Different messages for regular users vs. internal mode
- Uses Jewel UI `Tooltip` component

### Settings Persistence

**Tracked in `WizardSettings`:**
- `devCheckboxVisibleByUser: Boolean` - Whether user manually toggled visibility
- `enableDevVersions: Boolean` - Whether Dev mode is currently active
- `enableDevVersionsSetByUser: Boolean` - Whether user manually toggled Dev/Stable

**Behavior:**
- Settings persist across IDE restarts
- Hiding toggle automatically disables Dev mode
- Internal mode overrides user preference for visibility

## AGENTS.md / CLAUDE.md Generation

The wizard can generate an `AGENTS.md` blueprint for AI coding assistants (GitHub Copilot, Codex, Claude Code, etc.). The content is tailored to the selected platforms and includes:

- **Commands** — build, run, and test commands (only for targets the agent can execute)
- **Coding Standards** — KMP conventions: `commonMain` for shared code, `expect`/`actual`, version catalog, Compose resources
- **Pitfalls** — common mistakes: platform-specific deps in `commonMain`, manual xcodeproj edits, Wasm compatibility

`CLAUDE.md` is created as a **symlink** pointing to `AGENTS.md` so both files stay in sync.

### Windows Note

Creating symlinks on Windows requires **Developer Mode** enabled or administrator privileges. If symlink creation fails, only `AGENTS.md` is created.

## Development

### Requirements

- JDK 21
- Gradle 9.0+

### Building

```bash
./gradlew buildPlugin
```

Set the path to local Android Studio in `local.properties` (recommended):

```properties
androidStudio.local.path=/Users/<your-user>/Applications/Android Studio.app
```

If not set, Gradle builds against IntelliJ IDEA by default. The resulting ZIP works in both IDEs.

Output ZIP in `build/distributions/`.

### Testing

```bash
# Test in Android Studio
./gradlew runIde

# Test in IntelliJ IDEA
./gradlew runIde -PrunIntellijIdea=true
```

#### Dual Compilation Verification

**During development, you MUST verify compilation for both platforms:**

```bash
# 1. Verify Android Studio compilation (downloaded automatically if needed)
./gradlew compileKotlin

# 2. Verify IntelliJ IDEA compilation
./gradlew compileKotlin -PrunIntellijIdea=true
```

**Why?** The plugin must compile and run in both IDEs. Verifying both targets catches platform-specific regressions before release.

## Publishing to Marketplace

```bash
./gradlew buildPlugin
```

Upload the single ZIP from `build/distributions/` to JetBrains Marketplace. It works in both IDEA and AS.

### How It Works

The plugin uses a shared codebase with a single `plugin.xml`. Android Studio support is loaded conditionally via `optional="true"` dependency on `org.jetbrains.android`:

- **In IntelliJ IDEA**: entry is shown in the New Project wizard (`newProjectWizard.generator`)
- **In Android Studio**: entry is shown in Welcome Screen → More Projects (`WelcomeScreen.QuickStart`)

## License

This project is open source and licensed under the [Apache License 2.0](LICENSE).

**Copyright © 2026 barsia**
