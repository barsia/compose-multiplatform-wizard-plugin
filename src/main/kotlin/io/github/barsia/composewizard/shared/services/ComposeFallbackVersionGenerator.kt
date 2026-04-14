package io.github.barsia.composewizard.shared.services

object ComposeFallbackVersionGenerator {

    const val MAX_FALLBACK_VERSIONS = 5

    /**
     * Generate fallback versions from available Compose versions.
     * Uses real versions from Maven instead of generating hypothetical ones.
     */
    fun generateFallbackVersions(
        baseVersion: String,
        availableVersions: List<String>
    ): List<String> {
        return availableVersions
            .filter { version ->
                !version.contains("+dev") &&
                VersionComparison.isComposeVersionLessThan(version, baseVersion)
            }
            .take(MAX_FALLBACK_VERSIONS)
    }
}
