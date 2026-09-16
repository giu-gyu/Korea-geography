package com.koreageo.quiz.geo

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

/**
 * One administrative region (a province, or a city/county within a province).
 * [rings] holds one closed polygon per landmass part, already projected to local
 * planar "world" units (see [Projector]) — holes are dropped since Korean admin
 * boundaries have none worth the added rendering complexity.
 */
data class Region(
    val code: String,
    val name: String,
    val rings: List<List<Offset>>,
    val centroid: Offset,
    val bounds: Rect,
) {
    /**
     * Bounding-box footprint, used only to order drawing/hit-testing so small regions
     * (e.g. 서울특별시) render on top of and take tap priority over a larger neighboring
     * region they sit inside or against (e.g. 경기도) — some source polygons are not
     * perfectly disjoint at their shared border, so without this the smaller region can
     * get partly or fully painted over by the larger one drawn after it.
     */
    val approxArea: Float get() = bounds.width * bounds.height
}
