package com.bhanuagrawal.kmplab.macrobenchmark

import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ListBenchmark {

    @get:Rule
    val rule = MacrobenchmarkRule()

    // Column with verticalScroll
    @Test
    fun columnListScroll() = rule.measureRepeated(
        packageName = "com.bhanuagrawal.kmplab",
        metrics = listOf(FrameTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.COLD,
        setupBlock = {
            pressHome()
            startActivityAndWait { intent ->
                intent.putExtra("start_route", "/home")
            }
        }
    ) {
        device.waitForIdle()
        val scrollable = device.findObject(By.scrollable(true))
        scrollable?.fling(Direction.DOWN)
        device.waitForIdle()
        scrollable?.fling(Direction.UP)
        device.waitForIdle()
    }

    // LazyColumn
    @Test
    fun lazyColumnListScroll() = rule.measureRepeated(
        packageName = "com.bhanuagrawal.kmplab",
        metrics = listOf(FrameTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.COLD,
        setupBlock = {
            pressHome()
            startActivityAndWait { intent ->
                intent.putExtra("start_route", "/lazy")
            }
        }
    ) {
        device.waitForIdle()
        val scrollable = device.findObject(By.scrollable(true))
        scrollable?.fling(Direction.DOWN)
        device.waitForIdle()
        scrollable?.fling(Direction.UP)
        device.waitForIdle()
    }
}
