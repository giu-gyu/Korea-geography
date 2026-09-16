package com.koreageo.quiz.ui.map

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.koreageo.quiz.quiz.MapLevel
import com.koreageo.quiz.quiz.QuizViewModel
import com.koreageo.quiz.quiz.UiEvent
import com.koreageo.quiz.quiz.displayName
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(viewModel: QuizViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val lastCompletion by viewModel.lastCompletion.collectAsState()
    val camera = remember { CameraState() }
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var hasPlayedIntro by rememberSaveable { mutableStateOf(false) }
    var showCompletionDialog by remember(uiState.level.key) { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var fitScale by remember { mutableStateOf(1f) }
    var remainingMessage by remember { mutableStateOf<String?>(null) }
    var nextBlankCursor by remember(uiState.level.key) { mutableStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val goToNextBlank: () -> Unit = {
        val unrevealed = uiState.regions.filter { uiState.guesses[it.code]?.revealed != true }
        if (unrevealed.isNotEmpty()) {
            val target = unrevealed[nextBlankCursor % unrevealed.size]
            nextBlankCursor++
            viewModel.tapRegion(target)
            scope.launch { camera.animateTo(focusTransform(target, canvasSize, fitScale)) }
        }
    }

    // 도(道) 화면에서 시스템 뒤로가기를 누르면 앱을 나가지 말고 전국 화면으로 이동한다.
    BackHandler(enabled = uiState.level is MapLevel.Province) {
        viewModel.backToNational()
    }

    LaunchedEffect(uiState.level.key, uiState.regions, canvasSize) {
        if (uiState.regions.isEmpty() || canvasSize == Size.Zero) return@LaunchedEffect
        val bounds = unionBounds(uiState.regions)
        val target = fitTransform(bounds, canvasSize)
        fitScale = target.scale
        if (uiState.level is MapLevel.National && !hasPlayedIntro) {
            hasPlayedIntro = true
            val startBounds = zoomedBounds(bounds, factor = 0.12f, verticalAnchor = 0.5f)
            camera.snapToImmediate(fitTransform(startBounds, canvasSize, paddingFraction = 0f))
            delay(200)
            camera.animateTo(target, tween(1100, easing = FastOutSlowInEasing))
        } else {
            camera.animateTo(target)
        }
    }

    // 전체 완료든, "여기까지"로 1개 이상 맞히고 조기 종료든, lastCompletion이 새로
    // 갱신될 때마다 결과 다이얼로그를 띄운다.
    LaunchedEffect(lastCompletion?.completedAtMillis) {
        if (lastCompletion != null) showCompletionDialog = true
    }

    // 얼마 안 남았을 때(3개 이하) 화면 중앙 위쪽에 살짝 알려준다.
    LaunchedEffect(uiState.revealedCount, uiState.started, uiState.level.key) {
        if (!uiState.started) return@LaunchedEffect
        val remaining = uiState.regions.size - uiState.revealedCount
        if (remaining in 1..3) {
            remainingMessage = "${remaining}개 남았습니다"
            delay(1500)
            remainingMessage = null
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val message = when (event) {
                UiEvent.WrongAnswer -> null
                UiEvent.AlreadyRevealed -> "이미 맞춘 지역입니다."
                UiEvent.DrillNotAvailableYet -> "이 단계는 아직 준비 중입니다."
            }
            if (message != null) snackbarHostState.showSnackbar(message)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text(uiState.level.displayName()) },
                    navigationIcon = {
                        if (uiState.level is MapLevel.Province) {
                            IconButton(onClick = viewModel::backToNational) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "전국 지도로")
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { showHistoryDialog = true }) {
                            Icon(Icons.Filled.History, contentDescription = "기록")
                        }
                        // 활성화(체크) 상태 = 지역 이름을 숨김. 도전이 진행 중일 때는 이 토글이
                        // 아무 효과가 없으므로(빈칸/정답이 대신 표시 여부를 결정) 비활성화해서
                        // 지금은 켤 수 없다는 걸 보여준다.
                        IconToggleButton(
                            checked = !settings.showLabelsInBrowseMode,
                            onCheckedChange = { hide -> viewModel.updateSettings(settings.copy(showLabelsInBrowseMode = !hide)) },
                            enabled = !uiState.started,
                        ) {
                            Icon(
                                if (settings.showLabelsInBrowseMode) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = if (settings.showLabelsInBrowseMode) "지역 이름 숨기기" else "지역 이름 보이기",
                            )
                        }
                    },
                )
            },
            bottomBar = {
                Surface(tonalElevation = 3.dp) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                    ) {
                        if (!uiState.started) {
                            ChallengeButton(onClick = viewModel::startQuiz, modifier = Modifier.fillMaxWidth())
                        } else {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "${uiState.revealedCount} / ${uiState.regions.size} 완료",
                                    modifier = Modifier.align(Alignment.CenterStart),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                OutlinedButton(
                                    onClick = viewModel::stopHere,
                                    modifier = Modifier.align(Alignment.CenterEnd),
                                ) { Text("여기까지") }
                            }
                        }
                    }
                }
            },
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                MapCanvas(
                    regions = uiState.regions,
                    guesses = uiState.guesses,
                    started = uiState.started,
                    selectedRegionCode = uiState.selectedRegionCode,
                    camera = camera,
                    fitScale = fitScale,
                    baseLabelSp = if (uiState.level is MapLevel.National) 9f else 13f,
                    isNationalLevel = uiState.level is MapLevel.National,
                    showLabelsInBrowseMode = settings.showLabelsInBrowseMode,
                    onCanvasSizeChanged = { canvasSize = it },
                    onTapRegion = viewModel::tapRegion,
                    modifier = Modifier.fillMaxSize(),
                )

                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                if (uiState.started) {
                    NextBlankButton(
                        onClick = goToNextBlank,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp),
                    )
                }
            }
        }

        ConfettiOverlay(trigger = lastCompletion?.completedAtMillis, modifier = Modifier.fillMaxSize())

        RemainingBanner(
            message = remainingMessage,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 110.dp),
        )
    }

    uiState.selectedRegion?.let { region ->
        AnswerSheet(
            guess = uiState.selectedGuess,
            targetName = region.name,
            onSubmit = viewModel::submitAnswer,
            onDismiss = viewModel::dismissAnswerSheet,
            onRequestCharacterCountHint = viewModel::requestCharacterCountHint,
            onRequestChoseongHint = viewModel::requestChoseongHint,
        )
    }

    if (showHistoryDialog) {
        HistoryDialog(
            entries = remember(showHistoryDialog) { viewModel.loadHistory() },
            onDeleteEntry = viewModel::deleteHistoryEntry,
            onDismiss = { showHistoryDialog = false },
        )
    }

    val completionToShow = lastCompletion
    if (showCompletionDialog && completionToShow != null) {
        CompletionDialog(
            levelTitle = completionToShow.levelName,
            revealedCount = completionToShow.revealedCount,
            totalCount = completionToShow.totalCount,
            durationMillis = completionToShow.durationMillis,
            showBackToNational = uiState.level is MapLevel.Province,
            onRestart = {
                showCompletionDialog = false
                viewModel.startQuiz()
            },
            onBackToNational = {
                showCompletionDialog = false
                viewModel.backToNational()
            },
            onDismiss = { showCompletionDialog = false },
        )
    }
}

@Composable
private fun NextBlankButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilledTonalButton(onClick = onClick, modifier = modifier) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text("다음 빈칸")
    }
}
