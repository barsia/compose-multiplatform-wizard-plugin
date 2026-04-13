package io.github.barsia.composewizard.shared.services

/**
 * Service for fetching Material3 library versions from Maven Central.
 * 
 * Fetches from: https://repo1.maven.org/maven2/org/jetbrains/compose/material3/material3/
 * 
 * Now uses unified fetchVersions() method from base class.
 */
class Material3VersionService : LibraryVersionService()
