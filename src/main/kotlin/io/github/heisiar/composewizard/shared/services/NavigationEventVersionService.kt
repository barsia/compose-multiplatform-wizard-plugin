package io.github.heisiar.composewizard.shared.services

/**
 * Service for fetching NavigationEvent library versions from Maven Central.
 * 
 * Fetches from: https://repo1.maven.org/maven2/org/jetbrains/androidx/navigationevent/navigationevent-compose/
 * 
 * Now uses unified fetchVersions() method from base class.
 */
class NavigationEventVersionService : LibraryVersionService()
