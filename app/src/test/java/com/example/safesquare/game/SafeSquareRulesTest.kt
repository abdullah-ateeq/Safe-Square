package com.example.safesquare.game

import com.example.safesquare.game.SafeSquareRules.FINISH_PROGRESS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeSquareRulesTest {

    private val p1 = Player.PLAYER_ONE
    private val p2 = Player.PLAYER_TWO

    /** Builds a state with tokens placed at the given progress values (unlisted tokens stay in START). */
    private fun stateWith(
        vararg placements: Pair<Int, Int>,
        current: Player = p1
    ): SafeSquareGameState {
        val base = SafeSquareRules.initialState(current)
        val map = placements.toMap()
        return base.copy(tokens = base.tokens.map { token ->
            map[token.id]?.let { token.copy(progress = it, state = SafeSquareRules.stateForProgress(it)) } ?: token
        })
    }

    /** Progress at which [player]'s token stands on shared track [index]. */
    private fun progressAt(player: Player, index: Int): Int =
        Math.floorMod(index - SafeSquareRules.entryIndex(player), SafeSquareRules.TRACK_SIZE)

    private fun rolled(state: SafeSquareGameState, dice: Int) = SafeSquareRules.roll(state, dice)!!

    private fun move(state: SafeSquareGameState, dice: Int, tokenId: Int) =
        SafeSquareRules.applyMove(rolled(state, dice), tokenId)!!

    @Test
    fun initialState() {
        val s = SafeSquareRules.initialState()
        assertEquals(6, s.tokens.size)
        assertTrue(s.tokens.all { it.state == TokenState.START && it.progress == -1 })
        assertEquals(p1, s.currentPlayer)
        assertTrue(s.waitingForRoll)
        assertNull(s.winner)
        assertEquals(32, SafeSquareRules.getRoute(p1).size) // 28 track + 3 lane + home
        assertEquals(setOf(0, 5, 10, 14, 19, 24), SafeSquareRules.SAFE_TRACK_INDICES)
        assertEquals(14, SafeSquareRules.getRoute(p2).first().let { (it as SafeSquareRules.RouteStep.Track).index })
    }

    @Test
    fun cannotLeaveStartWithoutSix() {
        for (dice in 1..5) {
            val s = rolled(SafeSquareRules.initialState(), dice)
            assertTrue(s.legalTokenIds.isEmpty())
            assertNull(SafeSquareRules.applyMove(s, 0))
        }
    }

    @Test
    fun leaveStartWithSix() {
        val s = rolled(SafeSquareRules.initialState(), 6)
        assertEquals(setOf(0, 1, 2), s.legalTokenIds)
        val out = SafeSquareRules.applyMove(s, 0)!!
        val token = out.state.tokens[0]
        assertEquals(TokenState.TRACK, token.state)
        assertEquals(0, SafeSquareRules.getSharedTrackIndex(token))
    }

    @Test
    fun normalMovement() {
        val out = move(stateWith(0 to 2), 3, 0)
        assertEquals(5, out.state.tokens[0].progress)
        assertEquals(listOf(3, 4, 5), out.path)
        assertEquals(p2, out.state.currentPlayer)
        assertTrue(out.state.waitingForRoll)
    }

    @Test
    fun extraTurnAfterSix() {
        val out = move(stateWith(0 to 1), 6, 0)
        assertTrue(out.extraTurn)
        assertEquals(p1, out.state.currentPlayer)
        assertTrue(out.state.waitingForRoll)
    }

    @Test
    fun safeSquarePreventsCapture() {
        // P2 token on neutral safe square 5; P1 cannot land there at all.
        val s = rolled(stateWith(0 to 2, 3 to progressAt(p2, 5)), 3)
        assertFalse(0 in s.legalTokenIds)
        assertNull(SafeSquareRules.applyMove(s, 0))
        assertEquals(TokenState.TRACK, s.tokens[3].state)
    }

    @Test
    fun occupiedSafeSquareBlocksLanding() {
        // Own token on safe square 10 blocks own landing too.
        val s = rolled(stateWith(0 to 10, 1 to 7), 3)
        assertFalse(1 in s.legalTokenIds)
        // Occupied entry blocks leaving start.
        val entry = rolled(stateWith(3 to progressAt(p2, 0)), 6)
        assertTrue(entry.legalTokenIds.isEmpty())
    }

    @Test
    fun normalSpaceCapture() {
        val target = progressAt(p2, 3)
        val out = move(stateWith(0 to 1, 3 to target), 2, 0)
        assertEquals(3, out.capturedTokenId)
        assertEquals(TokenState.START, out.state.tokens[3].state)
        assertEquals(-1, out.state.tokens[3].progress)
        assertEquals(1, out.state.player1Captures)
    }

    @Test
    fun ownTokenBlocks() {
        val s = rolled(stateWith(0 to 2, 1 to 4), 2)
        assertEquals(setOf(1), s.legalTokenIds)
        assertNull(SafeSquareRules.applyMove(s, 0))
    }

    @Test
    fun homeLaneEntry() {
        val out = move(stateWith(0 to 26), 3, 0)
        val token = out.state.tokens[0]
        assertEquals(TokenState.HOME_LANE, token.state)
        assertNull(SafeSquareRules.getSharedTrackIndex(token))
        // Opponent can never capture inside the lane: P2 moving onto shared index 1 does not touch it.
        assertEquals(29, token.progress)
    }

    @Test
    fun exactRollFinish() {
        val out = move(stateWith(0 to 29), 2, 0)
        assertTrue(out.finished)
        assertEquals(TokenState.FINISHED, out.state.tokens[0].state)
        assertEquals(FINISH_PROGRESS, out.state.tokens[0].progress)
    }

    @Test
    fun overshootRejected() {
        val s = rolled(stateWith(0 to 29, 1 to 3), 5)
        assertEquals(setOf(1), s.legalTokenIds)
        assertNull(SafeSquareRules.applyMove(s, 0))
    }

    @Test
    fun noLegalMoveHandling() {
        val s = rolled(SafeSquareRules.initialState(), 4)
        assertTrue(s.hasNoLegalMove)
        val passed = SafeSquareRules.passTurn(s)!!
        assertEquals(p2, passed.currentPlayer)
        assertTrue(passed.waitingForRoll)

        // A 6 with nothing to move keeps the extra roll.
        val blocked = rolled(stateWith(0 to 29, 1 to 30, 2 to FINISH_PROGRESS), 6)
        assertTrue(blocked.hasNoLegalMove)
        assertEquals(p1, SafeSquareRules.passTurn(blocked)!!.currentPlayer)
    }

    @Test
    fun winAfterAllTokensFinish() {
        val out = move(stateWith(0 to FINISH_PROGRESS, 1 to FINISH_PROGRESS, 2 to 28), 3, 2)
        assertEquals(p1, out.winner)
        assertEquals(p1, out.state.winner)
        assertFalse(out.state.waitingForRoll)
        assertNull(SafeSquareRules.roll(out.state, 6))
    }

    @Test
    fun inputSafety() {
        val waiting = SafeSquareRules.initialState()
        assertNull(SafeSquareRules.applyMove(waiting, 0)) // move before roll
        val s = rolled(stateWith(3 to 2), 6)
        assertNull(SafeSquareRules.roll(s, 3)) // double roll
        assertNull(SafeSquareRules.applyMove(s, 3)) // opponent token
        val out = SafeSquareRules.applyMove(s, 0)!!
        assertNotNull(out)
        assertNull(SafeSquareRules.applyMove(out.state, 1)) // second move from same roll
        assertNull(SafeSquareRules.roll(s.copy(isAnimating = true, waitingForRoll = true), 2))
    }

    @Test
    fun randomGamesCompleteWithValidInvariants() {
        repeat(200) { seed ->
            val random = kotlin.random.Random(seed)
            var state = SafeSquareRules.initialState()
            var guard = 0
            while (state.winner == null) {
                check(++guard < 5000) { "Game $seed did not finish" }
                state = rolled(state, random.nextInt(1, 7))
                state = if (state.hasNoLegalMove) {
                    SafeSquareRules.passTurn(state)!!
                } else {
                    SafeSquareRules.applyMove(state, state.legalTokenIds.random(random))!!.state
                }
                val shared = state.tokens.mapNotNull { SafeSquareRules.getSharedTrackIndex(it) }
                assertEquals("No two tokens share a track space", shared.size, shared.toSet().size)
                Player.entries.forEach { p ->
                    val lane = state.tokensOf(p).filter { it.state == TokenState.HOME_LANE }.map { it.progress }
                    assertEquals(lane.size, lane.toSet().size)
                }
            }
            assertEquals(3, state.finishedCount(state.winner!!))
        }
    }
}
