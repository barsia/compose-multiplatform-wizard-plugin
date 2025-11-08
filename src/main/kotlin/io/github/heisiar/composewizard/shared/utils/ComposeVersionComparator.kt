package io.github.heisiar.composewizard.shared.utils

/**
 * Semantic version comparator for Compose Multiplatform versions.
 * 
 * Handles various version formats:
 * - Stable: "1.9.1", "1.10.0"
 * - Pre-release: "1.9.0-alpha03", "1.10.0-beta01", "1.9.0-rc02"
 * - Dev builds: "1.9.0+dev2970", "1.10.0-beta01+dev3194"
 */
object ComposeVersionComparator {
    
    fun parse(version: String, debug: Boolean = false): VersionComparable {
        return try {
            // Split by '.', '-', or '+' to handle both stable and dev versions
            val parts = version.split(".", "-", "+")
            val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
            
            // Extract primary suffix (alpha, beta, rc, or dev)
            val suffixPart = parts.getOrNull(3)?.takeIf { it.isNotEmpty() } ?: "zzz"
            val suffixName = suffixPart.takeWhile { !it.isDigit() }.ifEmpty { suffixPart }
            val suffixNum = suffixPart.filter { it.isDigit() }.toIntOrNull() ?: 0
            
            // Extract secondary dev suffix if present (for versions like "1.10.0-beta01+dev3194")
            val devPart = parts.getOrNull(4)?.takeIf { it.isNotEmpty() } ?: ""
            val devNum = if (devPart.startsWith("dev")) {
                devPart.filter { it.isDigit() }.toIntOrNull() ?: 0
            } else {
                0
            }
            
            val result = VersionComparable(major, minor, patch, suffixName, suffixNum, devNum)
            
            if (debug) {
                println("DEBUG ComposeVersionComparator: '$version' -> major=$major, minor=$minor, patch=$patch, suffix='$suffixName', suffixNum=$suffixNum, devNum=$devNum")
            }
            
            result
        } catch (e: Exception) {
            VersionComparable(0, 0, 0, version, 0, 0)
        }
    }
    
    data class VersionComparable(
        val major: Int,
        val minor: Int,
        val patch: Int,
        val suffix: String,
        val suffixNum: Int,
        val devNum: Int
    ) : Comparable<VersionComparable> {
        override fun compareTo(other: VersionComparable): Int {
            if (major != other.major) return major.compareTo(other.major)
            if (minor != other.minor) return minor.compareTo(other.minor)
            if (patch != other.patch) return patch.compareTo(other.patch)
            
            // "zzz" means no suffix (stable), which is higher than any suffix
            // Lexicographic comparison: "zzz" > "rc" > "dev" > "beta" > "alpha"
            val suffixCompare = suffix.compareTo(other.suffix)
            if (suffixCompare != 0) return suffixCompare
            
            // For same suffix, compare by number (higher is newer)
            if (suffixNum != other.suffixNum) return suffixNum.compareTo(other.suffixNum)
            
            // For same suffix and number, compare by dev number (higher dev is newer)
            return devNum.compareTo(other.devNum)
        }
    }
}

