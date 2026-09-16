package com.koreageo.quiz.ui.map

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.koreageo.quiz.geo.Region
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** A scale + translation that maps world (projected lon/lat) units to screen pixels. */
data class Transform(val scale: Float, val tx: Float, val ty: Float) {
    fun worldToScreen(p: Offset): Offset = Offset(p.x * scale + tx, p.y * scale + ty)
    fun screenToWorld(p: Offset): Offset = Offset((p.x - tx) / scale, (p.y - ty) / scale)
}

/** Computes the transform that fits [bounds] inside [canvasSize] with [paddingFraction] margin. */
fun fitTransform(bounds: Rect, canvasSize: Size, paddingFraction: Float = 0.08f): Transform {
    if (bounds.width <= 0f || bounds.height <= 0f || canvasSize.width <= 0f || canvasSize.height <= 0f) {
        return Transform(1f, canvasSize.width / 2f, canvasSize.height / 2f)
    }
    val padding = 1f + paddingFraction * 2f
    val scaleX = canvasSize.width / (bounds.width * padding)
    val scaleY = canvasSize.height / (bounds.height * padding)
    val scale = minOf(scaleX, scaleY)
    val worldCenter = bounds.center
    val tx = canvasSize.width / 2f - worldCenter.x * scale
    val ty = canvasSize.height / 2f - worldCenter.y * scale
    return Transform(scale, tx, ty)
}

/** Bounding box that contains every region's bounds. */
fun unionBounds(regions: List<Region>): Rect {
    if (regions.isEmpty()) return Rect.Zero
    var left = Float.MAX_VALUE
    var top = Float.MAX_VALUE
    var right = -Float.MAX_VALUE
    var bottom = -Float.MAX_VALUE
    for (region in regions) {
        val b = region.bounds
        if (b.left < left) left = b.left
        if (b.top < top) top = b.top
        if (b.right > right) right = b.right
        if (b.bottom > bottom) bottom = b.bottom
    }
    return Rect(left, top, right, bottom)
}

/**
 * A transform that centers on [region] at a reasonable zoom for picking it out and answering
 * it — fit to its own bounds like [fitTransform], but with generous padding so neighbors stay
 * visible for context, and clamped relative to [fitScale] so a tiny region (e.g. 서울특별시 at
 * the national level) doesn't zoom in absurdly far.
 */
fun focusTransform(region: Region, canvasSize: Size, fitScale: Float): Transform {
    val raw = fitTransform(region.bounds, canvasSize, paddingFraction = 1.2f)
    val scale = raw.scale.coerceIn(fitScale * 0.9f, fitScale * 8f)
    val center = region.centroid
    val tx = canvasSize.width / 2f - center.x * scale
    val ty = canvasSize.height / 2f - center.y * scale
    return Transform(scale, tx, ty)
}

/** Shrinks [bounds] toward the given vertical anchor (0 = top, 1 = bottom), used for the intro fly-in. */
fun zoomedBounds(bounds: Rect, factor: Float, verticalAnchor: Float): Rect {
    val newWidth = bounds.width * factor
    val newHeight = bounds.height * factor
    val anchorY = bounds.top + bounds.height * verticalAnchor
    return Rect(
        left = bounds.center.x - newWidth / 2f,
        top = anchorY - newHeight * verticalAnchor,
        right = bounds.center.x + newWidth / 2f,
        bottom = anchorY + newHeight * (1f - verticalAnchor),
    )
}

@Stable
class CameraState {
    val scale = Animatable(1f)
    val tx = Animatable(0f)
    val ty = Animatable(0f)

    val current: Transform get() = Transform(scale.value, tx.value, ty.value)

    suspend fun snapToImmediate(transform: Transform) = coroutineScope {
        launch { scale.snapTo(transform.scale) }
        launch { tx.snapTo(transform.tx) }
        launch { ty.snapTo(transform.ty) }
    }

    suspend fun animateTo(transform: Transform, spec: AnimationSpec<Float> = tween(700)) = coroutineScope {
        launch { scale.animateTo(transform.scale, spec) }
        launch { tx.animateTo(transform.tx, spec) }
        launch { ty.animateTo(transform.ty, spec) }
    }

    suspend fun panBy(dx: Float, dy: Float) {
        tx.snapTo(tx.value + dx)
        ty.snapTo(ty.value + dy)
    }

    /**
     * [minScale]/[maxScale] must be in the same units as [Transform.scale] — which depends on
     * the world extent currently being fit to screen (a few hundred for the national view, a
     * few thousand for a single province). Pass bounds derived from the current fit scale
     * (e.g. `fitScale * 0.5f` / `fitScale * 8f`), never fixed absolute numbers: a fixed range
     * that happens to sit below the natural fit scale clamps the very first pinch down to that
     * ceiling and never lets it grow back.
     */
    suspend fun zoomBy(factor: Float, focus: Offset, minScale: Float, maxScale: Float) {
        val newScale = (scale.value * factor).coerceIn(minScale, maxScale)
        val actualFactor = newScale / scale.value
        val newTx = focus.x - (focus.x - tx.value) * actualFactor
        val newTy = focus.y - (focus.y - ty.value) * actualFactor
        scale.snapTo(newScale)
        tx.snapTo(newTx)
        ty.snapTo(newTy)
    }
}
