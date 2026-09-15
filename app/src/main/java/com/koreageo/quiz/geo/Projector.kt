package com.koreageo.quiz.geo

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.cos

/**
 * Cheap equirectangular projection centered on South Korea. This is not
 * geodetically accurate, but at Korea's latitude span (~33-39N) the distortion
 * is imperceptible for a stylized quiz map, and it avoids pulling in a real
 * map/GIS SDK for what is otherwise a flat, tappable vector drawing.
 */
object Projector {
    private const val REF_LAT_DEG = 36.0
    private val REF_LAT_COS = cos(REF_LAT_DEG * PI / 180.0)

    /** lon/lat in degrees -> planar world unit (roughly degrees, x-compressed by latitude). */
    fun project(lon: Double, lat: Double): Offset {
        val x = (lon * REF_LAT_COS)
        val y = -lat
        return Offset(x.toFloat(), y.toFloat())
    }
}
