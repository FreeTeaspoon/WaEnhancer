package com.freeteaspoon.wppenhacer.utils

import org.junit.Assert.*
import org.junit.Test

class ReleaseVersionTest {
    private fun version(value: String) = requireNotNull(ReleaseVersion.parse(value))

    @Test fun newerInstalledBuildDoesNotOfferOlderRelease() {
        val installed = version("1.6.5 (F4DCF9C5)")
        val release = version("1.6.5-c35a30a6")
        assertFalse(installed.shouldUpdateTo(release, "behind"))
        assertFalse(installed.shouldUpdateTo(release))
        assertTrue(release.shouldUpdateTo(installed, "ahead"))
    }

    @Test fun sameVersionRequiresConfirmedAncestry() {
        val installed = version("1.6.6 (12345678)")
        val release = version("1.6.6-abcdef12")
        for (status in listOf(null, "behind", "identical", "diverged", "unknown")) {
            assertFalse(installed.shouldUpdateTo(release, status))
        }
        assertTrue(installed.shouldUpdateTo(release, "ahead"))
        assertFalse(installed.shouldUpdateTo(version("1.6.6-12345678"), "ahead"))
        assertFalse(installed.shouldUpdateTo(version("1.6.6-123456789abcdef0")))
    }

    @Test fun versionNumbersTakePrecedenceOverCommitIds() {
        assertTrue(version("1.6.5 (ffffffff)").shouldUpdateTo(version("1.6.6-00000000")))
        assertFalse(version("1.6.6 (00000000)").shouldUpdateTo(version("1.6.5-ffffffff"), "ahead"))
        assertTrue(version("1.6.9").shouldUpdateTo(version("1.6.10")))
        assertTrue(version("1.9.9").shouldUpdateTo(version("1.10.0")))
        assertTrue(version("1.99.99").shouldUpdateTo(version("2.0.0")))
    }

    @Test fun invalidAndPrereleaseNamesAreIgnored() {
        for (value in listOf(null, "", "1.6", "1.6.6-beta.1", "1.6.6-", "1.6.6-../../main", "999999999999.1.1")) {
            assertNull(ReleaseVersion.parse(value))
        }
        assertEquals(version("1.6.6-abcdef12"), version("v1.6.6-abcdef12"))
    }
}
