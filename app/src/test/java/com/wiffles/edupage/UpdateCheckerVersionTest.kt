package com.wiffles.edupage

import com.wiffles.edupage.network.UpdateChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerVersionTest {

    private val checker = UpdateChecker()

    @Test
    fun plainVersions_compareNumerically() {
        assertTrue(checker.compareVersions("v1.4.0", "1.3.5") > 0)
        assertTrue(checker.compareVersions("1.3.5", "1.4.0") < 0)
        assertEquals(0, checker.compareVersions("v1.4.0", "1.4.0"))
        assertEquals(0, checker.compareVersions("1.4", "1.4.0"))
    }

    @Test
    fun release_isNewerThanItsPrerelease() {
        assertTrue(checker.compareVersions("1.5.0", "1.5.0-beta.1") > 0)
        assertTrue(checker.compareVersions("1.5.0-beta.1", "1.5.0") < 0)
    }

    @Test
    fun prereleases_compareByBaseThenNumbers() {
        assertTrue(checker.compareVersions("1.5.0-beta.2", "1.5.0-beta.1") > 0)
        assertTrue(checker.compareVersions("1.5.0-beta.1", "1.5.0-alpha.9") > 0)
        // A newer base beta still beats an older stable release.
        assertTrue(checker.compareVersions("1.5.0-beta.1", "1.4.0") > 0)
    }

    @Test
    fun betaInstalled_getsOfferedStableOfSameBase() {
        // Regression: an installed beta must see the matching stable release as newer.
        assertTrue(checker.compareVersions("v1.5.0", "1.5.0-beta.1") > 0)
    }
}
