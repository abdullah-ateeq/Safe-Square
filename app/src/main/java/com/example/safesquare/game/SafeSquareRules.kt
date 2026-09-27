package com.example.safesquare.game

/**
 * Pure rules for Safe Square. No Android or UI dependencies: every function takes a
 * state and returns a new one, so the engine is fully unit-testable.
 */
object SafeSquareRules {
    const val TOKENS_PER_PLAYER = 3
    const val TRACK_SIZE = 28
    const val HOME_LANE_SIZE = 3
    const val START_PROGRESS = -1
    const val LAST_TRACK_PROGRESS = TRACK_SIZE - 1
    const val FINISH_PROGRESS = TRACK_SIZE + HOME_LANE_SIZE
    const val EXIT_ROLL = 6

    const val PLAYER_ONE_ENTRY = 0
    const val PLAYER_TWO_ENTRY = 14

    /** Fixed protected squares: both entries plus four neutral squares, symmetric for fairness. */
    val SAFE_TRACK_INDICES: Set<Int> = setOf(PLAYER_ONE_ENTRY, 5, 10, PLAYER_TWO_ENTRY, 19, 24)

    sealed interface RouteStep {
        data class Track(val index: Int) : RouteStep
        data class Lane(val cell: Int) : RouteStep
        data object Home : RouteStep
    }

    private val routes: Map<Player, List<RouteStep>> = Player.entries.associateWith { player ->
        buildList {
            for (step in 0..LAST_TRACK_PROGRESS) add(RouteStep.Track((entryIndex(player) + step) % TRACK_SIZE))
            for (cell in 0 until HOME_LANE_SIZE) add(RouteStep.Lane(cell))
            add(RouteStep.Home)
        }
    }

    fun entryIndex(player: Player): Int =
        if (player == Player.PLAYER_ONE) PLAYER_ONE_ENTRY else PLAYER_TWO_ENTRY

    fun isSafeTrackIndex(index: Int): Boolean = index in SAFE_TRACK_INDICES

    /** Ordered route for [player]; the list index equals route progress (entry = 0, HOME = 31). */
    fun getRoute(player: Player): List<RouteStep> = routes.getValue(player)

    fun stateForProgress(progress: Int): TokenState = when {
        progress < 0 -> TokenState.START
        progress <= LAST_TRACK_PROGRESS -> TokenState.TRACK
        progress < FINISH_PROGRESS -> TokenState.HOME_LANE
        else -> TokenState.FINISHED
    }

    fun sharedTrackIndex(player: Player, progress: Int): Int? =
        if (progress in 0..LAST_TRACK_PROGRESS) (entryIndex(player) + progress) % TRACK_SIZE else null

    /** Shared board index of [token], or null when it is in START, its home lane, or HOME. */
    fun getSharedTrackIndex(token: SafeRaceToken): Int? = sharedTrackIndex(token.player, token.progress)

    fun initialState(startingPlayer: Player = Player.PLAYER_ONE): SafeSquareGameState =
        SafeSquareGameState(
            tokens = Player.entries.flatMap { player ->
                (0 until TOKENS_PER_PLAYER).map { local ->
                    SafeRaceToken(
                        id = player.ordinal * TOKENS_PER_PLAYER + local,
                        player = player,
                        state = TokenState.START
                    )
                }
            },
            currentPlayer = startingPlayer
        )

    fun destinationProgress(token: SafeRaceToken, dice: Int): Int? = when (token.state) {
        TokenState.START -> if (dice == EXIT_ROLL) 0 else null
        TokenState.FINISHED -> null
        TokenState.TRACK, TokenState.HOME_LANE ->
            (token.progress + dice).takeIf { it <= FINISH_PROGRESS }
    }

    fun isLegalMove(state: SafeSquareGameState, token: SafeRaceToken, dice: Int): Boolean {
        val destination = destinationProgress(token, dice) ?: return false
        if (destination == FINISH_PROGRESS) return true
        val others = state.tokens.filter { it.id != token.id }

        val sharedDestination = sharedTrackIndex(token.player, destination)
        if (sharedDestination != null) {
            val occupant = others.firstOrNull { getSharedTrackIndex(it) == sharedDestination }
                ?: return true
            if (isSafeTrackIndex(sharedDestination)) return false
            return occupant.player != token.player
        }
        // Home lane: private to the owner, so only own tokens can block.
        return others.none { it.player == token.player && it.progress == destination }
    }

    fun getLegalTokens(state: SafeSquareGameState, player: Player, dice: Int): List<SafeRaceToken> =
        state.tokens.filter { it.player == player && isLegalMove(state, it, dice) }

    /** Applies a die result. Returns null if a roll is not allowed right now. */
    fun roll(state: SafeSquareGameState, dice: Int): SafeSquareGameState? {
        require(dice in 1..6) { "Die value must be 1..6" }
        if (state.winner != null || !state.waitingForRoll || state.isAnimating) return null
        val isSix = dice == EXIT_ROLL
        val player = state.currentPlayer
        return state.copy(
            diceValue = dice,
            legalTokenIds = getLegalTokens(state, player, dice).map { it.id }.toSet(),
            waitingForRoll = false,
            turns = state.turns + 1,
            player1Sixes = state.player1Sixes + if (isSix && player == Player.PLAYER_ONE) 1 else 0,
            player2Sixes = state.player2Sixes + if (isSix && player == Player.PLAYER_TWO) 1 else 0
        )
    }

    /** Resolves a roll with no legal move: a 6 still earns the extra roll, otherwise the turn passes. */
    fun passTurn(state: SafeSquareGameState): SafeSquareGameState? {
        if (!state.hasNoLegalMove || state.isAnimating) return null
        return endTurn(state, extraTurn = state.diceValue == EXIT_ROLL)
    }

    /** Moves [tokenId] by the current die value. Returns null for any illegal request. */
    fun applyMove(state: SafeSquareGameState, tokenId: Int): MoveOutcome? {
        if (state.winner != null || state.waitingForRoll || state.isAnimating) return null
        val dice = state.diceValue ?: return null
        val token = state.tokens.firstOrNull { it.id == tokenId } ?: return null
        if (token.player != state.currentPlayer) return null
        if (tokenId !in state.legalTokenIds || !isLegalMove(state, token, dice)) return null

        val destination = destinationProgress(token, dice) ?: return null
        val moved = token.copy(progress = destination, state = stateForProgress(destination))
        val sharedDestination = sharedTrackIndex(token.player, destination)
        val destinationSafe = sharedDestination != null && isSafeTrackIndex(sharedDestination)

        val captured = if (sharedDestination != null && !destinationSafe) {
            state.tokens.firstOrNull {
                it.player != token.player && getSharedTrackIndex(it) == sharedDestination
            }
        } else {
            null
        }
        // Deploying onto your own entry is not counted as a tactical safe landing.
        val safeLanding = destinationSafe && token.state != TokenState.START

        val tokens = state.tokens.map {
            when (it.id) {
                moved.id -> moved
                captured?.id -> it.copy(progress = START_PROGRESS, state = TokenState.START)
                else -> it
            }
        }
        val player = token.player
        val isP1 = player == Player.PLAYER_ONE
        val captureDelta = if (captured != null) 1 else 0
        val safeDelta = if (safeLanding) 1 else 0
        val updated = state.copy(
            tokens = tokens,
            player1Captures = state.player1Captures + if (isP1) captureDelta else 0,
            player2Captures = state.player2Captures + if (!isP1) captureDelta else 0,
            player1SafeLandings = state.player1SafeLandings + if (isP1) safeDelta else 0,
            player2SafeLandings = state.player2SafeLandings + if (!isP1) safeDelta else 0
        )

        val winner = player.takeIf { p -> tokens.filter { it.player == p }.all { it.state == TokenState.FINISHED } }
        val extraTurn = winner == null && dice == EXIT_ROLL
        val next = if (winner != null) {
            updated.copy(winner = winner, diceValue = null, legalTokenIds = emptySet(), waitingForRoll = false)
        } else {
            endTurn(updated, extraTurn)
        }

        return MoveOutcome(
            state = next,
            tokenId = token.id,
            fromProgress = token.progress,
            toProgress = destination,
            capturedTokenId = captured?.id,
            capturedFromProgress = captured?.progress,
            safeLanding = safeLanding,
            finished = destination == FINISH_PROGRESS,
            extraTurn = extraTurn,
            winner = winner
        )
    }

    private fun endTurn(state: SafeSquareGameState, extraTurn: Boolean): SafeSquareGameState =
        state.copy(
            currentPlayer = if (extraTurn) state.currentPlayer else state.currentPlayer.opponent,
            diceValue = null,
            legalTokenIds = emptySet(),
            waitingForRoll = true
        )
}
