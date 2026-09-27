package com.example.safesquare.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.safesquare.data.AppSettings
import com.example.safesquare.feedback.Haptic
import com.example.safesquare.feedback.Haptics
import com.example.safesquare.feedback.Sfx
import com.example.safesquare.feedback.SoundEngine
import com.example.safesquare.game.Player
import com.example.safesquare.game.SafeSquareRules
import com.example.safesquare.ui.components.DieView
import com.example.safesquare.ui.components.GameBoard
import com.example.safesquare.ui.components.PlayerPanel
import com.example.safesquare.ui.components.PrimaryButton
import com.example.safesquare.ui.components.ScreenContainer
import com.example.safesquare.ui.components.ShieldGlyph
import com.example.safesquare.ui.theme.LocalBoardPalette
import com.example.safesquare.ui.theme.playerColors
import kotlinx.coroutines.delay

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    settings: AppSettings,
    soundEngine: SoundEngine,
    haptics: Haptics,
    onExitToHome: () -> Unit,
    onGameFinished: () -> Unit
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val currentSettings by rememberUpdatedState(settings)
    var showRestart by rememberSaveable { mutableStateOf(false) }
    var showLeave by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(settings.fastMovement) { viewModel.setFastMovement(settings.fastMovement) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val s = currentSettings
            if (s.soundEnabled) {
                when (event) {
                    GameEvent.ROLL -> soundEngine.play(Sfx.DICE)
                    GameEvent.STEP -> soundEngine.play(Sfx.MOVE)
                    GameEvent.SAFE -> soundEngine.play(Sfx.SAFE)
                    GameEvent.CAPTURE -> soundEngine.play(Sfx.CAPTURE)
                    GameEvent.HOME -> soundEngine.play(Sfx.HOME)
                    GameEvent.WIN -> soundEngine.play(Sfx.WIN)
                }
            }
            if (s.hapticsEnabled) {
                when (event) {
                    GameEvent.ROLL -> haptics.perform(Haptic.DICE)
                    GameEvent.SAFE -> haptics.perform(Haptic.SAFE)
                    GameEvent.CAPTURE -> haptics.perform(Haptic.CAPTURE)
                    GameEvent.HOME -> haptics.perform(Haptic.HOME)
                    GameEvent.WIN -> haptics.perform(Haptic.WIN)
                    GameEvent.STEP -> Unit
                }
            }
        }
    }

    val winner = ui.game.winner
    val finishCallback by rememberUpdatedState(onGameFinished)
    LaunchedEffect(winner, ui.game.isAnimating) {
        if (winner != null && !ui.game.isAnimating) {
            delay(1300)
            finishCallback()
        }
    }

    BackHandler(enabled = winner == null) { showLeave = true }

    ScreenContainer {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val landscape = maxWidth > maxHeight
            val board: @Composable (Modifier) -> Unit = { mod ->
                BoxWithConstraints(mod, contentAlignment = Alignment.Center) {
                    val side = minOf(maxWidth, maxHeight, 720.dp)
                    BoardWithOverlays(ui, settings, viewModel.stepDurationMillis(), viewModel::moveToken, Modifier.size(side))
                }
            }
            if (landscape) {
                Row(
                    Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    board(Modifier.fillMaxHeight().weight(1.2f))
                    Spacer(Modifier.width(20.dp))
                    Column(
                        Modifier
                            .weight(1f)
                            .widthIn(max = 460.dp)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
                    ) {
                        GameTopBar({ showLeave = true }, { showRestart = true }, winner == null)
                        PanelFor(ui, Player.PLAYER_TWO)
                        TurnAndDice(ui, settings, viewModel::roll)
                        PanelFor(ui, Player.PLAYER_ONE)
                    }
                }
            } else {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GameTopBar({ showLeave = true }, { showRestart = true }, winner == null)
                    PanelFor(ui, Player.PLAYER_TWO)
                    board(Modifier.fillMaxWidth().weight(1f))
                    PanelFor(ui, Player.PLAYER_ONE)
                    TurnAndDice(ui, settings, viewModel::roll)
                }
            }
        }
    }

    if (showRestart) {
        ConfirmDialog(
            title = "Restart this race?",
            message = "All tokens return to Start. This match will not be recorded.",
            confirm = "Restart",
            onConfirm = {
                showRestart = false
                viewModel.newGame()
            },
            onDismiss = { showRestart = false }
        )
    }
    if (showLeave) {
        ConfirmDialog(
            title = "Leave this race?",
            message = "The current match will be abandoned.",
            confirm = "Leave",
            onConfirm = {
                showLeave = false
                onExitToHome()
            },
            onDismiss = { showLeave = false }
        )
    }
}

@Composable
private fun GameTopBar(onMenu: () -> Unit, onRestart: () -> Unit, enabled: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onMenu, enabled = enabled, modifier = Modifier.heightIn(min = 48.dp)) {
            Text("MENU", style = MaterialTheme.typography.labelLarge)
        }
        Text(
            "SAFE SQUARE",
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = MaterialTheme.typography.titleLarge.fontFamily),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onRestart, enabled = enabled, modifier = Modifier.heightIn(min = 48.dp)) {
            Text("RESTART", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun PanelFor(ui: GameUiState, player: Player) {
    val g = ui.game
    PlayerPanel(
        player = player,
        finished = g.finishedCount(player),
        active = g.activeCount(player),
        inStart = g.startCount(player),
        isTurn = g.winner == null && g.currentPlayer == player
    )
}

@Composable
private fun TurnAndDice(ui: GameUiState, settings: AppSettings, onRoll: () -> Unit) {
    val g = ui.game
    val player = g.winner ?: g.currentPlayer
    val colors = playerColors(player)
    val headline = if (g.winner != null) "${player.label.uppercase()} WINS" else "${player.label.uppercase()}'S TURN"
    val status = when {
        g.winner != null -> "Race complete"
        ui.isRolling -> "Rolling…"
        g.hasNoLegalMove -> "No legal move"
        ui.movingTokenId != null || (g.isAnimating && !g.waitingForRoll) -> "Moving…"
        !g.waitingForRoll && g.diceValue != null ->
            if (settings.showLegalMoves) "Choose a highlighted token" else "Choose a token to move"
        ui.extraRoll -> "Rolled a 6 · roll again"
        else -> "Roll the die"
    }
    val safeHeld = SafeSquareRules.SAFE_TRACK_INDICES.count { idx ->
        g.tokens.any { SafeSquareRules.getSharedTrackIndex(it) == idx }
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, colors.base.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
            ) {
                Text(headline, style = MaterialTheme.typography.titleMedium, color = colors.light, maxLines = 1)
                Text(status, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .semantics(mergeDescendants = true) {
                            contentDescription = "Safe Squares occupied: $safeHeld of 6"
                        }
                ) {
                    ShieldGlyph(LocalBoardPalette.current.safeRing, 14.dp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "SAFE $safeHeld / 6 HELD",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            val dieValue = ui.rollingFace ?: g.diceValue ?: ui.lastDice
            DieView(
                value = dieValue,
                rolling = ui.isRolling,
                dimmed = g.diceValue == null && !ui.isRolling,
                size = 60.dp,
                modifier = Modifier.clickable(enabled = ui.canRoll, onClickLabel = "Roll die", onClick = onRoll)
            )
            Spacer(Modifier.width(10.dp))
            PrimaryButton("Roll", onClick = onRoll, enabled = ui.canRoll)
        }
    }
}

@Composable
private fun BoardWithOverlays(
    ui: GameUiState,
    settings: AppSettings,
    stepMillis: Int,
    onTokenClick: (Int) -> Unit,
    modifier: Modifier
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        GameBoard(
            game = ui.game,
            displayProgress = ui.displayProgress,
            movingTokenId = ui.movingTokenId,
            capturedFlashId = ui.capturedFlashId,
            safePulse = ui.safePulse,
            stepMillis = stepMillis,
            showLegalMoves = settings.showLegalMoves,
            highlightSafeSquares = settings.highlightSafeSquares,
            onTokenClick = { if (ui.canMove) onTokenClick(it) }
        )
        val bannerType = ui.banner?.type
        val lastBanner = remember { mutableStateOf(BannerType.SAFE) }
        if (bannerType != null) lastBanner.value = bannerType
        AnimatedVisibility(
            visible = bannerType != null && ui.game.winner == null,
            enter = fadeIn(tween(140)) + scaleIn(tween(160), initialScale = 0.9f),
            exit = fadeOut(tween(220)) + scaleOut(tween(220), targetScale = 0.96f),
            modifier = Modifier.align(Alignment.Center)
        ) {
            BannerPill(lastBanner.value, ui)
        }
        AnimatedVisibility(
            visible = ui.game.winner != null,
            enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.9f),
            exit = fadeOut(tween(150)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            val w = ui.game.winner ?: Player.PLAYER_ONE
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                border = BorderStroke(2.dp, playerColors(w).light)
            ) {
                Text(
                    "${w.label.uppercase()} WINS",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 18.dp)
                )
            }
        }
    }
}

@Composable
private fun BannerPill(type: BannerType, ui: GameUiState) {
    val palette = LocalBoardPalette.current
    val accent = when (type) {
        BannerType.SAFE -> palette.safeRing
        BannerType.CAPTURE -> playerColors(ui.game.currentPlayer).light
        BannerType.HOME -> MaterialTheme.colorScheme.primary
        BannerType.NO_LEGAL_MOVE -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        border = BorderStroke(1.5.dp, accent),
        shadowElevation = 6.dp,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive }
    ) {
        Row(
            Modifier.padding(horizontal = 22.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (type == BannerType.SAFE) {
                ShieldGlyph(accent, 18.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text(type.text, style = MaterialTheme.typography.titleLarge, color = accent)
        }
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm.uppercase()) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } },
        containerColor = MaterialTheme.colorScheme.surface
    )
}
