package io.github.heisiar.composewizard.shared.services

interface LibraryVersionService {
    fun filterVersionsForDropdown(
        allVersions: List<String>,
        originalVersion: String,
        bundledVersion: String?,
        maxItems: Int
    ): List<String>
}
