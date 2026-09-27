package com.example.safesquare.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.safesquare.game.Player
import com.example.safesquare.ui.theme.Graphite800
import com.example.safesquare.ui.theme.playerColors

/** Full-screen background with a soft vertical vignette and safe-drawing insets applied. */
@Composable
fun ScreenContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val bg = MaterialTheme.colorScheme.background
    val tint = MaterialTheme.colorScheme.surfaceContainer
    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(tint, bg, bg)))
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        content()
    }
}

/** Title bar used by secondary screens. */
@Composable
fun ScreenHeader(title: String, onBack: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = onBack,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .semantics { contentDescription = "Back" }
        ) {
            BackChevron(MaterialTheme.colorScheme.onSurface)
        }
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() }
        )
    }
}

@Composable
private fun BackChevron(color: Color) {
    Canvas(Modifier.size(20.dp)) {
        val w = size.width
        val stroke = Stroke(w * 0.11f, cap = StrokeCap.Round)
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.62f, w * 0.18f)
            lineTo(w * 0.3f, w * 0.5f)
            lineTo(w * 0.62f, w * 0.82f)
        }
        drawPath(path, color, style = stroke)
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 56.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp, pressedElevation = 1.dp),
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp)
    ) {
        Text(text.uppercase(), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 52.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
    ) {
        Text(text.uppercase(), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SectionCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 1.dp
    ) {
        Column(Modifier.padding(18.dp), content = content)
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** Small raised token glyph for panels and lists. */
@Composable
fun TokenGlyph(player: Player, size: Dp = 22.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        drawToken(center, this.size.minDimension * 0.42f, player)
    }
}

@Composable
fun ShieldGlyph(color: Color, size: Dp = 16.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        drawShield(center, this.size.minDimension * 0.46f, color)
    }
}

/**
 * The premium die. While [rolling] it tumbles briefly; afterwards it settles on [value].
 */
@Composable
fun DieView(value: Int?, rolling: Boolean, dimmed: Boolean, modifier: Modifier = Modifier, size: Dp = 64.dp) {
    val tilt = remember { Animatable(0f) }
    LaunchedEffect(rolling) {
        if (rolling) {
            tilt.snapTo(0f)
            tilt.animateTo(360f, tween(330, easing = FastOutSlowInEasing))
        } else {
            tilt.snapTo(0f)
        }
    }
    val pip = Graphite800
    Canvas(
        modifier
            .size(size)
            .semantics {
                contentDescription = when {
                    rolling -> "Dice rolling"
                    value != null -> "Dice rolled $value"
                    else -> "Dice ready"
                }
                liveRegion = LiveRegionMode.Polite
            }
    ) {
        val side = this.size.minDimension * 0.8f
        val topLeft = Offset((this.size.width - side) / 2f, (this.size.height - side) / 2f)
        rotate(if (rolling) tilt.value else 0f) {
            drawDie(topLeft, side, value, pip, dimmed)
        }
    }
}

/** Player status panel: identity, home progress, active and waiting tokens. */
@Composable
fun PlayerPanel(
    player: Player,
    finished: Int,
    active: Int,
    inStart: Int,
    isTurn: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = playerColors(player)
    val borderColor = if (isTurn) colors.light else MaterialTheme.colorScheme.outlineVariant
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "${player.label}${if (isTurn) ", current turn" else ""}. " +
                    "Home $finished of 3. Active $active. In start $inStart."
            },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (isTurn) 2.dp else 1.dp, borderColor),
        tonalElevation = if (isTurn) 3.dp else 0.dp
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TokenGlyph(player, 30.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    player.label.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    if (isTurn) "▶ TO PLAY" else "${player.symbolName().uppercase()} TOKENS",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isTurn) colors.light else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            PanelStat("HOME", "$finished / 3")
            Spacer(Modifier.width(14.dp))
            PanelStat("ACTIVE", "$active")
            Spacer(Modifier.width(14.dp))
            PanelStat("START", "$inStart")
        }
    }
}

@Composable
private fun PanelStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun FeatureRow(text: String, content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        content()
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun CenteredColumn(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

@Composable
fun Caption(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier
    )
}

@Composable
fun VSpace(height: Dp) = Spacer(Modifier.height(height))
