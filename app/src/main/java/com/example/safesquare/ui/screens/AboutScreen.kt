package com.example.safesquare.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.safesquare.game.Player
import com.example.safesquare.ui.components.FeatureRow
import com.example.safesquare.ui.components.ScreenContainer
import com.example.safesquare.ui.components.ScreenHeader
import com.example.safesquare.ui.components.SectionCard
import com.example.safesquare.ui.components.ShieldGlyph
import com.example.safesquare.ui.components.TokenGlyph
import com.example.safesquare.ui.components.VSpace
import com.example.safesquare.ui.components.drawDie
import com.example.safesquare.ui.components.drawSafeNode
import com.example.safesquare.ui.theme.Graphite800
import com.example.safesquare.ui.theme.LocalBoardPalette

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val palette = LocalBoardPalette.current
    val iconColor = MaterialTheme.colorScheme.primary
    ScreenContainer {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader("About", onBack)
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Canvas(Modifier.size(84.dp)) {
                    drawSafeNode(center, size.minDimension * 0.4f, palette, highlight = true)
                }
                VSpace(16.dp)
                Text("Safe Square", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                Text("Version 1.0", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                VSpace(18.dp)
                Text(
                    "A polished local two-player race game where smart use of protected spaces can keep your tokens safe on the journey home.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 480.dp)
                )
                VSpace(24.dp)
                SectionCard(Modifier.widthIn(max = 480.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        FeatureRow("Local 2 Player") {
                            TokenGlyph(Player.PLAYER_ONE, 24.dp)
                        }
                        FeatureRow("Protected Safe Squares") {
                            ShieldGlyph(palette.safeRing, 24.dp)
                        }
                        FeatureRow("Capture Mechanic") {
                            TokenGlyph(Player.PLAYER_TWO, 24.dp)
                        }
                        FeatureRow("Dice Racing") {
                            Canvas(Modifier.size(24.dp)) {
                                val side = size.minDimension * 0.86f
                                drawDie(Offset((size.width - side) / 2, (size.height - side) / 2), side, 5, Graphite800)
                            }
                        }
                        FeatureRow("Offline Play") {
                            Canvas(Modifier.size(24.dp)) {
                                drawCircle(iconColor, size.minDimension * 0.38f, style = Stroke(size.minDimension * 0.09f))
                                drawCircle(iconColor, size.minDimension * 0.12f)
                            }
                        }
                    }
                }
                VSpace(24.dp)
            }
        }
    }
}
