package com.koreageo.quiz.quiz

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.koreageo.quiz.geo.GeoRepository
import com.koreageo.quiz.geo.Region
import com.koreageo.quiz.history.HistoryEntry
import com.koreageo.quiz.history.HistoryRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuizUiState(
    val level: MapLevel = MapLevel.National,
    val regions: List<Region> = emptyList(),
    val isLoading: Boolean = true,
    val started: Boolean = false,
    val guesses: Map<String, GuessState> = emptyMap(),
    val selectedRegionCode: String? = null,
) {
    val selectedRegion: Region? get() = regions.find { it.code == selectedRegionCode }
    val selectedGuess: GuessState get() = guesses[selectedRegionCode] ?: GuessState()
    val completed: Boolean get() = regions.isNotEmpty() && regions.all { guesses[it.code]?.revealed == true }
    val revealedCount: Int get() = regions.count { guesses[it.code]?.revealed == true }
    val canDrillDeeper: Boolean get() = level is MapLevel.National
}

class QuizViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GeoRepository(application)
    private val historyRepository = HistoryRepository(application)

    // quiz progress kept per level so it survives navigating back and forth
    private val guessesByLevel = mutableMapOf<String, Map<String, GuessState>>()
    private val startedByLevel = mutableMapOf<String, Boolean>()

    // when the current 도전 run started, per level, so a completion can compute how long it took
    private val questStartedAtByLevel = mutableMapOf<String, Long>()

    private val _lastCompletion = MutableStateFlow<HistoryEntry?>(null)
    val lastCompletion: StateFlow<HistoryEntry?> = _lastCompletion.asStateFlow()

    fun loadHistory(): List<HistoryEntry> = historyRepository.loadEntries()

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<UiEvent> = _events

    private val _settings = MutableStateFlow(QuizSettings())
    val settings: StateFlow<QuizSettings> = _settings.asStateFlow()

    fun updateSettings(newSettings: QuizSettings) {
        _settings.value = newSettings
    }

    init {
        loadNational()
    }

    private fun loadNational() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val regions = repository.loadSido()
            applyLevel(MapLevel.National, regions)
        }
    }

    fun selectProvince(region: Region) {
        val level = MapLevel.Province(code = region.code, name = region.name)
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, level = level) }
            val regions = repository.loadSigungu(region.code)
            applyLevel(level, regions)
        }
    }

    fun backToNational() {
        val current = _uiState.value
        guessesByLevel[current.level.key] = current.guesses
        startedByLevel[current.level.key] = current.started
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val regions = repository.loadSido()
            applyLevel(MapLevel.National, regions)
        }
    }

    private fun applyLevel(level: MapLevel, regions: List<Region>) {
        _uiState.update {
            it.copy(
                level = level,
                regions = regions,
                isLoading = false,
                started = startedByLevel[level.key] ?: false,
                guesses = guessesByLevel[level.key] ?: emptyMap(),
                selectedRegionCode = null,
            )
        }
    }

    fun startQuiz() {
        questStartedAtByLevel[_uiState.value.level.key] = System.currentTimeMillis()
        _uiState.update { it.copy(started = true, guesses = emptyMap(), selectedRegionCode = null) }
        persistCurrentProgress()
    }

    fun tapRegion(region: Region) {
        val state = _uiState.value
        val alreadyRevealed = state.guesses[region.code]?.revealed == true

        // 전국 화면에서는 아직 도전을 시작 안 했거나, 도전을 이미 다 끝냈을 때만 탭으로
        // 그 도/시 안으로 들어간다. 도전이 진행 중일 때는(아직 다 못 맞혔을 때는) 이미
        // 맞힌 지역이라도 진입시키지 않는다 — 안 그러면 경기도처럼 이미 맞힌 큰 지역
        // 근처를 탭할 때마다 실수로 그 안으로 들어가버려서, 서울특별시처럼 작고
        // 붙어있는 지역을 마저 맞히기가 어려워진다.
        if (state.level is MapLevel.National && (!state.started || state.completed)) {
            selectProvince(region)
            return
        }

        if (!state.started) {
            _events.tryEmit(UiEvent.DrillNotAvailableYet)
            return
        }
        if (alreadyRevealed) {
            _events.tryEmit(UiEvent.AlreadyRevealed)
            return
        }
        _uiState.update { it.copy(selectedRegionCode = region.code) }
    }

    fun dismissAnswerSheet() {
        _uiState.update { it.copy(selectedRegionCode = null) }
    }

    fun submitAnswer(input: String) {
        val state = _uiState.value
        val target = state.selectedRegion ?: return
        if (isCorrectAnswer(input, target.name)) {
            val updated = state.guesses.toMutableMap()
            updated[target.code] = (updated[target.code] ?: GuessState()).copy(revealed = true)
            _uiState.update { it.copy(guesses = updated, selectedRegionCode = null) }
            persistCurrentProgress()
            recordCompletionIfJustFinished()
        } else {
            val current = state.guesses[target.code] ?: GuessState()
            val updated = state.guesses.toMutableMap()
            updated[target.code] = current.copy(wrongCount = current.wrongCount + 1)
            _uiState.update { it.copy(guesses = updated) }
            persistCurrentProgress()
            _events.tryEmit(UiEvent.WrongAnswer)
        }
    }

    fun requestCharacterCountHint() {
        val state = _uiState.value
        val target = state.selectedRegion ?: return
        val current = state.guesses[target.code] ?: return
        if (!current.characterCountHintAvailable(_settings.value)) return
        val updated = state.guesses.toMutableMap()
        updated[target.code] = current.copy(characterCountHintShown = true)
        _uiState.update { it.copy(guesses = updated) }
        persistCurrentProgress()
    }

    fun requestChoseongHint() {
        val state = _uiState.value
        val target = state.selectedRegion ?: return
        val current = state.guesses[target.code] ?: return
        if (!current.choseongHintAvailable(_settings.value)) return
        val updated = state.guesses.toMutableMap()
        updated[target.code] = current.copy(choseongHintShown = true)
        _uiState.update { it.copy(guesses = updated) }
        persistCurrentProgress()
    }

    /** Ends the current run early (the "여기까지" button) and logs the partial result to history. */
    fun stopHere() {
        val state = _uiState.value
        val startedAt = questStartedAtByLevel.remove(state.level.key)
        if (startedAt != null) {
            val entry = HistoryEntry(
                levelName = state.level.displayName(),
                completedAtMillis = System.currentTimeMillis(),
                durationMillis = (System.currentTimeMillis() - startedAt).coerceAtLeast(0),
                revealedCount = state.revealedCount,
                totalCount = state.regions.size,
            )
            historyRepository.addEntry(entry)
            // No _lastCompletion update here — that would wrongly fire the completion
            // celebration/confetti for a run that wasn't actually finished.
        }
        _uiState.update { it.copy(started = false, guesses = emptyMap(), selectedRegionCode = null) }
        persistCurrentProgress()
    }

    private fun persistCurrentProgress() {
        val state = _uiState.value
        guessesByLevel[state.level.key] = state.guesses
        startedByLevel[state.level.key] = state.started
    }

    private fun recordCompletionIfJustFinished() {
        val state = _uiState.value
        if (!state.completed) return
        val startedAt = questStartedAtByLevel.remove(state.level.key) ?: return
        val entry = HistoryEntry(
            levelName = state.level.displayName(),
            completedAtMillis = System.currentTimeMillis(),
            durationMillis = (System.currentTimeMillis() - startedAt).coerceAtLeast(0),
            revealedCount = state.revealedCount,
            totalCount = state.regions.size,
        )
        historyRepository.addEntry(entry)
        _lastCompletion.value = entry
    }
}
