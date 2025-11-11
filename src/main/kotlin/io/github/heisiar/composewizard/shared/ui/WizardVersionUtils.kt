package io.github.heisiar.composewizard.shared.ui

internal fun isComposeVersionLessThan(version: String, threshold: String): Boolean {
    val versionBase = version.split("+").first()
    val thresholdBase = threshold.split("+").first()
    
    val versionNumeric = versionBase.split("-").first()
    val versionQualifier = versionBase.substringAfter("-", "")
    
    val thresholdNumeric = thresholdBase.split("-").first()
    val thresholdQualifier = thresholdBase.substringAfter("-", "")
    
    val versionParts = versionNumeric.split(".").map { it.toIntOrNull() ?: 0 }
    val thresholdParts = thresholdNumeric.split(".").map { it.toIntOrNull() ?: 0 }
    
    for (i in 0 until maxOf(versionParts.size, thresholdParts.size)) {
        val v = versionParts.getOrNull(i) ?: 0
        val t = thresholdParts.getOrNull(i) ?: 0
        if (v < t) return true
        if (v > t) return false
    }
    
    if (thresholdQualifier.isNotEmpty() && versionQualifier.isEmpty()) {
        return false
    }
    
    if (versionQualifier.isNotEmpty() && thresholdQualifier.isEmpty()) {
        return true
    }
    
    return versionQualifier < thresholdQualifier
}

