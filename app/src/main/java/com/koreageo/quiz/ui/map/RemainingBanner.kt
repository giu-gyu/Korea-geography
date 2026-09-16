package com.koreageo.quiz.ui.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight

private val REMAINING_BANNER_COLOR = Color(0xFFFF3B30)
private val REMAINING_BANNER_OUTLINE_COLOR = Color(0xFF1B1B1B)

/**
 * A brief, no-background "N개 남았습니다" callout — bold colored text with a dark outline so it
 * reads clearly over the busy map underneath, no Snackbar box needed.
 */
@Composable
fun RemainingBanner(message: String?, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn() + scaleIn(initialScale = 0.85f),
        exit = fadeOut() + scaleOut(targetScale = 0.85f),
        modifier = modifier,
    ) {
        val baseStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold)
        Box {
            Text(
                text = message ?: "",
                style = baseStyle.copy(
                    drawStyle = Stroke(width = 7f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                ),
                color = REMAINING_BANNER_OUTLINE_COLOR,
            )
            Text(text = message ?: "", style = baseStyle, color = REMAINING_BANNER_COLOR)
        }
    }
}
