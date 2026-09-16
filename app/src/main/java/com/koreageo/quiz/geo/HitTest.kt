package com.koreageo.quiz.geo

import androidx.compose.ui.geometry.Offset
import kotlin.math.sqrt

/**
 * Finds which region (if any) contains [point], in world units. Smaller regions are tested
 * first so a tap in a spot where a small region sits inside/against a larger one (e.g.
 * 서울특별시 against 경기도) matches the one drawn on top, not whichever happens to come
 * first in the source list.
 *
 * If nothing exactly contains [point] but a region matching [isPreferred] (e.g. "not yet
 * answered") has its boundary within [toleranceWorldUnits] of the point, that region is
 * returned instead — a fat-finger assist. Without it, a small unanswered region surrounded by
 * an already-answered giant neighbor (경기도 fully solved, 서울특별시 still blank) becomes
 * nearly untappable, since any near-miss around its tiny edge lands back inside the giant one.
 */
fun List<Region>.hitTest(
    point: Offset,
    toleranceWorldUnits: Float = 0f,
    isPreferred: (Region) -> Boolean = { false },
): Region? {
    val ordered = sortedBy { it.approxArea }

    for (region in ordered) {
        if (containsPoint(region, point)) return region
    }

    if (toleranceWorldUnits <= 0f) return null

    var best: Region? = null
    var bestDistance = toleranceWorldUnits
    for (region in ordered) {
        if (!isPreferred(region)) continue
        val distance = distanceToBoundary(region, point)
        if (distance < bestDistance) {
            bestDistance = distance
            best = region
        }
    }
    return best
}

private fun containsPoint(region: Region, point: Offset): Boolean {
    val b = region.bounds
    if (point.x < b.left || point.x > b.right || point.y < b.top || point.y > b.bottom) return false
    for (ring in region.rings) {
        if (ringContains(ring, point)) return true
    }
    return false
}

/** Standard ray-casting point-in-polygon test against one closed ring. */
private fun ringContains(ring: List<Offset>, point: Offset): Boolean {
    var inside = false
    var j = ring.size - 1
    for (i in ring.indices) {
        val pi = ring[i]
        val pj = ring[j]
        val intersects = (pi.y > point.y) != (pj.y > point.y) &&
            point.x < (pj.x - pi.x) * (point.y - pi.y) / (pj.y - pi.y) + pi.x
        if (intersects) inside = !inside
        j = i
    }
    return inside
}

/** Minimum distance from [point] to the nearest edge of any of the region's rings. */
private fun distanceToBoundary(region: Region, point: Offset): Float {
    var minDistance = Float.MAX_VALUE
    for (ring in region.rings) {
        if (ring.size < 2) continue
        for (i in ring.indices) {
            val a = ring[i]
            val b = ring[(i + 1) % ring.size]
            val d = distancePointToSegment(point, a, b)
            if (d < minDistance) minDistance = d
        }
    }
    return minDistance
}

private fun distancePointToSegment(p: Offset, a: Offset, b: Offset): Float {
    val abx = b.x - a.x
    val aby = b.y - a.y
    val lengthSq = abx * abx + aby * aby
    val t = if (lengthSq > 0f) (((p.x - a.x) * abx + (p.y - a.y) * aby) / lengthSq).coerceIn(0f, 1f) else 0f
    val closestX = a.x + t * abx
    val closestY = a.y + t * aby
    val dx = p.x - closestX
    val dy = p.y - closestY
    return sqrt(dx * dx + dy * dy)
}
