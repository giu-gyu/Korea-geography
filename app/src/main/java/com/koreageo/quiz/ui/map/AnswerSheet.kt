package com.koreageo.quiz.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.koreageo.quiz.quiz.GuessState
import com.koreageo.quiz.quiz.HangulUtil
import com.koreageo.quiz.quiz.QuizSettings
import com.koreageo.quiz.quiz.characterCountHintAvailable
import com.koreageo.quiz.quiz.choseongHintAvailable
import com.koreageo.quiz.quiz.isCorrectAnswer

@Composable
fun AnswerSheet(
    guess: GuessState,
    targetName: String,
    settings: QuizSettings,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var answer by remember(guess) { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.wrapContentSize()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "이 지역의 이름은 무엇일까요?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = answer,
                    onValueChange = { newValue ->
                        answer = newValue
                        // "확인"을 누르지 않아도, 입력값이 정답과 일치하는 순간 바로 정답 처리한다.
                        if (isCorrectAnswer(newValue, targetName)) {
                            onSubmit(newValue)
                        }
                    },
                    singleLine = true,
                    placeholder = { Text("정답을 입력하세요") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onDone = { if (answer.isNotBlank()) onSubmit(answer) },
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                if (guess.wrongCount > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "오답입니다. 다시 시도해보세요. (${guess.wrongCount}회 오답)",
                        color = Color(0xFFC0392B),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                // 힌트는 버튼을 누르지 않아도 설정된 틀린 횟수 조건을 만족하면 바로 나온다.
                if (guess.characterCountHintAvailable(settings)) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "글자수 힌트: ${targetName.length}글자",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                if (guess.choseongHintAvailable(settings)) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "초성 힌트: ${HangulUtil.choseong(targetName)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(onClick = onDismiss) { Text("취소") }
                    Button(
                        onClick = { if (answer.isNotBlank()) onSubmit(answer) },
                        enabled = answer.isNotBlank(),
                    ) { Text("확인") }
                }
            }
        }
    }
}
