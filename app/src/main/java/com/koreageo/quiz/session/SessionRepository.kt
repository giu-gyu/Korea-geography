package com.koreageo.quiz.session

import android.content.Context
import com.koreageo.quiz.quiz.GuessState
import org.json.JSONObject

/**
 * Persists the in-progress quiz session (current screen + every level's progress) to
 * SharedPreferences so it survives the app's process being killed in the background, not just
 * configuration changes.
 */
class SessionRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(session: SavedSession) {
        val root = JSONObject()
        root.put("currentLevelType", session.currentLevelType)
        if (session.currentProvinceCode != null) root.put("currentProvinceCode", session.currentProvinceCode)
        if (session.currentProvinceName != null) root.put("currentProvinceName", session.currentProvinceName)

        val guessesJson = JSONObject()
        for ((levelKey, guesses) in session.guessesByLevel) {
            val levelObj = JSONObject()
            for ((code, g) in guesses) {
                levelObj.put(
                    code,
                    JSONObject()
                        .put("revealed", g.revealed)
                        .put("wrongCount", g.wrongCount)
                        .put("characterCountHintShown", g.characterCountHintShown)
                        .put("choseongHintShown", g.choseongHintShown),
                )
            }
            guessesJson.put(levelKey, levelObj)
        }
        root.put("guessesByLevel", guessesJson)

        val startedJson = JSONObject()
        for ((levelKey, started) in session.startedByLevel) startedJson.put(levelKey, started)
        root.put("startedByLevel", startedJson)

        val questStartedJson = JSONObject()
        for ((levelKey, at) in session.questStartedAtByLevel) questStartedJson.put(levelKey, at)
        root.put("questStartedAtByLevel", questStartedJson)

        prefs.edit().putString(KEY_SESSION, root.toString()).apply()
    }

    fun load(): SavedSession? {
        val raw = prefs.getString(KEY_SESSION, null) ?: return null
        return try {
            val root = JSONObject(raw)

            val guessesByLevel = mutableMapOf<String, Map<String, GuessState>>()
            root.optJSONObject("guessesByLevel")?.let { guessesJson ->
                val levelKeys = guessesJson.keys()
                while (levelKeys.hasNext()) {
                    val levelKey = levelKeys.next()
                    val levelObj = guessesJson.getJSONObject(levelKey)
                    val guesses = mutableMapOf<String, GuessState>()
                    val codeKeys = levelObj.keys()
                    while (codeKeys.hasNext()) {
                        val code = codeKeys.next()
                        val g = levelObj.getJSONObject(code)
                        guesses[code] = GuessState(
                            revealed = g.optBoolean("revealed", false),
                            wrongCount = g.optInt("wrongCount", 0),
                            characterCountHintShown = g.optBoolean("characterCountHintShown", false),
                            choseongHintShown = g.optBoolean("choseongHintShown", false),
                        )
                    }
                    guessesByLevel[levelKey] = guesses
                }
            }

            val startedByLevel = mutableMapOf<String, Boolean>()
            root.optJSONObject("startedByLevel")?.let { startedJson ->
                val keys = startedJson.keys()
                while (keys.hasNext()) {
                    val levelKey = keys.next()
                    startedByLevel[levelKey] = startedJson.getBoolean(levelKey)
                }
            }

            val questStartedAtByLevel = mutableMapOf<String, Long>()
            root.optJSONObject("questStartedAtByLevel")?.let { questJson ->
                val keys = questJson.keys()
                while (keys.hasNext()) {
                    val levelKey = keys.next()
                    questStartedAtByLevel[levelKey] = questJson.getLong(levelKey)
                }
            }

            SavedSession(
                currentLevelType = root.optString("currentLevelType", "national"),
                currentProvinceCode = if (root.has("currentProvinceCode")) root.getString("currentProvinceCode") else null,
                currentProvinceName = if (root.has("currentProvinceName")) root.getString("currentProvinceName") else null,
                guessesByLevel = guessesByLevel,
                startedByLevel = startedByLevel,
                questStartedAtByLevel = questStartedAtByLevel,
            )
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val PREFS_NAME = "quiz_session"
        private const val KEY_SESSION = "session"
    }
}
