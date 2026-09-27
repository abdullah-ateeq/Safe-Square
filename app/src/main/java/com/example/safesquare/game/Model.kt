package com.example.safesquare.game

enum class Player {
    PLAYER_ONE,
    PLAYER_TWO;

    val opponent: Player
        get() = if (this == PLAYER_ONE) PLAYER_TWO else PLAYER_ONE

    val number: Int
        get() = ordinal + 1

    val label: String
        get() = "Player $number"
}

enum class TokenState {
    START,
    TRACK,
    HOME_LANE,
    FINISHED
}

/**
 * A single race token.
 *
 * [progress] is the distance travelled along the player's route:
 * -1 = START, 0..27 = shared track (0 is the entry square),
 * 28..30 = private home lane, 31 = finished (HOME).
 */
data class SafeRaceToken(
    val id: Int,
    val player: Player,
    val state: TokenState,
    val progress: Int = SafeSquareRules.START_PROGRESS
) {
    /** 0-based index of this token within its owner's set. */
    val localIndex: Int
        get() = id % SafeSquareRules.TOKENS_PER_PLAYER
}

data class SafeSquareGameState(
    val tokens: List<SafeRaceToken>,
    val currentPlayer: Player = Player.PLAYER_ONE,
    val diceValue: Int? = null,
    val legalTokenIds: Set<Int> = emptySet(),
    val waitingForRoll: Boolean = true,
    val winner: Player? = null,
    val turns: Int = 0,
    val player1Captures: Int = 0,
    val player2Captures: Int = 0,
    val player1SafeLandings: Int = 0,
    val player2SafeLandings: Int = 0,
    val player1Sixes: Int = 0,
    val player2Sixes: Int = 0,
    val isAnimating: Boolean = false
) {
    fun tokensOf(player: Player): List<SafeRaceToken> = tokens.filter { it.player == player }

    fun finishedCount(player: Player): Int = tokensOf(player).count { it.state == TokenState.FINISHED }

    fun startCount(player: Player): Int = tokensOf(player).count { it.state == TokenState.START }

    fun activeCount(player: Player): Int =
        tokensOf(player).count { it.state == TokenState.TRACK || it.state == TokenState.HOME_LANE }

    fun capturesOf(player: Player): Int =
        if (player == Player.PLAYER_ONE) player1Captures else player2Captures

    fun safeLandingsOf(player: Player): Int =
        if (player == Player.PLAYER_ONE) player1SafeLandings else player2SafeLandings

    fun sixesOf(player: Player): Int =
        if (player == Player.PLAYER_ONE) player1Sixes else player2Sixes

    /** True after a roll that left the current player with nothing to move. */
    val hasNoLegalMove: Boolean
        get() = winner == null && !waitingForRoll && diceValue != null && legalTokenIds.isEmpty()
}

/** Result of a successfully applied move. [state] already reflects the turn hand-over. */
data class MoveOutcome(
    val state: SafeSquareGameState,
    val tokenId: Int,
    val fromProgress: Int,
    val toProgress: Int,
    val capturedTokenId: Int?,
    val capturedFromProgress: Int?,
    val safeLanding: Boolean,
    val finished: Boolean,
    val extraTurn: Boolean,
    val winner: Player?
) {
    /** Route progress values the token visits, one entry per animation step. */
    val path: List<Int>
        get() = if (fromProgress < 0) listOf(toProgress) else ((fromProgress + 1)..toProgress).toList()
}
