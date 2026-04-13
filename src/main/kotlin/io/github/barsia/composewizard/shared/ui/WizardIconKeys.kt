package io.github.barsia.composewizard.shared.ui

import org.jetbrains.jewel.ui.icon.PathIconKey

/**
 * IconKey holders for Compose Multiplatform Wizard icons.
 * Following Jewel Best Practices for icon management.
 */
object WizardIconKeys {
    val Apple: PathIconKey = PathIconKey("icons/ios.svg", WizardIconKeys::class.java)
    val Web: PathIconKey = PathIconKey("icons/web.svg", WizardIconKeys::class.java)
    val RefreshVersions: PathIconKey = PathIconKey("icons/refresh-versions.svg", WizardIconKeys::class.java)
    val Desktop: PathIconKey = PathIconKey("icons/desktop.svg", WizardIconKeys::class.java)
    val Android: PathIconKey = PathIconKey("icons/android.svg", WizardIconKeys::class.java)
    val Pin: PathIconKey = PathIconKey("icons/pin.svg", WizardIconKeys::class.java)
    val Lock: PathIconKey = PathIconKey("icons/lock.svg", WizardIconKeys::class.java)
    val ExternalLink: PathIconKey = PathIconKey("icons/external-link.svg", WizardIconKeys::class.java)
    val ComposeWatermark: PathIconKey = PathIconKey("META-INF/compose.svg", WizardIconKeys::class.java)
    val Bug: PathIconKey = PathIconKey("icons/bug.svg", WizardIconKeys::class.java)
    val Feature: PathIconKey = PathIconKey("icons/feature.svg", WizardIconKeys::class.java)
    val Folder: PathIconKey = PathIconKey("icons/folder.svg", WizardIconKeys::class.java)
    val FolderOutline: PathIconKey = PathIconKey("icons/folder-outline.svg", WizardIconKeys::class.java)
}
