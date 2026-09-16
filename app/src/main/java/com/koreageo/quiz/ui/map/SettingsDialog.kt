package com.koreageo.quiz.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.koreageo.quiz.quiz.QuizSettings

@Composable
fun SettingsDialog(
    settings: QuizSettings,
    onSettingsChange: (QuizSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.wrapContentSize()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "설정",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("지역 이름 표시", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = settings.showLabelsInBrowseMode,
                        onCheckedChange = { onSettingsChange(settings.copy(showLabelsInBrowseMode = it)) },
                    )
                }

                Spacer(Modifier.height(20.dp))
                Text("글자수 힌트 (틀린 횟수, 0은 사용 안 함)", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                NumberChoiceRow(
                    value = settings.characterCountHintThreshold,
                    onValueChange = { onSettingsChange(settings.copy(characterCountHintThreshold = it)) },
                )

                Spacer(Modifier.height(20.dp))
                Text("초성 힌트 (틀린 횟수, 0은 사용 안 함)", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                NumberChoiceRow(
                    value = settings.choseongHintThreshold,
                    onValueChange = { onSettingsChange(settings.copy(choseongHintThreshold = it)) },
                )

                Spacer(Modifier.height(20.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("닫기")
                }
            }
        }
    }
}

@Composable
private fun NumberChoiceRow(value: Int, onValueChange: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (n in 0..4) {
            if (n == value) {
                Button(
                    onClick = { onValueChange(n) },
                    modifier = Modifier.size(44.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                ) { Text("$n") }
            } else {
                OutlinedButton(
                    onClick = { onValueChange(n) },
                    modifier = Modifier.size(44.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                ) { Text("$n") }
            }
        }
    }
}
