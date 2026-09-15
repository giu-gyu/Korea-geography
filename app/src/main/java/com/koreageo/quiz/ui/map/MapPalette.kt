package com.koreageo.quiz.ui.map

import androidx.compose.ui.graphics.Color

/** Cycling fill colors so neighboring regions read as visually distinct. */
val REGION_PALETTE = listOf(
    Color(0xFFB8D8D8),
    Color(0xFFD8C9B8),
    Color(0xFFC9D8B8),
    Color(0xFFD8B8C9),
    Color(0xFFB8C9D8),
    Color(0xFFE0D0A0),
    Color(0xFFCBB8D8),
    Color(0xFFA9D0C4),
)

val REGION_STROKE = Color(0xFF3E5C58)
val REGION_REVEALED_FILL_BOOST = 0.15f
val REGION_SELECTED_STROKE = Color(0xFFE2572B)
val LABEL_TEXT_COLOR = Color(0xFF1F2D2A)
