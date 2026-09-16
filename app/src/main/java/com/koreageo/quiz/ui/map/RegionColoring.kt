package com.koreageo.quiz.ui.map

import androidx.compose.ui.geometry.Rect
import com.koreageo.quiz.geo.Region

/** True if [a] and [b]'s bounding boxes touch or nearly touch, expanded by a small margin. */
private fun regionsAreNear(a: Region, b: Region): Boolean {
    val scale = maxOf(a.bounds.width, a.bounds.height, b.bounds.width, b.bounds.height)
    val margin = maxOf(0.02f, scale * 0.02f)
    val ar: Rect = a.bounds
    val br: Rect = b.bounds
    val noOverlap = ar.right + margin < br.left - margin ||
        br.right + margin < ar.left - margin ||
        ar.bottom + margin < br.top - margin ||
        br.bottom + margin < ar.top - margin
    return !noOverlap
}

/**
 * Assigns each region a palette index such that no two regions near enough to plausibly share
 * a border get the same one — plain "color by list position" cycles through the palette in
 * source-data order, which has no relation to geography, so neighbors like 서울특별시/경기도 or
 * 의정부시/남양주시/가평군 kept landing on the same color. Bounding-box proximity is a cheap
 * adjacency proxy that errs toward treating too many pairs as neighbors rather than too few,
 * which only makes the result more conservative, never wrong in the way that mattered here.
 */
fun assignRegionColors(regions: List<Region>, paletteSize: Int): Map<String, Int> {
    val n = regions.size
    val neighbors = Array(n) { mutableListOf<Int>() }
    for (i in 0 until n) {
        for (j in i + 1 until n) {
            if (regionsAreNear(regions[i], regions[j])) {
                neighbors[i].add(j)
                neighbors[j].add(i)
            }
        }
    }

    val colorOf = IntArray(n) { -1 }
    // Color the visually dominant (larger) regions first so they get first pick and spread out.
    val order = (0 until n).sortedByDescending { regions[it].approxArea }
    for (i in order) {
        val used = neighbors[i].mapNotNullTo(HashSet()) { colorOf[it].takeIf { c -> c >= 0 } }
        var color = 0
        while (color in used) color++
        colorOf[i] = color % paletteSize
    }

    return regions.indices.associate { regions[it].code to colorOf[it] }
}
