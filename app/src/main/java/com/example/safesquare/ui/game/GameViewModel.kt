package com.example.safesquare.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safesquare.game.MoveOutcome
import com.example.safesquare.game.Player
import com.example.safesquare.game.SafeSquareGameState
import com.example.safesquare.game.SafeSquareRules
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class BannerType(val text: String) {
    SAFE("SAFE"),
    CAPTURE("CAPTURE"),
    HOME("HOME"),
    NO_LEGAL_MOVE("NO LEGAL MOVE")
}

data class Banner(val type: BannerType, val key: Long)

data class SafePulse(val nodeId: Int, val key: Long)

enum class GameEvent { ROLL, STEP, SAFE, CAPTURE, HOME, WIN }

data class GameUiState(
    val game: SafeSquareGameState = SafeSquareRules.initialState(),
    /** Temporary route-progress overrides for tokens mid-animation. */
    val displayProgress: Map<Int, Int> = emptyMap(),
    val movingTokenId: Int? = null,
    val capturedFlashId: Int? = null,
    val isRolling: Boolean = false,
    val rollingFace: Int? = null,
    val lastDice: Int? = null,
    /** True when the current player keeps the turn after a 6. */
    val extraRoll: Boolean = false,
    val banner: Banner? = null,
    val safePulse: SafePulse? = null,
    val durationMillis: Long = 0L
) {
    /** Input is accepted only when the engine is idle. */
    val canRoll: Boolean
        get() = game.winner == null && game.waitingForRoll && !game.isAnimating && !isRolling

    val canMove: Boolean
        get() = game.winner == null && !game.waitingForRoll && !game.isAnimating && !isRolling
}

class GameViewModel(
    private val onGameFinished: suspend (winner: Player, mostCapturesInGame: Int) -> Unit,
    private val random: Random = Random.Default,
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {

    private val _ui = MutableStateFlow(GameUiState())
    val ui: StateFlow<GameUiState> = _ui.asStateFlow()

    private val _events = MutableSharedFlow<GameEvent>(extraBufferCapacity = 32)
    val events: SharedFlow<GameEvent> = _events.asSharedFlow()

    private var actionJob: Job? = null
    private var bannerJob: Job? = null
    private var startedAt = clock()
    private var eventKey = 0L
    private var fastMovement = false

    private val stepMillis: Long
        get() = if (fastMovement) FAST_STEP_MS else NORMAL_STEP_MS

    fun setFastMovement(enabled: Boolean) {
        fastMovement = enabled
    }

    fun stepDurationMillis(): Int = stepMillis.toInt()

    fun newGame() {
        actionJob?.cancel()
        bannerJob?.cancel()
        startedAt = clock()
        _ui.value = GameUiState()
    }

    fun roll() {
        if (!_ui.value.canRoll) return
        _ui.update { it.copy(isRolling = true, extraRoll = false, game = it.game.copy(isAnimating = true)) }
        _events.tryEmit(GameEvent.ROLL)

        actionJob = viewModelScope.launch {
            val frames = ROLL_ANIMATION_MS / ROLL_FRAME_MS
            repeat(frames.toInt()) {
                _ui.update { it.copy(rollingFace = random.nextInt(1, 7)) }
                delay(ROLL_FRAME_MS)
            }
            val value = random.nextInt(1, 7)
            val rolled = SafeSquareRules.roll(_ui.value.game.copy(isAnimating = false), value)
            if (rolled == null) {
                _ui.update { it.copy(isRolling = false, rollingFace = null, game = it.game.copy(isAnimating = false)) }
                return@launch
            }

            if (!rolled.hasNoLegalMove) {
                _ui.update { it.copy(game = rolled, isRolling = false, rollingFace = null, lastDice = value) }
                return@launch
            }

            // Nothing can move: show the result briefly, then resolve the turn.
            _ui.update {
                it.copy(game = rolled.copy(isAnimating = true), isRolling = false, rollingFace = null, lastDice = value)
            }
            showBanner(BannerType.NO_LEGAL_MOVE)
            delay(NO_MOVE_PAUSE_MS)
            val passed = SafeSquareRules.passTurn(rolled) ?: rolled
            _ui.update { it.copy(game = passed, extraRoll = passed.currentPlayer == rolled.currentPlayer) }
        }
    }

    fun moveToken(tokenId: Int) {
        val current = _ui.value
        if (!current.canMove) return
        val outcome = SafeSquareRules.applyMove(current.game, tokenId) ?: return

        _ui.update {
            it.copy(
                game = it.game.copy(isAnimating = true, legalTokenIds = emptySet()),
                movingTokenId = tokenId
            )
        }
        actionJob = viewModelScope.launch { animateMove(outcome) }
    }

    private suspend fun animateMove(outcome: MoveOutcome) {
        val step = stepMillis
        for (progress in outcome.path) {
            _ui.update { it.copy(displayProgress = mapOf(outcome.tokenId to progress)) }
            _events.tryEmit(GameEvent.STEP)
            delay(step)
        }

        if (outcome.capturedTokenId != null) {
            _ui.update { it.copy(capturedFlashId = outcome.capturedTokenId) }
            showBanner(BannerType.CAPTURE)
            _events.tryEmit(GameEvent.CAPTURE)
            delay(CAPTURE_FLASH_MS)
        }

        val next = outcome.state
        val finishedAt = if (next.winner != null) clock() else null
        _ui.update {
            it.copy(
                game = next.copy(isAnimating = true),
                displayProgress = emptyMap(),
                movingTokenId = null,
                capturedFlashId = null,
                extraRoll = outcome.extraTurn,
                durationMillis = finishedAt?.let { end -> end - startedAt } ?: it.durationMillis
            )
        }

        if (outcome.safeLanding) {
            val tokenAfter = next.tokens.first { it.id == outcome.tokenId }
            SafeSquareRules.getSharedTrackIndex(tokenAfter)?.let { node ->
                _ui.update { it.copy(safePulse = SafePulse(node, ++eventKey)) }
            }
            showBanner(BannerType.SAFE)
            _events.tryEmit(GameEvent.SAFE)
        }
        if (outcome.finished) {
            showBanner(BannerType.HOME)
            _events.tryEmit(GameEvent.HOME)
        }
        if (outcome.capturedTokenId != null) {
            // Let the captured token glide back to START before play continues.
            delay(CAPTURE_RETURN_MS)
        }

        val winner = outcome.winner
        if (winner != null) {
            _events.tryEmit(GameEvent.WIN)
            onGameFinished(winner, maxOf(next.player1Captures, next.player2Captures))
        }
        _ui.update { it.copy(game = it.game.copy(isAnimating = false)) }
    }

    private fun showBanner(type: BannerType) {
        val banner = Banner(type, ++eventKey)
        _ui.update { it.copy(banner = banner) }
        bannerJob?.cancel()
        bannerJob = viewModelScope.launch {
            delay(BANNER_MS)
            _ui.update { if (it.banner?.key == banner.key) it.copy(banner = null) else it }
        }
    }

    companion object {
        const val NORMAL_STEP_MS = 100L
        const val FAST_STEP_MS = 60L
        const val ROLL_ANIMATION_MS = 330L
        const val ROLL_FRAME_MS = 55L
        const val NO_MOVE_PAUSE_MS = 950L
        const val CAPTURE_FLASH_MS = 320L
        const val CAPTURE_RETURN_MS = 420L
        const val BANNER_MS = 900L
    }
}
