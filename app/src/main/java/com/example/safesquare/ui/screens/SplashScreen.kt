package com.example.safesquare.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.example.safesquare.game.BoardLayout
import com.example.safesquare.game.Player
import com.example.safesquare.game.SafeSquareRules
import com.example.safesquare.ui.components.ScreenContainer
import com.example.safesquare.ui.components.drawBoard
import com.example.safesquare.ui.components.drawDie
import com.example.safesquare.ui.components.drawToken
import com.example.safesquare.ui.theme.Graphite800
import com.example.safesquare.ui.theme.LocalBoardPalette

private const val SPLASH_MS = 1500f

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val time = remember { Animatable(0f) }
    val finish by rememberUpdatedState(onFinished)
    LaunchedEffect(Unit) {
        time.animateTo(SPLASH_MS, tween(SPLASH_MS.toInt(), easing = LinearEasing))
        finish()
    }
    val palette = LocalBoardPalette.current
    val measurer = rememberTextMeasurer()
    val safeOrder = remember { SafeSquareRules.SAFE_TRACK_INDICES.sorted() }

    ScreenContainer {
        Column(
            Modifier
                .fillMaxSize()
                .semantics { contentDescription = "Safe Square" },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Canvas(
                Modifier
                    .fillMaxWidth(0.78f)
                    .widthIn(max = 420.dp)
                    .aspectRatio(1f)
                    .graphicsLayer {
                        val t = time.value
                        alpha = (t / 300f).coerceIn(0f, 1f)
                        val scale = 0.96f + 0.04f * alpha
                        scaleX = scale
                        scaleY = scale
                    }
            ) {
                val t = time.value
                val s = size.minDimension
                drawBoard(palette, highlightSafe = true, textMeasurer = measurer) { id ->
                    val k = safeOrder.indexOf(id)
                    if (t > 300f + k * 70f) 1f else 0f
                }

                fun nodeAt(progress: Float): Offset {
                    val i = progress.toInt().coerceIn(0, SafeSquareRules.TRACK_SIZE - 1)
                    val j = (i + 1) % SafeSquareRules.TRACK_SIZE
                    val f = progress - i
                    val a = BoardLayout.nodes[i]
                    val b = BoardLayout.nodes[j]
                    return Offset((a.x + (b.x - a.x) * f) * s, (a.y + (b.y - a.y) * f) * s)
                }

                // Die tumbles, then settles on 5.
                if (t in 420f..1150f) {
                    val rolling = t < 800f
                    val face = if (rolling) ((t / 60f).toInt() % 6) + 1 else 5
                    val side = s * 0.1f
                    val center = Offset(s * 0.695f, s * 0.695f)
                    rotate(if (rolling) (t - 420f) * 0.9f else 0f, center) {
                        drawDie(center - Offset(side / 2, side / 2), side, face, Graphite800)
                    }
                }

                // Player 1 token moves from its entry onto the shielded square 5.
                val p1Progress = ((t - 800f) / 300f).coerceIn(0f, 1f) * 5f
                val tokenR = BoardLayout.TOKEN_RADIUS * s
                drawToken(nodeAt(p1Progress), tokenR, Player.PLAYER_ONE, (t / 400f).coerceIn(0f, 1f))

                // Landing ring on the Safe Square.
                if (t in 1100f..1450f) {
                    val k = (t - 1100f) / 350f
                    drawCircle(
                        palette.safeRing.copy(alpha = 1f - k),
                        BoardLayout.NODE_RADIUS * s * (1.1f + k),
                        nodeAt(5f),
                        style = Stroke(s * 0.007f)
                    )
                }

                // Opponent passes by, unable to touch the protected token.
                if (t > 1050f) {
                    val p2Progress = 2f + ((t - 1050f) / 400f).coerceIn(0f, 1f) * 7f
                    drawToken(nodeAt(p2Progress), tokenR, Player.PLAYER_TWO, ((t - 1050f) / 120f).coerceIn(0f, 1f))
                }
            }
            Spacer(Modifier.height(28.dp))
            val titleAlpha = ((time.value - 1050f) / 350f).coerceIn(0f, 1f)
            Text(
                "SAFE SQUARE",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.graphicsLayer {
                    alpha = titleAlpha
                    translationY = (1f - titleAlpha) * 18f
                }
            )
        }
    }
}
