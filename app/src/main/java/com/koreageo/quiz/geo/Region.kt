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
)
