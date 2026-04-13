package io.github.barsia.composewizard.shared.services

/**
 * Service for fetching Material3 Adaptive library versions from Maven Central.
 * 
 * Fetches from: https://repo1.maven.org/maven2/org/jetbrains/compose/material3/adaptive/adaptive/
 * 
 * Now uses unified fetchVersions() method from base class.
 */
class Material3AdaptiveVersionService : LibraryVersionService()
