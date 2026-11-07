package com.freeteaspoon.wppenhacer.utils

/** Release ordering is numeric; Git hashes identify builds but have no sortable order. */
internal data class ReleaseVersion(val parts: List<Int>, val hash: String?) : Comparable<ReleaseVersion> {
    override fun compareTo(other: ReleaseVersion): Int {
        parts.zip(other.parts).forEach { (installed, released) ->
            installed.compareTo(released).takeIf { it != 0 }?.let { return it }
        }
        return 0
    }

    fun sameBuild(other: ReleaseVersion): Boolean = hash != null && other.hash != null &&
        (hash.startsWith(other.hash, ignoreCase = true) || other.hash.startsWith(hash, ignoreCase = true))

    fun shouldUpdateTo(release: ReleaseVersion, compareStatus: String? = null): Boolean = when {
        release > this -> true
        release < this || sameBuild(release) -> false
        hash == null || release.hash == null -> false
        else -> compareStatus == "ahead"
    }

    companion object {
        private val pattern = Regex("""^v?(\d+)\.(\d+)\.(\d+)(?:\s+\(([a-fA-F0-9]{7,40})\)|-([a-fA-F0-9]{7,40}))?$""")

        fun parse(value: String?): ReleaseVersion? {
            val match = pattern.matchEntire(value?.trim() ?: return null) ?: return null
            val parts = (1..3).map { match.groupValues[it].toIntOrNull() ?: return null }
            val hash = match.groupValues[4].ifEmpty { match.groupValues[5] }.ifEmpty { null }?.lowercase()
            return ReleaseVersion(parts, hash)
        }
    }
}
