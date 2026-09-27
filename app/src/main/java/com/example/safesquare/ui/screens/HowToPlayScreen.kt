package com.example.safesquare.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.safesquare.game.Player
import com.example.safesquare.ui.components.ScreenContainer
import com.example.safesquare.ui.components.ScreenHeader
import com.example.safesquare.ui.components.SectionCard
import com.example.safesquare.ui.components.VSpace
import com.example.safesquare.ui.components.drawSafeNode
import com.example.safesquare.ui.components.drawToken
import com.example.safesquare.ui.components.drawTrackNode
import com.example.safesquare.ui.theme.BoardPalette
import com.example.safesquare.ui.theme.Ivory
import com.example.safesquare.ui.theme.LocalBoardPalette
import com.example.safesquare.ui.theme.playerColors

private val rules = listOf(
    "Roll a 6 to bring a token out of Start onto your entry square. A 6 also grants one extra roll.",
    "Move exactly the rolled number of spaces. Only tokens that can legally move are highlighted.",
    "Land exactly on an opponent on a normal space to capture it — it returns to Start.",
    "Safe Squares (shield markers) protect tokens. No capture can ever happen there.",
    "Only one token may occupy a Safe Square. An occupied Safe Square blocks every landing.",
    "You cannot land on your own token.",
    "After one full lap, your token turns into your private 3-space home lane.",
    "Reaching HOME needs an exact roll. Overshooting is not allowed.",
    "First player to bring all 3 tokens HOME wins."
)

@Composable
fun HowToPlayScreen(onBack: () -> Unit) {
    val palette = LocalBoardPalette.current
    ScreenContainer {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader("How to Play", onBack)
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(Modifier.widthIn(max = 560.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SectionCard {
                        Text("THE RACE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        VSpace(6.dp)
                        Text(
                            "Two players, three tokens each. Race clockwise around 28 shared spaces, then into your home lane.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    SectionCard {
                        rules.forEachIndexed { i, rule -> RuleLine(i + 1, rule) }
                    }
                    SectionCard {
                        Text("EXAMPLES", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        VSpace(12.dp)
                        ExampleStrip(
                            palette,
                            description = "Example: Player 2 rolls 3. The target is a Safe Square held by Player 1, so the move is blocked.",
                            safeIndex = 3,
                            mover = Player.PLAYER_TWO to 0,
                            target = Player.PLAYER_ONE to 3,
                            blocked = true
                        )
                        Text("Roll 3 → occupied Safe Square: blocked.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        VSpace(16.dp)
                        ExampleStrip(
                            palette,
                            description = "Example: Player 1 rolls 3 and lands on Player 2 on a normal space, capturing it.",
                            safeIndex = 5,
                            mover = Player.PLAYER_ONE to 0,
                            target = Player.PLAYER_TWO to 3,
                            blocked = false
                        )
                        Text("Roll 3 → opponent on a normal space: capture.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    VSpace(12.dp)
                }
            }
        }
    }
}

@Composable
private fun RuleLine(number: Int, text: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Text("$number", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
        }
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(start = 12.dp, top = 3.dp))
    }
}

@Composable
private fun ExampleStrip(
    palette: BoardPalette,
    description: String,
    safeIndex: Int,
    mover: Pair<Player, Int>,
    target: Pair<Player, Int>,
    blocked: Boolean
) {
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(76.dp)
            .padding(bottom = 6.dp)
            .semantics { contentDescription = description }
    ) {
        drawRoundRect(
            Brush.linearGradient(listOf(palette.plateLight, palette.plateDark)),
            cornerRadius = CornerRadius(size.height * 0.2f)
        )
        val count = 6
        val gap = size.width / count
        val y = size.height * 0.56f
        val r = minOf(gap * 0.3f, size.height * 0.26f)
        fun at(i: Int) = Offset(gap * (i + 0.5f), y)
        for (i in 0 until count) {
            if (i == safeIndex) drawSafeNode(at(i), r, palette, highlight = true) else drawTrackNode(at(i), r, palette)
        }
        // Movement arc from mover to target.
        drawArrow(at(mover.second), at(target.second), r, blocked)
        drawToken(at(target.second), r * 0.8f, target.first)
        drawToken(at(mover.second), r * 0.8f, mover.first)
        if (!blocked) {
            drawCircle(playerColors(mover.first).light, r * 1.25f, at(target.second), style = Stroke(r * 0.12f))
        }
    }
}

private fun DrawScope.drawArrow(from: Offset, to: Offset, r: Float, blocked: Boolean) {
    val top = size.height * 0.12f
    val path = Path().apply {
        moveTo(from.x, from.y - r)
        cubicTo(from.x, top, to.x, top, to.x, to.y - r * 1.05f)
    }
    val color = if (blocked) Ivory.copy(alpha = 0.6f) else Ivory
    drawPath(
        path,
        color,
        style = Stroke(r * 0.12f, cap = StrokeCap.Round, pathEffect = if (blocked) PathEffect.dashPathEffect(floatArrayOf(r * 0.3f, r * 0.25f)) else null)
    )
    if (blocked) {
        val c = Offset((from.x + to.x) / 2f, top + r * 0.6f)
        val k = r * 0.32f
        drawLine(Ivory, c + Offset(-k, -k), c + Offset(k, k), r * 0.16f, StrokeCap.Round)
        drawLine(Ivory, c + Offset(-k, k), c + Offset(k, -k), r * 0.16f, StrokeCap.Round)
    } else {
        val tip = Offset(to.x, to.y - r * 0.95f)
        val head = Path().apply {
            moveTo(tip.x, tip.y)
            lineTo(tip.x - r * 0.28f, tip.y - r * 0.4f)
            lineTo(tip.x + r * 0.28f, tip.y - r * 0.4f)
            close()
        }
        drawPath(head, Ivory)
    }
}
