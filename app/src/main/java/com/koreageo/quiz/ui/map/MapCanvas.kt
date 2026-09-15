package com.koreageo.quiz.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.koreageo.quiz.geo.Region
import com.koreageo.quiz.geo.hitTest
import com.koreageo.quiz.quiz.GuessState
import kotlinx.coroutines.launch

@Composable
fun MapCanvas(
    regions: List<Region>,
    guesses: Map<String, GuessState>,
    started: Boolean,
    selectedRegionCode: String?,
    camera: CameraState,
    onCanvasSizeChanged: (Size) -> Unit,
    onTapRegion: (Region) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val textMeasurer = rememberTextMeasurer()

    Canvas(
        modifier = modifier
            .onSizeChanged { onCanvasSizeChanged(it.toSize()) }
            .pointerInput(regions) {
                detectTapGestures { screenPoint ->
                    val worldPoint = camera.current.screenToWorld(screenPoint)
                    regions.hitTest(worldPoint)?.let(onTapRegion)
                }
            }
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    scope.launch {
                        if (zoom != 1f) camera.zoomBy(zoom, centroid)
                        if (pan != Offset.Zero) camera.panBy(pan.x, pan.y)
                    }
                }
            },
    ) {
        val transform = camera.current
        regions.forEachIndexed { index, region ->
            val guess = guesses[region.code] ?: GuessState()
            val isSelected = region.code == selectedRegionCode
            val baseColor = REGION_PALETTE[index % REGION_PALETTE.size]
            val fillColor = if (guess.revealed) {
                lighten(baseColor, REGION_REVEALED_FILL_BOOST)
            } else {
                baseColor
            }

            val path = Path()
            for (ring in region.rings) {
                if (ring.isEmpty()) continue
                val first = transform.worldToScreen(ring.first())
                path.moveTo(first.x, first.y)
                for (i in 1 until ring.size) {
                    val p = transform.worldToScreen(ring[i])
                    path.lineTo(p.x, p.y)
                }
                path.close()
            }

            drawPath(path, color = fillColor, style = Fill)
            drawPath(
                path,
                color = if (isSelected) REGION_SELECTED_STROKE else REGION_STROKE,
                style = Stroke(width = if (isSelected) 4f else 1.5f),
            )

            val showLabel = !started || guess.revealed
            if (showLabel) {
                val labelPoint = transform.worldToScreen(region.centroid)
                val layout = textMeasurer.measure(
                    text = region.name,
                    style = TextStyle(
                        color = LABEL_TEXT_COLOR,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    ),
                )
                drawText(
                    layout,
                    topLeft = Offset(
                        labelPoint.x - layout.size.width / 2f,
                        labelPoint.y - layout.size.height / 2f,
                    ),
                )
            }
        }
    }
}

private fun lighten(color: Color, amount: Float): Color = Color(
    red = (color.red + (1f - color.red) * amount).coerceIn(0f, 1f),
    green = (color.green + (1f - color.green) * amount).coerceIn(0f, 1f),
    blue = (color.blue + (1f - color.blue) * amount).coerceIn(0f, 1f),
    alpha = color.alpha,
)
