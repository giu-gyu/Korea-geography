package com.koreageo.quiz.geo

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Loads the pre-processed boundary JSON bundled under assets/geo/. */
class GeoRepository(private val context: Context) {

    private var sidoCache: List<Region>? = null
    private val sigunguCache = mutableMapOf<String, List<Region>>()

    suspend fun loadSido(): List<Region> {
        sidoCache?.let { return it }
        val regions = withContext(Dispatchers.IO) { parseRegions("geo/sido.json") }
        sidoCache = regions
        return regions
    }

    suspend fun loadSigungu(provinceCode: String): List<Region> {
        sigunguCache[provinceCode]?.let { return it }
        val regions = withContext(Dispatchers.IO) { parseRegions("geo/sigungu_$provinceCode.json") }
        sigunguCache[provinceCode] = regions
        return regions
    }

    private fun parseRegions(assetPath: String): List<Region> {
        val text = context.assets.open(assetPath).bufferedReader(Charsets.UTF_8).use { it.readText() }
        val array = JSONArray(text)
        val result = ArrayList<Region>(array.length())
        for (i in 0 until array.length()) {
            result.add(parseRegion(array.getJSONObject(i)))
        }
        return result
    }

    private fun parseRegion(obj: JSONObject): Region {
        val code = obj.getString("code")
        val name = obj.getString("name")

        val ringsJson = obj.getJSONArray("rings")
        val rings = ArrayList<List<Offset>>(ringsJson.length())
        for (r in 0 until ringsJson.length()) {
            val ring = ringsJson.getJSONArray(r)
            val points = ArrayList<Offset>(ring.length())
            for (p in 0 until ring.length()) {
                val pt = ring.getJSONArray(p)
                points.add(Projector.project(pt.getDouble(0), pt.getDouble(1)))
            }
            rings.add(points)
        }

        val centroidJson = obj.getJSONArray("centroid")
        val centroid = Projector.project(centroidJson.getDouble(0), centroidJson.getDouble(1))

        val bboxJson = obj.getJSONArray("bbox")
        val corner1 = Projector.project(bboxJson.getDouble(0), bboxJson.getDouble(1))
        val corner2 = Projector.project(bboxJson.getDouble(2), bboxJson.getDouble(3))
        val bounds = Rect(
            left = minOf(corner1.x, corner2.x),
            top = minOf(corner1.y, corner2.y),
            right = maxOf(corner1.x, corner2.x),
            bottom = maxOf(corner1.y, corner2.y),
        )

        return Region(code = code, name = name, rings = rings, centroid = centroid, bounds = bounds)
    }
}
