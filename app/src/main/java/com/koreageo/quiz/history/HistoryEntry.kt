package com.koreageo.quiz.history

/** One completed quiz run: which level, when it finished, and how long it took. */
data class HistoryEntry(
    val levelName: String,
    val completedAtMillis: Long,
    val durationMillis: Long,
)
