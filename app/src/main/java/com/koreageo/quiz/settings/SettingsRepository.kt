package com.koreageo.quiz.settings

import android.content.Context
import com.koreageo.quiz.quiz.QuizSettings

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): QuizSettings {
        val defaults = QuizSettings()
        return QuizSettings(
            showLabelsInBrowseMode = prefs.getBoolean(KEY_SHOW_LABELS, defaults.showLabelsInBrowseMode),
            characterCountHintEnabled = prefs.getBoolean(KEY_CHAR_COUNT_HINT, defaults.characterCountHintEnabled),
            choseongHintEnabled = prefs.getBoolean(KEY_CHOSEONG_HINT, defaults.choseongHintEnabled),
        )
    }

    fun save(settings: QuizSettings) {
        prefs.edit()
            .putBoolean(KEY_SHOW_LABELS, settings.showLabelsInBrowseMode)
            .putBoolean(KEY_CHAR_COUNT_HINT, settings.characterCountHintEnabled)
            .putBoolean(KEY_CHOSEONG_HINT, settings.choseongHintEnabled)
            .apply()
    }

    /** 지도를 눌러 도/시에 진입한 횟수를 1 늘리고 새 값을 돌려준다 (앱을 껐다 켜도 이어서 센다). */
    fun incrementProvinceEntryCount(): Int {
        val next = prefs.getInt(KEY_PROVINCE_ENTRY_COUNT, 0) + 1
        prefs.edit().putInt(KEY_PROVINCE_ENTRY_COUNT, next).apply()
        return next
    }

    private companion object {
        const val PREFS_NAME = "quiz_settings"
        const val KEY_SHOW_LABELS = "showLabelsInBrowseMode"
        const val KEY_CHAR_COUNT_HINT = "characterCountHintEnabled"
        const val KEY_CHOSEONG_HINT = "choseongHintEnabled"
        const val KEY_PROVINCE_ENTRY_COUNT = "provinceEntryCount"
    }
}
