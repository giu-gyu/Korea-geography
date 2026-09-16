package com.koreageo.quiz.ui.map

import androidx.compose.ui.graphics.Color

/** Fill colors assigned per-region by [assignRegionColors] so neighbors never match. */
val REGION_PALETTE = listOf(
    Color(0xFFB8D8D8),
    Color(0xFFD8C9B8),
    Color(0xFFC9D8B8),
    Color(0xFFD8B8C9),
    Color(0xFFB8C9D8),
    Color(0xFFE0D0A0),
    Color(0xFFCBB8D8),
    Color(0xFFA9D0C4),
    Color(0xFFD8CFA0),
    Color(0xFFA0C7D8),
    Color(0xFFCED8A0),
    Color(0xFFD8A0B7),
)

val REGION_STROKE = Color(0xFF3E5C58)
val REGION_REVEALED_FILL_BOOST = 0.15f
val REGION_SELECTED_STROKE = Color(0xFFE2572B)
val LABEL_TEXT_COLOR = Color(0xFF1F2D2A)
