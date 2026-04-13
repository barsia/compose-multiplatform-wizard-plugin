package io.github.barsia.composewizard.shared.services

/**
 * Service for fetching Lifecycle library versions from Maven Central.
 * 
 * Fetches from: https://repo1.maven.org/maven2/org/jetbrains/androidx/lifecycle/lifecycle-runtime/
 * 
 * Now uses unified fetchVersions() method from base class.
 */
class LifecycleVersionService : LibraryVersionService()
