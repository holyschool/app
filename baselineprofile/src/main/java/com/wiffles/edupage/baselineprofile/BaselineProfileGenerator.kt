package com.wiffles.edupage.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import org.junit.Rule
import org.junit.Test

/**
 * Records the cold-start path into a Baseline Profile.
 *
 * A signed-in install starts straight on the cached main UI, so a plain
 * [androidx.benchmark.macro.MacrobenchmarkScope.startActivityAndWait] captures the whole startup
 * journey: process init, Hilt graph, theme, nav graph, shell and overview composition.
 */
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun startup() = rule.collect(
        packageName = "com.wiffles.edupage",
    ) {
        pressHome()
        startActivityAndWait()
    }
}