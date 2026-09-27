package com.example.safesquare.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.safesquare.game.Player
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class BoardTheme { CLASSIC, MIDNIGHT }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val boardTheme: BoardTheme = BoardTheme.CLASSIC,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val showLegalMoves: Boolean = true,
    val highlightSafeSquares: Boolean = true,
    val fastMovement: Boolean = false
)

data class GameStats(
    val gamesPlayed: Int = 0,
    val playerOneWins: Int = 0,
    val playerTwoWins: Int = 0,
    val mostCaptures: Int = 0
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "safe_square")

class SettingsRepository(context: Context) {
    private val store = context.applicationContext.dataStore

    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val boardTheme = stringPreferencesKey("board_theme")
        val sound = booleanPreferencesKey("sound")
        val haptics = booleanPreferencesKey("haptics")
        val showLegal = booleanPreferencesKey("show_legal_moves")
        val highlightSafe = booleanPreferencesKey("highlight_safe")
        val fastMovement = booleanPreferencesKey("fast_movement")
        val gamesPlayed = intPreferencesKey("games_played")
        val p1Wins = intPreferencesKey("p1_wins")
        val p2Wins = intPreferencesKey("p2_wins")
        val mostCaptures = intPreferencesKey("most_captures")
    }

    val settings: Flow<AppSettings> = store.data.map { p ->
        AppSettings(
            themeMode = enumOrDefault(p[Keys.themeMode], ThemeMode.SYSTEM),
            boardTheme = enumOrDefault(p[Keys.boardTheme], BoardTheme.CLASSIC),
            soundEnabled = p[Keys.sound] ?: true,
            hapticsEnabled = p[Keys.haptics] ?: true,
            showLegalMoves = p[Keys.showLegal] ?: true,
            highlightSafeSquares = p[Keys.highlightSafe] ?: true,
            fastMovement = p[Keys.fastMovement] ?: false
        )
    }.distinctUntilChanged()

    val stats: Flow<GameStats> = store.data.map { p ->
        GameStats(
            gamesPlayed = p[Keys.gamesPlayed] ?: 0,
            playerOneWins = p[Keys.p1Wins] ?: 0,
            playerTwoWins = p[Keys.p2Wins] ?: 0,
            mostCaptures = p[Keys.mostCaptures] ?: 0
        )
    }.distinctUntilChanged()

    suspend fun setThemeMode(mode: ThemeMode) = store.edit { it[Keys.themeMode] = mode.name }
    suspend fun setBoardTheme(theme: BoardTheme) = store.edit { it[Keys.boardTheme] = theme.name }
    suspend fun setSound(enabled: Boolean) = store.edit { it[Keys.sound] = enabled }
    suspend fun setHaptics(enabled: Boolean) = store.edit { it[Keys.haptics] = enabled }
    suspend fun setShowLegalMoves(enabled: Boolean) = store.edit { it[Keys.showLegal] = enabled }
    suspend fun setHighlightSafeSquares(enabled: Boolean) = store.edit { it[Keys.highlightSafe] = enabled }
    suspend fun setFastMovement(enabled: Boolean) = store.edit { it[Keys.fastMovement] = enabled }

    suspend fun recordGame(winner: Player, capturesThisGame: Int) {
        store.edit { p ->
            p[Keys.gamesPlayed] = (p[Keys.gamesPlayed] ?: 0) + 1
            val winKey = if (winner == Player.PLAYER_ONE) Keys.p1Wins else Keys.p2Wins
            p[winKey] = (p[winKey] ?: 0) + 1
            p[Keys.mostCaptures] = maxOf(p[Keys.mostCaptures] ?: 0, capturesThisGame)
        }
    }

    suspend fun resetStats() {
        store.edit { p ->
            p.remove(Keys.gamesPlayed)
            p.remove(Keys.p1Wins)
            p.remove(Keys.p2Wins)
            p.remove(Keys.mostCaptures)
        }
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
        name?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default
}
