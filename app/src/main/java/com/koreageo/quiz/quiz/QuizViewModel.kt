package com.koreageo.quiz.quiz

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.koreageo.quiz.geo.GeoRepository
import com.koreageo.quiz.geo.Region
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

    // quiz progress kept per level so it survives navigating back and forth
    private val guessesByLevel = mutableMapOf<String, Map<String, GuessState>>()
    private val startedByLevel = mutableMapOf<String, Boolean>()

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
        _uiState.update { it.copy(started = true, guesses = emptyMap(), selectedRegionCode = null) }
        persistCurrentProgress()
    }

    fun tapRegion(region: Region) {
        val state = _uiState.value
        if (!state.started) {
            if (state.level is MapLevel.National) {
                selectProvince(region)
            } else {
                _events.tryEmit(UiEvent.DrillNotAvailableYet)
            }
            return
        }
        if (state.guesses[region.code]?.revealed == true) {
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

    private fun persistCurrentProgress() {
        val state = _uiState.value
        guessesByLevel[state.level.key] = state.guesses
        startedByLevel[state.level.key] = state.started
    }
}
