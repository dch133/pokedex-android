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

package com.skydoves.pokedex.compose.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.skydoves.pokedex.compose.core.designsystem.theme.PokedexTheme
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PokedexRadarChart(
  stats: List<Int>,
  maxStat: Int,
  labels: List<String>,
  modifier: Modifier = Modifier,
  color: Color = PokedexTheme.colors.primary,
) {
  val animationProgress = remember { Animatable(0f) }

  LaunchedEffect(stats) {
    animationProgress.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 1000)
    )
  }

  Box(
    modifier = modifier,
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val center = Offset(size.width / 2f, size.height / 2f)
      val radius = size.minDimension / 2f
      val angleStep = 2 * Math.PI / stats.size

      // Draw background webs
      for (i in 1..5) {
        val webRadius = radius * (i / 5f)
        val path = Path()
        for (j in stats.indices) {
          val angle = j * angleStep - Math.PI / 2
          val x = center.x + webRadius * cos(angle).toFloat()
          val y = center.y + webRadius * sin(angle).toFloat()
          if (j == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path, Color.LightGray.copy(alpha = 0.5f), style = Stroke(width = 1.dp.toPx()))
      }

      // Draw axes
      for (i in stats.indices) {
        val angle = i * angleStep - Math.PI / 2
        val x = center.x + radius * cos(angle).toFloat()
        val y = center.y + radius * sin(angle).toFloat()
        drawLine(Color.LightGray.copy(alpha = 0.5f), center, Offset(x, y), strokeWidth = 1.dp.toPx())
      }

      // Draw stat path
      val statPath = Path()
      for (i in stats.indices) {
        val angle = i * angleStep - Math.PI / 2
        val statValue = (stats[i].toFloat() / maxStat) * radius * animationProgress.value
        val x = center.x + statValue * cos(angle).toFloat()
        val y = center.y + statValue * sin(angle).toFloat()
        if (i == 0) statPath.moveTo(x, y) else statPath.lineTo(x, y)
      }
      statPath.close()

      drawPath(statPath, color.copy(alpha = 0.4f), style = Fill)
      drawPath(statPath, color, style = Stroke(width = 2.dp.toPx()))
    }
  }
}
