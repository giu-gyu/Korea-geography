package com.koreageo.quiz.ui.map

import androidx.compose.ui.text.TextLayoutResult
import kotlin.math.abs

/** One label being laid out: a mutable screen-space center that [resolveLabelOverlaps] nudges. */
class LabelBox(
    val layout: TextLayoutResult,
    var cx: Float,
    var cy: Float,
) {
    val halfWidth: Float get() = layout.size.width / 2f
    val halfHeight: Float get() = layout.size.height / 2f
}

/**
 * Nudges every pair of overlapping label boxes apart by the minimum amount needed so they
 * touch but don't overlap — along whichever axis (x or y) needs the smaller push, split evenly
 * between the two so a label only moves as far as it has to. Runs several relaxation passes
 * since separating one pair can shift a box into a new collision with a third (e.g. 충청남도 /
 * 세종특별자치시 / a neighboring province all landing close together).
 */
fun resolveLabelOverlaps(boxes: List<LabelBox>, iterations: Int = 8) {
    repeat(iterations) {
        for (i in boxes.indices) {
            for (j in i + 1 until boxes.size) {
                val a = boxes[i]
                val b = boxes[j]
                val dx = b.cx - a.cx
                val dy = b.cy - a.cy
                val overlapX = (a.halfWidth + b.halfWidth) - abs(dx)
                val overlapY = (a.halfHeight + b.halfHeight) - abs(dy)
                if (overlapX <= 0f || overlapY <= 0f) continue // not overlapping on this axis
                if (overlapX < overlapY) {
                    val push = overlapX / 2f
                    val sign = if (dx >= 0f) 1f else -1f
                    a.cx -= push * sign
                    b.cx += push * sign
                } else {
                    val push = overlapY / 2f
                    val sign = if (dy >= 0f) 1f else -1f
                    a.cy -= push * sign
                    b.cy += push * sign
                }
            }
        }
    }
}
