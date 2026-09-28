package com.koreageo.quiz.quiz

data class GuessState(
    val revealed: Boolean = false,
    val wrongCount: Int = 0,
    val characterCountHintShown: Boolean = false,
    val choseongHintShown: Boolean = false,
)

/**
 * [showLabelsInBrowseMode]: whether region names show while browsing (before 도전), toggled from
 * the top bar. The two hint flags decide whether that kind of hint is ever offered/shown.
 */
data class QuizSettings(
    val showLabelsInBrowseMode: Boolean = true,
    val characterCountHintEnabled: Boolean = true,
    val choseongHintEnabled: Boolean = true,
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

    /** 도전이 끝났다(전부 맞힘 또는 "여기까지"). [showResult]면 결과 다이얼로그/폭죽을 보여준다. */
    data class QuizEnded(val showResult: Boolean) : UiEvent()
}
