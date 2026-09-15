package com.koreageo.quiz.geo

import androidx.compose.ui.geometry.Offset

/** Finds which region (if any) contains [point], in world units. */
fun List<Region>.hitTest(point: Offset): Region? {
    for (region in this) {
        val b = region.bounds
        if (point.x < b.left || point.x > b.right || point.y < b.top || point.y > b.bottom) continue
        for (ring in region.rings) {
            if (ringContains(ring, point)) return region
        }
    }
    return null
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
