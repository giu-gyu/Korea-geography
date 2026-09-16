package com.koreageo.quiz.ui.map

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun CompletionDialog(
    levelTitle: String,
    durationMillis: Long?,
    showBackToNational: Boolean,
    onRestart: () -> Unit,
    onBackToNational: () -> Unit,
    onDismiss: () -> Unit,
) {
    val scale = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .graphicsLayer { scaleX = scale.value; scaleY = scale.value },
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = "🎉", fontSize = 48.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "축하합니다!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${levelTitle}의 모든 지역을 맞혔습니다!",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                if (durationMillis != null) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "걸린 시간: ${formatDuration(durationMillis)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CHALLENGE_BUTTON_COLOR,
                    )
                }
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (showBackToNational) {
                        OutlinedButton(onClick = onBackToNational) { Text("전국 지도로") }
                    } else {
                        OutlinedButton(onClick = onDismiss) { Text("닫기") }
                    }
                    Button(
                        onClick = onRestart,
                        colors = ButtonDefaults.buttonColors(containerColor = CHALLENGE_BUTTON_COLOR),
                    ) { Text("다시하기") }
                }
            }
        }
    }
}
