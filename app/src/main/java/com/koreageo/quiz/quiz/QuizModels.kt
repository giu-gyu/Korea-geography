package com.koreageo.quiz.quiz

data class GuessState(
    val revealed: Boolean = false,
    val wrongCount: Int = 0,
    val characterCountHintShown: Boolean = false,
    val choseongHintShown: Boolean = false,
)

/** Whether region names show while browsing (before 도전) — toggled directly from the top bar. */
data class QuizSettings(
    val showLabelsInBrowseMode: Boolean = true,
)

sealed class MapLevel {
    /** Stable key used to keep per-level quiz progress separate. */
    abstract val key: String

    data object National : MapLevel() {
        override val key: String = "national"
    }

    data class Province(val code: String, val name: String) : MapLevel() {
        override val key: String = "province:$code"
    }
}

fun MapLevel.displayName(): String = when (this) {
    is MapLevel.National -> "대한민국"
    is MapLevel.Province -> name
}

sealed class UiEvent {
    data object WrongAnswer : UiEvent()
    data object AlreadyRevealed : UiEvent()
    data object DrillNotAvailableYet : UiEvent()
}
