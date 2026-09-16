package com.koreageo.quiz.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.koreageo.quiz.geo.Region
import com.koreageo.quiz.geo.hitTest
import com.koreageo.quiz.quiz.GuessState
import kotlin.math.sqrt
import kotlinx.coroutines.launch

@Composable
fun MapCanvas(
    regions: List<Region>,
    guesses: Map<String, GuessState>,
    started: Boolean,
    selectedRegionCode: String?,
    camera: CameraState,
    fitScale: Float,
    baseLabelSp: Float,
    isNationalLevel: Boolean,
    showLabelsInBrowseMode: Boolean,
    onCanvasSizeChanged: (Size) -> Unit,
    onTapRegion: (Region) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val textMeasurer = rememberTextMeasurer()
    // Draw larger regions first so a smaller one that sits inside/against a bigger
    // neighbor (e.g. 서울특별시 in/against 경기도) renders on top instead of getting
    // painted over.
    val drawOrder = remember(regions) { regions.sortedByDescending { it.approxArea } }
    // Bounding-box-adjacency-based coloring, not list position — see assignRegionColors().
    val colorAssignment = remember(regions) { assignRegionColors(regions, REGION_PALETTE.size) }

    Canvas(
        modifier = modifier
            .onSizeChanged { onCanvasSizeChanged(it.toSize()) }
            .pointerInput(regions) {
                detectTapGestures { screenPoint ->
                    val worldPoint = camera.current.screenToWorld(screenPoint)
                    regions.hitTest(worldPoint)?.let(onTapRegion)
                }
            }
            .pointerInput(fitScale) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    scope.launch {
                        if (zoom != 1f) {
                            camera.zoomBy(zoom, centroid, minScale = fitScale * 0.85f, maxScale = fitScale * 8f)
                        }
                        if (pan != Offset.Zero) camera.panBy(pan.x, pan.y)
                    }
                }
            },
    ) {
        val transform = camera.current

        // Pass 1: fill + stroke every polygon first. Labels are drawn in a separate pass
        // below, strictly after every polygon — otherwise a region drawn later (smaller
        // regions draw last, see drawOrder above) paints its fill right over a label that
        // an earlier, larger region already drew at that screen position.
        for (region in drawOrder) {
            val guess = guesses[region.code] ?: GuessState()
            val isSelected = region.code == selectedRegionCode
            val baseColor = REGION_PALETTE[colorAssignment[region.code] ?: 0]
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
            val fillColor = if (guess.revealed) lighten(baseColor, REGION_REVEALED_FILL_BOOST) else baseColor
            drawPath(path, color = fillColor, style = Fill)
            drawPath(
                path,
                color = if (isSelected) REGION_SELECTED_STROKE else REGION_STROKE,
                style = Stroke(width = if (isSelected) 4f else 1.5f),
            )
        }

        // Pass 2: measure every visible label, then push overlapping ones apart by the
        // minimum amount before drawing any of them on top of the finished polygon layer.
        val labelBoxes = ArrayList<LabelBox>(drawOrder.size)
        for (region in drawOrder) {
            val guess = guesses[region.code] ?: GuessState()
            val showText = guess.revealed || (!started && showLabelsInBrowseMode)
            val showBlank = started && !guess.revealed
            if (!showText && !showBlank) continue

            // Labels grow as the user zooms in past this level's default fit, and shrink
            // (down to a floor) when zoomed out — sqrt-damped so screen distance between
            // neighboring labels grows faster than the text itself, easing overlap.
            val zoomRatio = (transform.scale / fitScale).coerceAtLeast(0.05f)
            val fontSizeSp = (baseLabelSp * sqrt(zoomRatio)).coerceIn(baseLabelSp * 0.6f, baseLabelSp * 3f)
            // 광역자치단체(시/도) 이름은 진하고 이탤릭체로, 그 아래 시/군/구 이름은
            // 보통 굵기로 그려서 지도만 보고도 어느 행정 단계인지 구분되게 한다.
            val layout = textMeasurer.measure(
                text = region.name,
                style = if (isNationalLevel) {
                    TextStyle(
                        color = LABEL_TEXT_COLOR,
                        fontSize = fontSizeSp.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        letterSpacing = 0.05.em,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    TextStyle(
                        color = LABEL_TEXT_COLOR,
                        fontSize = fontSizeSp.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                },
            )
            val anchor = transform.worldToScreen(region.centroid)
            labelBoxes.add(LabelBox(layout, isBlank = !showText, anchor.x, anchor.y))
        }

        resolveLabelOverlaps(labelBoxes)

        val blankPaddingPx = 4.dp.toPx()
        for (box in labelBoxes) {
            if (box.isBlank) {
                val topLeft = Offset(box.cx - box.halfWidth - blankPaddingPx, box.cy - box.halfHeight - blankPaddingPx)
                val size = Size(box.layout.size.width + blankPaddingPx * 2, box.layout.size.height + blankPaddingPx * 2)
                val cornerRadius = CornerRadius(blankPaddingPx)
                drawRoundRect(color = BLANK_FILL_COLOR, topLeft = topLeft, size = size, cornerRadius = cornerRadius)
                drawRoundRect(
                    color = BLANK_STROKE_COLOR,
                    topLeft = topLeft,
                    size = size,
                    cornerRadius = cornerRadius,
                    style = Stroke(width = 1.5f),
                )
            } else {
                drawText(
                    box.layout,
                    topLeft = Offset(box.cx - box.halfWidth, box.cy - box.halfHeight),
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
