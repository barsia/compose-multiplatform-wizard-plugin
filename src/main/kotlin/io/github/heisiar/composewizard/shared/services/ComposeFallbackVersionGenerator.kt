package io.github.heisiar.composewizard.shared.services

import io.github.heisiar.composewizard.shared.ComposeVersions

object ComposeFallbackVersionGenerator {
    
    private const val MAX_FALLBACK_VERSIONS = 30
    
    fun generateFallbackVersions(baseVersion: String): List<String> {
        val versions = mutableListOf<String>()
        
        versions.addAll(ComposeVersions.LIBRARY_BUNDLES.keys.filter { !it.contains("+dev") })
        
        val parts = baseVersion.split(".")
        
        if (parts.size < 3) return versions.distinct().take(MAX_FALLBACK_VERSIONS)
        
        val major = parts[0].toIntOrNull() ?: return versions.distinct().take(MAX_FALLBACK_VERSIONS)
        val minor = parts[1].toIntOrNull() ?: return versions.distinct().take(MAX_FALLBACK_VERSIONS)
        val patchWithQualifier = parts[2]
        
        val patchParts = patchWithQualifier.split("-")
        val patch = patchParts[0].toIntOrNull() ?: return versions.distinct().take(MAX_FALLBACK_VERSIONS)
        val qualifier = if (patchParts.size > 1) patchParts[1] else ""
        
        if (qualifier.isNotEmpty()) {
            val (qualifierType, qualifierNum) = parseQualifier(qualifier)
            
            if (qualifierType.isNotEmpty() && qualifierNum > 0) {
                for (i in (qualifierNum - 1) downTo 1) {
                    versions.add("$major.$minor.$patch-$qualifierType${i.toString().padStart(2, '0')}")
                }
                
                if (qualifierType == "rc") {
                    for (i in 3 downTo 1) {
                        versions.add("$major.$minor.$patch-beta${i.toString().padStart(2, '0')}")
                    }
                    for (i in 3 downTo 1) {
                        versions.add("$major.$minor.$patch-alpha${i.toString().padStart(2, '0')}")
                    }
                }
                
                if (qualifierType == "beta") {
                    for (i in 3 downTo 1) {
                        versions.add("$major.$minor.$patch-alpha${i.toString().padStart(2, '0')}")
                    }
                }
            }
        }
        
        if (patch > 0) {
            for (p in (patch - 1) downTo maxOf(0, patch - 2)) {
                versions.add("$major.$minor.$p")
                for (q in listOf("rc", "beta", "alpha")) {
                    for (num in 3 downTo 1) {
                        versions.add("$major.$minor.$p-$q${num.toString().padStart(2, '0')}")
                    }
                }
            }
        }
        
        for (m in (minor - 1) downTo 0) {
            for (p in 3 downTo 0) {
                versions.add("$major.$m.$p")
            }
        }
        
        return versions.distinct().take(MAX_FALLBACK_VERSIONS)
    }
    
    private fun parseQualifier(qualifier: String): Pair<String, Int> {
        val match = Regex("""(beta|alpha|rc)(\d+)""").find(qualifier)
        return if (match != null) {
            val type = match.groupValues[1]
            val num = match.groupValues[2].toIntOrNull() ?: 0
            type to num
        } else {
            "" to 0
        }
    }
}
