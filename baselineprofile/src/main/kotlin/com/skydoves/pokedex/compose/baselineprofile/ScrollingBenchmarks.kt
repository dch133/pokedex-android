/*
 * Designed and developed by 2024 skydoves (Jaewoong Eum)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.skydoves.pokedex.compose.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This test class benchmarks the frame performance (jank) of the pokedex list.
 * It uses [FrameTimingMetric] to measure the duration of frames produced during the scroll.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ScrollingBenchmarks {

  @get:Rule
  val rule = MacrobenchmarkRule()

  @Test
  fun scrollCompilationNone() = benchmark(CompilationMode.None())

  @Test
  fun scrollCompilationBaselineProfiles() =
    benchmark(CompilationMode.Partial(BaselineProfileMode.Require))

  private fun benchmark(compilationMode: CompilationMode) {
    rule.measureRepeated(
      packageName = InstrumentationRegistry.getArguments().getString("targetAppId")
        ?: throw Exception("targetAppId not passed as instrumentation runner arg"),
      metrics = listOf(FrameTimingMetric()),
      compilationMode = compilationMode,
      startupMode = StartupMode.WARM,
      iterations = 5,
      setupBlock = {
        pressHome()
        startActivityAndWait()
      },
      measureBlock = {
        explorePokedexHome()
      },
    )
  }
}
