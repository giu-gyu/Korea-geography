package com.koreageo.quiz.ui.map

import androidx.compose.ui.geometry.Offset
import com.koreageo.quiz.geo.Region

/**
 * Manual nudges (in world units, same scale as [Region.centroid] — roughly degrees) for the
 * handful of labels whose raw centroids sit close enough together to overlap. 서울특별시,
 * 인천광역시 and 경기도 all land at nearly the same latitude at the national zoom level, so
 * their labels collide; 경기도's own territory is large enough to move its label away from
 * that cluster without it landing outside the province, and 서울/인천 get pushed apart along
 * the axis they were already offset on. World y is -latitude, so a negative dy moves a label
 * up/north on screen and a positive dy moves it down/south.
 */
private val LABEL_OFFSETS: Map<String, Offset> = mapOf(
    "서울특별시" to Offset(0f, -0.10f),
    "인천광역시" to Offset(-0.05f, 0.16f),
    "경기도" to Offset(0.25f, 0.55f),
)

fun labelAnchor(region: Region): Offset {
    val offset = LABEL_OFFSETS[region.name] ?: return region.centroid
    return region.centroid + offset
}
