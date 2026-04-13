package io.github.barsia.composewizard.shared.services

/**
 * Service for fetching Navigation3 library versions from Maven Central.
 * 
 * Fetches from: https://repo1.maven.org/maven2/org/jetbrains/androidx/navigation3/navigation3-compose/
 * 
 * Now uses unified fetchVersions() method from base class.
 */
class Navigation3VersionService : LibraryVersionService()
