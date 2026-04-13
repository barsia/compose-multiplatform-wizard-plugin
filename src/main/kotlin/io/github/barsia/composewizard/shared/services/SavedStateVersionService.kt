package io.github.barsia.composewizard.shared.services

/**
 * Service for fetching SavedState library versions from Maven Central.
 * 
 * Fetches from: https://repo1.maven.org/maven2/org/jetbrains/androidx/savedstate/savedstate/
 * 
 * Now uses unified fetchVersions() method from base class.
 */
class SavedStateVersionService : LibraryVersionService()
