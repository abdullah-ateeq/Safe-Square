package com.example.safesquare.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.safesquare.data.GameStats
import com.example.safesquare.game.Player
import com.example.safesquare.ui.components.PrimaryButton
import com.example.safesquare.ui.components.ScreenContainer
import com.example.safesquare.ui.components.SecondaryButton
import com.example.safesquare.ui.components.VSpace
import com.example.safesquare.ui.components.drawSafeNode
import com.example.safesquare.ui.components.drawToken
import com.example.safesquare.ui.theme.LocalBoardPalette

@Composable
fun HomeScreen(
    stats: GameStats,
    onPlay: () -> Unit,
    onHowToPlay: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit
) {
    ScreenContainer {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Emblem()
            VSpace(28.dp)
            Text(
                "SAFE SQUARE",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() }
            )
            VSpace(10.dp)
            Text(
                "Race smart. Find safety. Reach home.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            VSpace(40.dp)
            Column(
                Modifier.widthIn(max = 360.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PrimaryButton("Play", onPlay, Modifier.fillMaxWidth())
                SecondaryButton("How to Play", onHowToPlay, Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SecondaryButton("Settings", onSettings, Modifier.weight(1f))
                    SecondaryButton("About", onAbout, Modifier.weight(1f))
                }
            }
            VSpace(32.dp)
            StatsStrip(stats)
        }
    }
}

@Composable
private fun Emblem() {
    val palette = LocalBoardPalette.current
    Canvas(
        Modifier
            .size(112.dp)
            .semantics { contentDescription = "Safe Square emblem" }
    ) {
        val s = size.minDimension
        val c = center
        drawCircle(palette.safeRing.copy(alpha = 0.35f), s * 0.48f, c, style = Stroke(s * 0.012f))
        drawCircle(palette.safeRing.copy(alpha = 0.18f), s * 0.42f, c, style = Stroke(s * 0.006f))
        drawSafeNode(c, s * 0.24f, palette, highlight = true)
        drawToken(c + Offset(-s * 0.3f, s * 0.24f), s * 0.1f, Player.PLAYER_ONE)
        drawToken(c + Offset(s * 0.3f, -s * 0.24f), s * 0.1f, Player.PLAYER_TWO)
    }
}

@Composable
private fun StatsStrip(stats: GameStats) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth()
    ) {
        Row(Modifier.padding(vertical = 14.dp)) {
            StatCell("GAMES", stats.gamesPlayed, Modifier.weight(1f))
            StatCell("P1 WINS", stats.playerOneWins, Modifier.weight(1f))
            StatCell("P2 WINS", stats.playerTwoWins, Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCell(label: String, value: Int, modifier: Modifier) {
    Column(
        modifier.semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("$value", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
