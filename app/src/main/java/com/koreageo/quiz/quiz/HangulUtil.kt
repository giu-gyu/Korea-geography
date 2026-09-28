package com.koreageo.quiz.quiz

object HangulUtil {
    private const val SYLLABLE_BASE = 0xAC00
    private const val SYLLABLE_END = 0xD7A3
    private const val JUNG_COUNT = 21
    private const val JONG_COUNT = 28

    private val CHOSEONG = charArrayOf(
        'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ',
        'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ',
    )

    private val COMPOSITE_JUNG = setOf(9, 10, 11, 14, 15, 16, 19)
    private val COMPOSITE_JONG = setOf(3, 5, 6, 9, 10, 11, 12, 13, 14, 15, 18)

    /**
     * 자모 단위로 센 글자 수. 한글 IME는 받침/모음을 지워도 글자 수가 그대로인 경우가 많아서
     * (예: "한" -> "하"), 백스페이스 여부는 이 값이 줄었는지로 판단한다.
     */
    fun jamoCount(text: String): Int {
        var count = 0
        for (ch in text) {
            val code = ch.code
            if (code in SYLLABLE_BASE..SYLLABLE_END) {
                val offset = code - SYLLABLE_BASE
                val jung = (offset % (JUNG_COUNT * JONG_COUNT)) / JONG_COUNT
                val jong = offset % JONG_COUNT
                count += 1 + (if (jung in COMPOSITE_JUNG) 2 else 1) +
                    (if (jong == 0) 0 else if (jong in COMPOSITE_JONG) 2 else 1)
            } else {
                count += 1
            }
        }
        return count
    }

    /** e.g. "부천시" -> "ㅂㅊㅅ". Non-Hangul-syllable characters pass through unchanged. */
    fun choseong(text: String): String = buildString {
        for (ch in text) {
            val code = ch.code
            if (code in SYLLABLE_BASE..SYLLABLE_END) {
                val choIndex = (code - SYLLABLE_BASE) / (JUNG_COUNT * JONG_COUNT)
                append(CHOSEONG[choIndex])
            } else {
                append(ch)
            }
        }
    }
}

/** Trims and strips internal whitespace so "수원 시" and "수원시" compare equal. */
fun normalizeAnswer(input: String): String = input.trim().replace("\\s+".toRegex(), "")

/**
 * Common short forms accepted at the province level, e.g. "서울" for "서울특별시". Keys are the
 * current (2026-07-01 개편 반영) official names as loaded from the bundled boundary data.
 */
private val PROVINCE_ALIASES: Map<String, Set<String>> = mapOf(
    "서울특별시" to setOf("서울", "서울시"),
    "부산광역시" to setOf("부산", "부산시"),
    "대구광역시" to setOf("대구", "대구시"),
    "인천광역시" to setOf("인천", "인천시"),
    "대전광역시" to setOf("대전", "대전시"),
    "울산광역시" to setOf("울산", "울산시"),
    "세종시" to setOf("세종", "세종특별자치시"),
    "경기도" to setOf("경기"),
    "강원특별자치도" to setOf("강원", "강원도"),
    "충청북도" to setOf("충북"),
    "충청남도" to setOf("충남"),
    "전북특별자치도" to setOf("전북", "전라북도"),
    "경상북도" to setOf("경북"),
    "경상남도" to setOf("경남"),
    "제주도" to setOf("제주", "제주특별자치도"),
    // 2026-07-01 광주광역시+전라남도 통합 출범.
    "전남광주통합특별시" to setOf("광주전남", "전남광주", "광주", "전남"),
)

/** 시/군/구는 항상 이 세 글자 중 하나로 끝나므로, 그 글자를 뗀 형태도 정답으로 인정한다. */
private val STRIPPABLE_SUFFIXES = setOf('시', '군', '구')

fun isCorrectAnswer(input: String, correctName: String): Boolean {
    val normalizedInput = normalizeAnswer(input)
    if (normalizedInput.isEmpty()) return false
    if (normalizedInput == correctName) return true

    // 경산시->경산, 청도군->청도, 서초구->서초 처럼 마지막 글자(시/군/구)를 뗀 형태도 인정한다.
    // "도"로 끝나는 광역자치단체(경상북도 등)는 단순히 한 글자를 떼는 게 아니라 아래
    // PROVINCE_ALIASES에 별도로 정리된 축약형(경북 등)을 쓰므로 여기서는 다루지 않는다.
    if (correctName.length > 1 && correctName.last() in STRIPPABLE_SUFFIXES &&
        normalizedInput == correctName.dropLast(1)
    ) {
        return true
    }

    val aliases = PROVINCE_ALIASES[correctName] ?: return false
    return normalizedInput in aliases
}
