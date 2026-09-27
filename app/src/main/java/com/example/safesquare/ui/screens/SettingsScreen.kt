package com.example.safesquare.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.safesquare.data.AppSettings
import com.example.safesquare.data.BoardTheme
import com.example.safesquare.data.GameStats
import com.example.safesquare.data.ThemeMode
import com.example.safesquare.ui.components.ScreenContainer
import com.example.safesquare.ui.components.ScreenHeader
import com.example.safesquare.ui.components.SecondaryButton
import com.example.safesquare.ui.components.SectionCard
import com.example.safesquare.ui.components.StatRow
import com.example.safesquare.ui.components.VSpace

data class SettingsActions(
    val onThemeMode: (ThemeMode) -> Unit,
    val onBoardTheme: (BoardTheme) -> Unit,
    val onSound: (Boolean) -> Unit,
    val onHaptics: (Boolean) -> Unit,
    val onShowLegalMoves: (Boolean) -> Unit,
    val onHighlightSafe: (Boolean) -> Unit,
    val onFastMovement: (Boolean) -> Unit,
    val onResetStats: () -> Unit
)

@Composable
fun SettingsScreen(
    settings: AppSettings,
    stats: GameStats,
    actions: SettingsActions,
    onBack: () -> Unit
) {
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    ScreenContainer {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader("Settings", onBack)
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(Modifier.widthIn(max = 560.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SectionCard {
                        SectionLabel("APPEARANCE")
                        OptionLabel("Theme")
                        Segmented(
                            options = listOf(ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark", ThemeMode.SYSTEM to "System"),
                            selected = settings.themeMode,
                            onSelect = actions.onThemeMode
                        )
                        VSpace(14.dp)
                        OptionLabel("Board Theme")
                        Segmented(
                            options = listOf(BoardTheme.CLASSIC to "Classic", BoardTheme.MIDNIGHT to "Midnight"),
                            selected = settings.boardTheme,
                            onSelect = actions.onBoardTheme
                        )
                    }
                    SectionCard {
                        SectionLabel("FEEDBACK")
                        ToggleRow("Sound Effects", settings.soundEnabled, actions.onSound)
                        ToggleRow("Haptic Feedback", settings.hapticsEnabled, actions.onHaptics)
                    }
                    SectionCard {
                        SectionLabel("GAMEPLAY")
                        ToggleRow("Show Legal Moves", settings.showLegalMoves, actions.onShowLegalMoves, "Highlight tokens that can use the roll")
                        ToggleRow("Highlight Safe Squares", settings.highlightSafeSquares, actions.onHighlightSafe, "Gold accent on shield squares")
                        ToggleRow("Fast Movement", settings.fastMovement, actions.onFastMovement, "Quicker token animation")
                    }
                    SectionCard {
                        SectionLabel("STATISTICS")
                        StatRow("Games Played", "${stats.gamesPlayed}")
                        StatRow("Player 1 Wins", "${stats.playerOneWins}")
                        StatRow("Player 2 Wins", "${stats.playerTwoWins}")
                        StatRow("Most Captures in a Game", "${stats.mostCaptures}")
                        VSpace(10.dp)
                        SecondaryButton("Reset Statistics", { confirmReset = true }, Modifier.fillMaxWidth())
                    }
                    VSpace(12.dp)
                }
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset statistics?", style = MaterialTheme.typography.titleLarge) },
            text = { Text("Games played and win counts will be cleared.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    actions.onResetStats()
                }) { Text("RESET") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("CANCEL") } },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
    VSpace(10.dp)
}

@Composable
private fun OptionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    VSpace(8.dp)
}

@Composable
private fun <T> Segmented(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .selectableGroup()
        ) {
            options.forEach { (value, label) ->
                val isSelected = value == selected
                Box(
                    Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(value) }),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                label.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit, detail: String? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            if (detail != null) {
                Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}
