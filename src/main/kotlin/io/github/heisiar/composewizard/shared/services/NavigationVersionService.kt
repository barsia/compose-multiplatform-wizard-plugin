package io.github.heisiar.composewizard.shared.services

/**
 * Service for fetching Navigation library versions from Maven Central.
 * 
 * Fetches from: https://repo1.maven.org/maven2/org/jetbrains/androidx/navigation/navigation-compose/
 * 
 * Now uses unified fetchVersions() method from base class.
 */
class NavigationVersionService : LibraryVersionService()
