package com.example.safesquare.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.safesquare.game.Player
import com.example.safesquare.game.SafeSquareGameState
import com.example.safesquare.ui.components.PrimaryButton
import com.example.safesquare.ui.components.ScreenContainer
import com.example.safesquare.ui.components.SecondaryButton
import com.example.safesquare.ui.components.SectionCard
import com.example.safesquare.ui.components.StatRow
import com.example.safesquare.ui.components.TokenGlyph
import com.example.safesquare.ui.components.VSpace
import com.example.safesquare.ui.theme.playerColors

@Composable
fun ResultScreen(
    game: SafeSquareGameState,
    durationMillis: Long,
    onPlayAgain: () -> Unit,
    onHome: () -> Unit
) {
    val winner = game.winner ?: Player.PLAYER_ONE
    ScreenContainer {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            TokenGlyph(winner, 72.dp)
            VSpace(18.dp)
            Text(
                "${winner.label.uppercase()} WINS",
                style = MaterialTheme.typography.headlineMedium,
                color = playerColors(winner).light,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() }
            )
            VSpace(6.dp)
            Text(
                "All three tokens reached home.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            VSpace(28.dp)
            SectionCard(Modifier.widthIn(max = 440.dp)) {
                StatRow("Turns Played", "${game.turns}")
                StatRow("Match Duration", formatDuration(durationMillis))
                HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                ComparisonHeader()
                ComparisonRow("Captures", game.player1Captures, game.player2Captures)
                ComparisonRow("Safe Landings", game.player1SafeLandings, game.player2SafeLandings)
                ComparisonRow("Sixes Rolled", game.player1Sixes, game.player2Sixes)
            }
            VSpace(28.dp)
            Column(
                Modifier.widthIn(max = 360.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PrimaryButton("Play Again", onPlayAgain, Modifier.fillMaxWidth())
                SecondaryButton("Home", onHome, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun ComparisonHeader() {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("", modifier = Modifier.weight(1f))
        Row(Modifier.weight(0.5f), horizontalArrangement = Arrangement.Center) { TokenGlyph(Player.PLAYER_ONE, 20.dp) }
        Row(Modifier.weight(0.5f), horizontalArrangement = Arrangement.Center) { TokenGlyph(Player.PLAYER_TWO, 20.dp) }
    }
}

@Composable
private fun ComparisonRow(label: String, p1: Int, p2: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp)
            .semantics(mergeDescendants = true) { contentDescription = "$label: Player 1 $p1, Player 2 $p2" },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text("$p1", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.weight(0.5f))
        Text("$p2", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.weight(0.5f))
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
