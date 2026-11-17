package com.noor.core.update

/** Semantic version (major.minor.patch) parsed from tags like "v1.2.3" or "1.2". Pre-release suffixes are ignored. */
data class Version(val major: Int, val minor: Int, val patch: Int) : Comparable<Version> {
    override fun compareTo(other: Version): Int =
        compareValuesBy(this, other, Version::major, Version::minor, Version::patch)

    override fun toString() = "$major.$minor.$patch"

    companion object {
        private val pattern = Regex("""^[vV]?(\d+)(?:\.(\d+))?(?:\.(\d+))?""")

        fun parse(text: String?): Version? {
            val m = pattern.find(text?.trim() ?: return null) ?: return null
            return Version(
                m.groupValues[1].toInt(),
                m.groupValues[2].ifEmpty { "0" }.toInt(),
                m.groupValues[3].ifEmpty { "0" }.toInt(),
            )
        }

        /** True if [candidate] is a strictly newer stable version than [installed]. */
        fun isNewer(candidate: String?, installed: String?): Boolean {
            val c = parse(candidate) ?: return false
            val i = parse(installed) ?: return true
            return c > i
        }
    }
}
