package com.koreageo.quiz.session

import com.koreageo.quiz.quiz.GuessState

/**
 * Everything needed to resume a quiz exactly where it was — which screen was open and every
 * level's progress so far. Restored on process death (Android can kill a backgrounded app's
 * process at any time; without this, reopening the app looked like the quiz had "reset").
 */
data class SavedSession(
    val currentLevelType: String, // "national" or "province"
    val currentProvinceCode: String?,
    val currentProvinceName: String?,
    val guessesByLevel: Map<String, Map<String, GuessState>>,
    val startedByLevel: Map<String, Boolean>,
    val questStartedAtByLevel: Map<String, Long>,
)
