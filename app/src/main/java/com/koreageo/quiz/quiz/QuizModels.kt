package com.koreageo.quiz.quiz

data class GuessState(
    val revealed: Boolean = false,
    val wrongCount: Int = 0,
    val characterCountHintShown: Boolean = false,
    val choseongHintShown: Boolean = false,
)

/**
 * User-configurable hint behavior. `null` means that hint is turned off entirely (the "X"
 * option); 0 means it's shown immediately with no wrong guesses needed; 1-4 is the number of
 * wrong guesses required before it's automatically shown.
 */
data class QuizSettings(
    val showLabelsInBrowseMode: Boolean = true,
    val characterCountHintThreshold: Int? = 1,
    val choseongHintThreshold: Int? = 3,
)

fun GuessState.characterCountHintAvailable(settings: QuizSettings): Boolean {
    val threshold = settings.characterCountHintThreshold ?: return false
    return wrongCount >= threshold
}

fun GuessState.choseongHintAvailable(settings: QuizSettings): Boolean {
    val threshold = settings.choseongHintThreshold ?: return false
    return wrongCount >= threshold
}

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
