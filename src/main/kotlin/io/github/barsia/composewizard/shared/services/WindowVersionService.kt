package io.github.barsia.composewizard.shared.services

/**
 * Service for fetching Window library versions from Maven Central.
 * 
 * Fetches from: https://repo1.maven.org/maven2/org/jetbrains/androidx/window/window-core/
 * 
 * Now uses unified fetchVersions() method from base class.
 */
class WindowVersionService : LibraryVersionService()
