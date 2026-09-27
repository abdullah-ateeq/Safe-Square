package com.example.safesquare.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.safesquare.game.BoardLayout
import com.example.safesquare.game.Player
import com.example.safesquare.game.SafeRaceToken
import com.example.safesquare.game.SafeSquareGameState
import com.example.safesquare.game.SafeSquareRules
import com.example.safesquare.game.TokenState
import com.example.safesquare.ui.game.SafePulse
import com.example.safesquare.ui.theme.Ivory
import com.example.safesquare.ui.theme.LocalBoardPalette
import kotlin.math.roundToInt

@Composable
fun GameBoard(
    game: SafeSquareGameState,
    displayProgress: Map<Int, Int>,
    movingTokenId: Int?,
    capturedFlashId: Int?,
    safePulse: SafePulse?,
    stepMillis: Int,
    showLegalMoves: Boolean,
    highlightSafeSquares: Boolean,
    onTokenClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalBoardPalette.current
    val textMeasurer = rememberTextMeasurer()
    val transition = rememberInfiniteTransition(label = "legal")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    val safeRing = remember { Animatable(1f) }
    LaunchedEffect(safePulse?.key) {
        if (safePulse != null) {
            safeRing.snapTo(0f)
            safeRing.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
        }
    }

    BoxWithConstraints(modifier.aspectRatio(1f).clipToBounds()) {
        val boardDp = maxWidth
        val density = LocalDensity.current
        val boardPx = with(density) { boardDp.toPx() }

        Canvas(Modifier.fillMaxSize()) {
            drawBoard(palette, highlightSafeSquares, textMeasurer)
            val s = size.minDimension

            // Destination preview for each legal token.
            if (showLegalMoves) {
                val dice = game.diceValue
                if (dice != null) {
                    game.legalTokenIds.forEach { id ->
                        val token = game.tokens.first { it.id == id }
                        val dest = SafeSquareRules.destinationProgress(token, dice) ?: return@forEach
                        val point = BoardLayout.positionFor(token.player, token.localIndex, dest).toOffset(s)
                        val r = (if (dest == SafeSquareRules.FINISH_PROGRESS) BoardLayout.FINISHED_TOKEN_RADIUS else BoardLayout.NODE_RADIUS) * s * 1.05f
                        drawCircle(
                            Ivory.copy(alpha = 0.55f + 0.35f * pulse),
                            r,
                            point,
                            style = Stroke(s * 0.005f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(s * 0.012f, s * 0.008f)))
                        )
                    }
                }
            }

            // Safe landing pulse ring.
            if (safePulse != null && safeRing.value < 1f) {
                val node = BoardLayout.nodes[safePulse.nodeId]
                val t = safeRing.value
                drawCircle(
                    palette.safeRing.copy(alpha = (1f - t) * 0.9f),
                    BoardLayout.NODE_RADIUS * s * (1.1f + t * 0.9f),
                    Offset(node.x * s, node.y * s),
                    style = Stroke(s * 0.008f * (1f - t * 0.5f))
                )
            }
        }

        // Accessibility anchors for Safe Squares.
        val safeAnchor = 20.dp
        BoardLayout.nodes.filter { it.isSafe }.forEach { node ->
            val occupant = game.tokens.firstOrNull { SafeSquareRules.getSharedTrackIndex(it) == node.id }
            val description = buildString {
                append("Safe Square")
                node.entryOwner?.let { append(", ${it.label} entry") }
                append(if (occupant != null) ", occupied by ${occupant.player.label}" else ", empty")
            }
            Box(
                Modifier
                    .offset { centeredOffset(node.x, node.y, boardPx, safeAnchor.roundToPx()) }
                    .size(safeAnchor)
                    .semantics { contentDescription = description }
            )
        }

        game.tokens.forEach { token ->
            val progress = displayProgress[token.id] ?: token.progress
            TokenPiece(
                token = token,
                progress = progress,
                boardPx = boardPx,
                isMoving = token.id == movingTokenId,
                isLegal = token.id in game.legalTokenIds,
                showLegal = showLegalMoves,
                isFlashing = token.id == capturedFlashId,
                pulse = pulse,
                stepMillis = stepMillis,
                onClick = { onTokenClick(token.id) }
            )
        }
    }
}

private fun centeredOffset(x: Float, y: Float, boardPx: Float, sizePx: Int) =
    IntOffset((x * boardPx - sizePx / 2f).roundToInt(), (y * boardPx - sizePx / 2f).roundToInt())

@Composable
private fun TokenPiece(
    token: SafeRaceToken,
    progress: Int,
    boardPx: Float,
    isMoving: Boolean,
    isLegal: Boolean,
    showLegal: Boolean,
    isFlashing: Boolean,
    pulse: Float,
    stepMillis: Int,
    onClick: () -> Unit
) {
    val target = BoardLayout.positionFor(token.player, token.localIndex, progress)
    val position by animateOffsetAsState(
        targetValue = Offset(target.x, target.y),
        animationSpec = tween(if (isMoving) (stepMillis * 0.85f).toInt() else 420, easing = FastOutSlowInEasing),
        label = "tokenPosition"
    )
    val finished = SafeSquareRules.stateForProgress(progress) == TokenState.FINISHED
    val radiusFraction by animateFloatAsState(
        targetValue = if (finished) BoardLayout.FINISHED_TOKEN_RADIUS else BoardLayout.TOKEN_RADIUS,
        animationSpec = tween(300),
        label = "tokenRadius"
    )
    val alpha = if (isFlashing) 0.35f + 0.65f * (1f - pulse) else 1f
    val hitSize = 44.dp
    val description = tokenDescription(token, progress, isLegal)
    val interaction = remember { MutableInteractionSource() }

    Box(
        Modifier
            .zIndex(if (isMoving) 3f else if (isLegal) 2f else 1f)
            .offset { centeredOffset(position.x, position.y, boardPx, hitSize.roundToPx()) }
            .size(hitSize)
            .semantics { contentDescription = description }
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = isLegal,
                role = Role.Button,
                onClickLabel = "Move token",
                onClick = onClick
            )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val r = radiusFraction * boardPx
            val c = center
            if (isLegal && showLegal) {
                drawCircle(Ivory.copy(alpha = 0.22f + 0.3f * pulse), r * (1.35f + 0.12f * pulse), c)
                drawCircle(Ivory.copy(alpha = 0.85f), r * (1.3f + 0.12f * pulse), c, style = Stroke(r * 0.12f))
            }
            drawToken(c, r, token.player, alpha)
        }
    }
}

private fun tokenDescription(token: SafeRaceToken, progress: Int, legal: Boolean): String {
    val base = "${token.player.label} token ${token.localIndex + 1}"
    val shared = SafeSquareRules.sharedTrackIndex(token.player, progress)
    val where = when (SafeSquareRules.stateForProgress(progress)) {
        TokenState.START -> "in Start"
        TokenState.TRACK -> if (shared != null && SafeSquareRules.isSafeTrackIndex(shared)) "on Safe Square" else "on track"
        TokenState.HOME_LANE -> "in home lane"
        TokenState.FINISHED -> "home"
    }
    return if (legal) "$base, $where, token can move" else "$base, $where"
}

/** Convenience used by panels. */
fun Player.symbolName(): String = if (this == Player.PLAYER_ONE) "Diamond" else "Circle"
