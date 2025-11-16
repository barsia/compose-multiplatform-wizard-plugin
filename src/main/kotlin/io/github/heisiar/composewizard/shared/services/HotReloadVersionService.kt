package io.github.heisiar.composewizard.shared.services

/**
 * Service for fetching Hot Reload library versions from Maven Central.
 * 
 * Fetches from: https://repo1.maven.org/maven2/org/jetbrains/compose/hot-reload/
 * 
 * Now uses unified fetchVersions() method from base class.
 */
class HotReloadVersionService : LibraryVersionService()
