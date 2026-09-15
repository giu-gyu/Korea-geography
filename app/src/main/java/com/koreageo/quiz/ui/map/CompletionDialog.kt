package com.koreageo.quiz.ui.map

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun CompletionDialog(
    levelTitle: String,
    showBackToNational: Boolean,
    onRestart: () -> Unit,
    onBackToNational: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("축하합니다!") },
        text = { Text("${levelTitle}의 모든 지역을 맞혔습니다.") },
        confirmButton = {
            TextButton(onClick = onRestart) { Text("다시하기") }
        },
        dismissButton = {
            if (showBackToNational) {
                TextButton(onClick = onBackToNational) { Text("전국 지도로") }
            } else {
                TextButton(onClick = onDismiss) { Text("닫기") }
            }
        },
    )
}
