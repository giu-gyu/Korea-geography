package com.koreageo.quiz.ui.map

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(viewModel: QuizViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val camera = remember { CameraState() }
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var hasPlayedIntro by rememberSaveable { mutableStateOf(false) }
    var showCompletionDialog by remember(uiState.level.key) { mutableStateOf(false) }
    var fitScale by remember { mutableStateOf(1f) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.level.key, uiState.regions, canvasSize) {
        if (uiState.regions.isEmpty() || canvasSize == Size.Zero) return@LaunchedEffect
        val bounds = unionBounds(uiState.regions)
        val target = fitTransform(bounds, canvasSize)
        fitScale = target.scale
        if (uiState.level is MapLevel.National && !hasPlayedIntro) {
            hasPlayedIntro = true
            val startBounds = zoomedBounds(bounds, factor = 0.12f, verticalAnchor = 0.9f)
            camera.snapToImmediate(fitTransform(startBounds, canvasSize, paddingFraction = 0f))
            delay(200)
            camera.animateTo(target, tween(1100, easing = FastOutSlowInEasing))
        } else {
            camera.animateTo(target)
        }
    }

    LaunchedEffect(uiState.completed) {
        if (uiState.completed) showCompletionDialog = true
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(breadcrumb(uiState.level)) },
                navigationIcon = {
                    if (uiState.level is MapLevel.Province) {
                        IconButton(onClick = viewModel::backToNational) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "전국 지도로")
                        }
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
                        Button(onClick = viewModel::startQuiz, modifier = Modifier.fillMaxWidth()) {
                            Text("시작")
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "${uiState.revealedCount} / ${uiState.regions.size} 완료",
                                modifier = Modifier.align(Alignment.CenterStart),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            OutlinedButton(
                                onClick = viewModel::startQuiz,
                                modifier = Modifier.align(Alignment.CenterEnd),
                            ) { Text("다시 시작") }
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
                onCanvasSizeChanged = { canvasSize = it },
                onTapRegion = viewModel::tapRegion,
                modifier = Modifier.fillMaxSize(),
            )

            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }

    uiState.selectedRegion?.let { region ->
        AnswerSheet(
            guess = uiState.selectedGuess,
            targetName = region.name,
            onSubmit = viewModel::submitAnswer,
            onDismiss = viewModel::dismissAnswerSheet,
            onRequestHint = viewModel::requestHint,
        )
    }

    if (showCompletionDialog) {
        CompletionDialog(
            levelTitle = breadcrumb(uiState.level),
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

private fun breadcrumb(level: MapLevel): String = when (level) {
    is MapLevel.National -> "대한민국 (광역자치단체)"
    is MapLevel.Province -> level.name
}
