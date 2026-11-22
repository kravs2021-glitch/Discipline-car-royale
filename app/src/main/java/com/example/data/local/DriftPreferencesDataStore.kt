package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.drift.CarStyle
import com.example.drift.TrackId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.driftDataStore by preferencesDataStore(name = "drift_masters_prefs")

data class DriftUserPrefs(
    val pilotName: String = "Piloto 1",
    val carColor: String = "#E62424",
    val carStyle: CarStyle = CarStyle.COUPE,
    val selectedTrack: TrackId = TrackId.JAPAN,
    val botCount: Int = 1,
    val isCheatUnlocked: Boolean = false,
    val bestDriftScore: Int = 0
)

class DriftPreferencesDataStore(private val context: Context) {

    private object Keys {
        val PILOT_NAME = stringPreferencesKey("pilot_name")
        val CAR_COLOR = stringPreferencesKey("car_color")
        val CAR_STYLE = stringPreferencesKey("car_style")
        val SELECTED_TRACK = stringPreferencesKey("selected_track")
        val BOT_COUNT = intPreferencesKey("bot_count")
        val CHEAT_UNLOCKED = booleanPreferencesKey("isCheatUnlocked_fun-kyon")
        val BEST_SCORE = intPreferencesKey("best_drift_score")
    }

    val prefsFlow: Flow<DriftUserPrefs> = context.driftDataStore.data.map { prefs ->
        val unlocked = prefs[Keys.CHEAT_UNLOCKED] ?: false
        val rawStyle = CarStyle.entries.firstOrNull { it.name == prefs[Keys.CAR_STYLE] } ?: CarStyle.COUPE
        val rawTrack = TrackId.entries.firstOrNull { it.name == prefs[Keys.SELECTED_TRACK] } ?: TrackId.JAPAN
        DriftUserPrefs(
            pilotName = prefs[Keys.PILOT_NAME] ?: "Piloto 1",
            carColor = prefs[Keys.CAR_COLOR] ?: "#E62424",
            carStyle = if (!unlocked && rawStyle == CarStyle.PLANE) CarStyle.COUPE else rawStyle,
            selectedTrack = if (!unlocked && rawTrack == TrackId.AIRPORT) TrackId.JAPAN else rawTrack,
            botCount = (prefs[Keys.BOT_COUNT] ?: 1).coerceIn(0, 3),
            isCheatUnlocked = unlocked,
            bestDriftScore = prefs[Keys.BEST_SCORE] ?: 0
        )
    }

    suspend fun updatePilotName(name: String) {
        context.driftDataStore.edit { it[Keys.PILOT_NAME] = name.take(18) }
    }

    suspend fun updateCarColor(hex: String) {
        context.driftDataStore.edit { it[Keys.CAR_COLOR] = hex }
    }

    suspend fun updateCarStyle(style: CarStyle) {
        context.driftDataStore.edit { it[Keys.CAR_STYLE] = style.name }
    }

    suspend fun updateSelectedTrack(track: TrackId) {
        context.driftDataStore.edit { it[Keys.SELECTED_TRACK] = track.name }
    }

    suspend fun updateBotCount(count: Int) {
        context.driftDataStore.edit { it[Keys.BOT_COUNT] = count.coerceIn(0, 3) }
    }

    suspend fun unlockCheat() {
        context.driftDataStore.edit { it[Keys.CHEAT_UNLOCKED] = true }
    }

    suspend fun recordBestScoreIfHigher(score: Int) {
        context.driftDataStore.edit { prefs ->
            val current = prefs[Keys.BEST_SCORE] ?: 0
            if (score > current) {
                prefs[Keys.BEST_SCORE] = score
            }
        }
    }
}
