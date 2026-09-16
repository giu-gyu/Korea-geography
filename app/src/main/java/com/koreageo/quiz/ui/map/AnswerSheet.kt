package com.koreageo.quiz.ui.map

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.koreageo.quiz.quiz.GuessState
import com.koreageo.quiz.quiz.HangulUtil
import com.koreageo.quiz.quiz.isCorrectAnswer

private val ANSWER_ACCENT_COLOR = CHALLENGE_BUTTON_COLOR

@Composable
fun AnswerSheet(
    guess: GuessState,
    targetName: String,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
    onRequestCharacterCountHint: () -> Unit,
    onRequestChoseongHint: () -> Unit,
) {
    var answer by remember(guess) { mutableStateOf("") }

    // 화면 아래쪽 약 85~90% 지점에 띄운다 — 정중앙이나 그 위쪽에 뜨면 방금 탭한 작은
    // 지역(서울특별시 등)이 다이얼로그에 가려져서 어떤 지역이 눌렸는지 확인할 수 없다.
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // 카드 바깥(비활성화된 영역)을 누르면 취소 버튼 없이도 바로 문제를 취소한다.
                .pointerInput(Unit) { detectTapGestures { onDismiss() } }
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
            contentAlignment = BiasAlignment(0f, 0.85f),
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.86f)
                    // 카드 위 탭은 여기서 흡수해서 뒤쪽 배경의 취소 탭 핸들러로 전달되지 않게 한다.
                    .pointerInput(Unit) { detectTapGestures {} },
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    OutlinedTextField(
                        value = answer,
                        onValueChange = { newValue ->
                            answer = newValue
                            // 별도 확인 버튼 없이, 입력값이 정답과 일치하는 순간 바로 정답 처리한다.
                            if (isCorrectAnswer(newValue, targetName)) {
                                onSubmit(newValue)
                            }
                        },
                        singleLine = true,
                        placeholder = { Text("정답을 입력하세요", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ANSWER_ACCENT_COLOR,
                            cursorColor = ANSWER_ACCENT_COLOR,
                        ),
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
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "오답입니다. 다시 시도해보세요. (${guess.wrongCount}회 오답)",
                            color = Color(0xFFC0392B),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }

                    if (guess.characterCountHintShown) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "글자수 힌트: ${targetName.length}글자",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    if (guess.choseongHintShown) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "초성 힌트: ${HangulUtil.choseong(targetName)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    if (!guess.characterCountHintShown || !guess.choseongHintShown) {
                        Spacer(Modifier.height(18.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                        ) {
                            if (!guess.characterCountHintShown) {
                                TextButton(onClick = onRequestCharacterCountHint) { Text("글자수 힌트") }
                            }
                            if (!guess.choseongHintShown) {
                                TextButton(onClick = onRequestChoseongHint) { Text("초성 힌트") }
                            }
                        }
                    }
                }
            }
        }
    }
}
