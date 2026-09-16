package com.koreageo.quiz.ui.map

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.random.Random

private val CONFETTI_COLORS = listOf(
    Color(0xFFFF6D28),
    Color(0xFFFFC542),
    Color(0xFF4CAF93),
    Color(0xFF3E8DDE),
    Color(0xFFE2557A),
    Color(0xFF8E6ADB),
)

private const val PARTICLE_COUNT = 70
private const val DURATION_MILLIS = 2400

private class ConfettiParticle(
    val xFraction: Float,
    val startDelayFraction: Float,
    val fallDistanceFraction: Float,
    val driftAmplitude: Float,
    val driftPhase: Float,
    val rotationSpeed: Float,
    val sizePx: Float,
    val color: Color,
) {
    companion object {
        fun random(): ConfettiParticle = ConfettiParticle(
            xFraction = Random.nextFloat(),
            startDelayFraction = Random.nextFloat() * 0.3f,
            fallDistanceFraction = 0.55f + Random.nextFloat() * 0.55f,
            driftAmplitude = 20f + Random.nextFloat() * 40f,
            driftPhase = Random.nextFloat() * (Math.PI.toFloat() * 2f),
            rotationSpeed = (Random.nextFloat() - 0.5f) * 900f,
            sizePx = 10f + Random.nextFloat() * 8f,
            color = CONFETTI_COLORS[Random.nextInt(CONFETTI_COLORS.size)],
        )
    }
}

/**
 * A one-shot confetti burst, replayed whenever [trigger] changes to a new non-null value (a
 * timestamp works well). Purely decorative — draws over whatever is behind it and ignores touch.
 */
@Composable
fun ConfettiOverlay(trigger: Any?, modifier: Modifier = Modifier) {
    if (trigger == null) return

    val particles = remember(trigger) { List(PARTICLE_COUNT) { ConfettiParticle.random() } }
    val progress = remember(trigger) { Animatable(0f) }

    LaunchedEffect(trigger) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(DURATION_MILLIS, easing = LinearEasing))
    }

    if (progress.value >= 1f) return

    Canvas(modifier = modifier) {
        val p = progress.value
        for (particle in particles) {
            val localProgress = ((p - particle.startDelayFraction) / (1f - particle.startDelayFraction))
                .coerceIn(0f, 1f)
            if (localProgress <= 0f) continue

            val x = particle.xFraction * size.width +
                kotlin.math.sin(localProgress * Math.PI.toFloat() * 2f + particle.driftPhase) * particle.driftAmplitude
            val y = -particle.sizePx + localProgress * (size.height * particle.fallDistanceFraction + particle.sizePx * 2f)
            val alpha = (1f - localProgress).coerceIn(0f, 1f)

            rotate(degrees = particle.rotationSpeed * localProgress, pivot = Offset(x, y)) {
                drawRect(
                    color = particle.color.copy(alpha = alpha),
                    topLeft = Offset(x - particle.sizePx / 2f, y - particle.sizePx / 4f),
                    size = Size(particle.sizePx, particle.sizePx / 2f),
                )
            }
        }
    }
}
