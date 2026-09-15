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

    suspend fun panBy(dx: Float, dy: Float) = coroutineScope {
        launch { tx.snapTo(tx.value + dx) }
        launch { ty.snapTo(ty.value + dy) }
    }

    suspend fun zoomBy(factor: Float, focus: Offset) = coroutineScope {
        val newScale = (scale.value * factor).coerceIn(0.3f, 40f)
        val actualFactor = newScale / scale.value
        val newTx = focus.x - (focus.x - tx.value) * actualFactor
        val newTy = focus.y - (focus.y - ty.value) * actualFactor
        launch { scale.snapTo(newScale) }
        launch { tx.snapTo(newTx) }
        launch { ty.snapTo(newTy) }
    }
}
