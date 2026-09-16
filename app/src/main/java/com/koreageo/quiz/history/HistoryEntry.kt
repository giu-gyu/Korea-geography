package com.koreageo.quiz.history

/**
 * One quiz run, whether finished completely or stopped early: which level, when it ended, how
 * long it took, and how many of the level's regions were revealed ([revealedCount] equals
 * [totalCount] for a full completion).
 */
data class HistoryEntry(
    val levelName: String,
    val completedAtMillis: Long,
    val durationMillis: Long,
    val revealedCount: Int,
    val totalCount: Int,
)
