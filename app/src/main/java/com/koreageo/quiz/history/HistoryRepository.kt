package com.koreageo.quiz.history

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Persists completed-quiz records to SharedPreferences as a small JSON array, newest first. */
class HistoryRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadEntries(): List<HistoryEntry> {
        val raw = prefs.getString(KEY_ENTRIES, null) ?: return emptyList()
        val array = JSONArray(raw)
        val result = ArrayList<HistoryEntry>(array.length())
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            result.add(
                HistoryEntry(
                    levelName = obj.getString("levelName"),
                    completedAtMillis = obj.getLong("completedAtMillis"),
                    durationMillis = obj.getLong("durationMillis"),
                    revealedCount = obj.optInt("revealedCount", 0),
                    totalCount = obj.optInt("totalCount", 0),
                ),
            )
        }
        return result
    }

    fun addEntry(entry: HistoryEntry) {
        val entries = ArrayList(loadEntries())
        entries.add(0, entry)
        if (entries.size > MAX_ENTRIES) {
            entries.subList(MAX_ENTRIES, entries.size).clear()
        }
        val array = JSONArray()
        for (e in entries) {
            array.put(
                JSONObject()
                    .put("levelName", e.levelName)
                    .put("completedAtMillis", e.completedAtMillis)
                    .put("durationMillis", e.durationMillis)
                    .put("revealedCount", e.revealedCount)
                    .put("totalCount", e.totalCount),
            )
        }
        prefs.edit().putString(KEY_ENTRIES, array.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "quiz_history"
        private const val KEY_ENTRIES = "entries"
        private const val MAX_ENTRIES = 200
    }
}
