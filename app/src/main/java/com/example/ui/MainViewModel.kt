package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.EvolutionSummary
import com.example.data.HabitCatalog
import com.example.data.HistoryEntry
import com.example.data.local.AppDatabase
import com.example.data.local.DriftPreferencesDataStore
import com.example.data.local.DriftUserPrefs
import com.example.data.repository.DisciplineRepository
import com.example.drift.CarStyle
import com.example.drift.DriftRaceStatus
import com.example.drift.GhostFrame
import com.example.drift.TrackId
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MainTab {
    EXAM,
    HISTORY,
    DRIFT,
    ROYALE
}

enum class ExamStage {
    WELCOME,
    QUESTION,
    ANALYZING,
    RESULTS
}

data class ExamUiState(
    val stage: ExamStage = ExamStage.WELCOME,
    val currentQuestionIndex: Int = 0,
    val answers: Map<Int, String> = emptyMap(),
    val latestResult: HistoryEntry? = null,
    val latestDelta: Int? = null
)

data class DriftSessionState(
    val status: DriftRaceStatus = DriftRaceStatus.LOBBY,
    val isCompetitiveMode: Boolean = false,
    val countdownNumber: Int? = null,
    val competitiveSecondsLeft: Int = 60,
    val isGhostActive: Boolean = false,
    val ghostReplay: List<GhostFrame> = emptyList(),
    val pendingGhostReplay: List<GhostFrame> = emptyList(),
    val lastWinnerScore: Int = 0,
    val cheatError: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: DisciplineRepository = DisciplineRepository(
        historyDao = AppDatabase.getInstance(application).historyDao(),
        driftPrefs = DriftPreferencesDataStore(application)
    )

    private val _activeTab = MutableStateFlow(MainTab.EXAM)
    val activeTab: StateFlow<MainTab> = _activeTab.asStateFlow()

    private val _showGameCover = MutableStateFlow(true)
    val showGameCover: StateFlow<Boolean> = _showGameCover.asStateFlow()

    val history: StateFlow<List<HistoryEntry>> = repository.historyStream
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val evolutionSummary: StateFlow<EvolutionSummary> = history
        .map { HabitCatalog.computeEvolutionSummary(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EvolutionSummary())

    val driftPrefs: StateFlow<DriftUserPrefs> = repository.driftPrefsStream
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DriftUserPrefs())

    private val _examUi = MutableStateFlow(ExamUiState())
    val examUi: StateFlow<ExamUiState> = _examUi.asStateFlow()

    private val _driftSession = MutableStateFlow(DriftSessionState())
    val driftSession: StateFlow<DriftSessionState> = _driftSession.asStateFlow()

    private var raceTimerJob: Job? = null
    private var advanceQuestionJob: Job? = null

    fun selectTab(tab: MainTab) {
        _activeTab.value = tab
    }

    fun dismissGameCover() {
        _showGameCover.value = false
    }

    fun startExam() {
        advanceQuestionJob?.cancel()
        _activeTab.value = MainTab.EXAM
        _examUi.value = ExamUiState(
            stage = ExamStage.QUESTION,
            currentQuestionIndex = 0,
            answers = emptyMap(),
            latestResult = null,
            latestDelta = null
        )
    }

    fun resetExamToWelcome() {
        advanceQuestionJob?.cancel()
        _activeTab.value = MainTab.EXAM
        _examUi.value = ExamUiState(stage = ExamStage.WELCOME)
    }

    fun selectOption(questionId: Int, optionId: String) {
        val current = _examUi.value
        if (current.stage != ExamStage.QUESTION) return
        val updatedAnswers = current.answers.toMutableMap().apply { put(questionId, optionId) }
        _examUi.value = current.copy(answers = updatedAnswers)

        advanceQuestionJob?.cancel()
        advanceQuestionJob = viewModelScope.launch {
            delay(260)
            val questions = HabitCatalog.questions
            if (current.currentQuestionIndex < questions.lastIndex) {
                _examUi.value = _examUi.value.copy(
                    currentQuestionIndex = current.currentQuestionIndex + 1
                )
            } else {
                finalizeExam(updatedAnswers)
            }
        }
    }

    fun previousQuestion() {
        advanceQuestionJob?.cancel()
        val current = _examUi.value
        if (current.stage == ExamStage.QUESTION && current.currentQuestionIndex > 0) {
            _examUi.value = current.copy(currentQuestionIndex = current.currentQuestionIndex - 1)
        }
    }

    private fun finalizeExam(finalAnswers: Map<Int, String>) {
        viewModelScope.launch {
            _examUi.value = _examUi.value.copy(stage = ExamStage.ANALYZING)
            val prevLatest = history.value.lastOrNull()
            delay(650)
            val saved = repository.saveExamAnswers(finalAnswers)
            val delta = prevLatest?.let { saved.score - it.score }
            _examUi.value = _examUi.value.copy(
                stage = ExamStage.RESULTS,
                latestResult = saved,
                latestDelta = delta
            )
        }
    }

    fun deleteHistoryEntry(id: String) {
        viewModelScope.launch {
            repository.deleteEntry(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
        }
    }

    // --- Drift Masters 3D Controls ---

    fun updatePilotName(name: String) {
        viewModelScope.launch { repository.updatePilotName(name) }
    }

    fun updateCarColor(hex: String) {
        viewModelScope.launch { repository.updateCarColor(hex) }
    }

    fun updateCarStyle(style: CarStyle) {
        viewModelScope.launch { repository.updateCarStyle(style) }
    }

    fun updateSelectedTrack(track: TrackId) {
        viewModelScope.launch { repository.updateSelectedTrack(track) }
    }

    fun updateBotCount(count: Int) {
        viewModelScope.launch { repository.updateBotCount(count) }
    }

    fun submitCheatCode(code: String) {
        val cleaned = code.trim().lowercase()
        if (cleaned == "fun-kyon") {
            viewModelScope.launch {
                repository.unlockCheat()
                _driftSession.value = _driftSession.value.copy(cheatError = null)
            }
        } else if (cleaned.isNotEmpty()) {
            _driftSession.value = _driftSession.value.copy(cheatError = "Código inválido")
        }
    }

    fun clearCheatError() {
        if (_driftSession.value.cheatError != null) {
            _driftSession.value = _driftSession.value.copy(cheatError = null)
        }
    }

    fun startDriftRace(isCompetitive: Boolean) {
        startRaceInternal(
            isCompetitive = isCompetitive,
            ghostActive = false,
            ghostFrames = emptyList()
        )
    }

    fun startGhostMatch() {
        val pending = _driftSession.value.pendingGhostReplay
        if (pending.isEmpty()) return
        startRaceInternal(
            isCompetitive = true,
            ghostActive = true,
            ghostFrames = pending
        )
    }

    private fun startRaceInternal(
        isCompetitive: Boolean,
        ghostActive: Boolean,
        ghostFrames: List<GhostFrame>
    ) {
        raceTimerJob?.cancel()
        _driftSession.value = _driftSession.value.copy(
            status = DriftRaceStatus.COUNTDOWN,
            isCompetitiveMode = isCompetitive,
            countdownNumber = 3,
            competitiveSecondsLeft = 60,
            isGhostActive = ghostActive,
            ghostReplay = ghostFrames
        )

        raceTimerJob = viewModelScope.launch {
            delay(1000)
            _driftSession.value = _driftSession.value.copy(countdownNumber = 2)
            delay(1000)
            _driftSession.value = _driftSession.value.copy(countdownNumber = 1)
            delay(1000)
            _driftSession.value = _driftSession.value.copy(
                status = DriftRaceStatus.RACING,
                countdownNumber = 0
            )
            launch {
                delay(900)
                if (_driftSession.value.countdownNumber == 0) {
                    _driftSession.value = _driftSession.value.copy(countdownNumber = null)
                }
            }
            if (isCompetitive) {
                var seconds = 60
                while (seconds > 0 && _driftSession.value.status == DriftRaceStatus.RACING) {
                    delay(1000)
                    seconds -= 1
                    _driftSession.value = _driftSession.value.copy(competitiveSecondsLeft = seconds)
                }
            }
        }
    }

    fun finishDriftRace(finalScore: Int, recordedFrames: List<GhostFrame>) {
        raceTimerJob?.cancel()
        viewModelScope.launch {
            repository.recordBestDriftScore(finalScore)
        }
        _driftSession.value = _driftSession.value.copy(
            status = DriftRaceStatus.FINISHED,
            countdownNumber = null,
            lastWinnerScore = finalScore,
            pendingGhostReplay = if (recordedFrames.isNotEmpty()) recordedFrames else _driftSession.value.pendingGhostReplay
        )
    }

    fun leaveDriftToLobby() {
        raceTimerJob?.cancel()
        _driftSession.value = _driftSession.value.copy(
            status = DriftRaceStatus.LOBBY,
            countdownNumber = null
        )
    }
}
