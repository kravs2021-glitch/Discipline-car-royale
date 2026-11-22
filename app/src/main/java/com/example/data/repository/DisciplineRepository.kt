package com.example.data.repository

import com.example.data.ExamStatus
import com.example.data.HabitCatalog
import com.example.data.HistoryEntry
import com.example.data.local.DriftPreferencesDataStore
import com.example.data.local.DriftUserPrefs
import com.example.data.local.HistoryDao
import com.example.data.local.HistoryEntity
import com.example.drift.CarStyle
import com.example.drift.TrackId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class DisciplineRepository(
    private val historyDao: HistoryDao,
    private val driftPrefs: DriftPreferencesDataStore
) {
    val historyStream: Flow<List<HistoryEntry>> = historyDao.observeAllChronological()
        .map { entities -> entities.map { it.toDomain() } }

    val driftPrefsStream: Flow<DriftUserPrefs> = driftPrefs.prefsFlow

    suspend fun saveExamAnswers(answers: Map<Int, String>): HistoryEntry {
        val score = HabitCatalog.calculateScoreFromAnswers(answers, HabitCatalog.questions.size)
        val entry = HistoryEntry(
            id = UUID.randomUUID().toString(),
            epochMillis = System.currentTimeMillis(),
            score = score,
            status = if (score >= 60) ExamStatus.MOGGER else ExamStatus.MOGGED,
            answers = answers.toMap()
        )
        historyDao.insert(HistoryEntity.fromDomain(entry))
        return entry
    }

    suspend fun deleteEntry(id: String) = historyDao.deleteById(id)

    suspend fun clearAllHistory() = historyDao.clearAll()

    suspend fun updatePilotName(name: String) = driftPrefs.updatePilotName(name)

    suspend fun updateCarColor(hex: String) = driftPrefs.updateCarColor(hex)

    suspend fun updateCarStyle(style: CarStyle) = driftPrefs.updateCarStyle(style)

    suspend fun updateSelectedTrack(track: TrackId) = driftPrefs.updateSelectedTrack(track)

    suspend fun updateBotCount(count: Int) = driftPrefs.updateBotCount(count)

    suspend fun unlockCheat() = driftPrefs.unlockCheat()

    suspend fun recordBestDriftScore(score: Int) = driftPrefs.recordBestScoreIfHigher(score)
}
