package com.example.safesquare.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.safesquare.data.BoardTheme
import com.example.safesquare.data.ThemeMode
import com.example.safesquare.game.Player

private val DarkColors = darkColorScheme(
    primary = Brass,
    onPrimary = Graphite900,
    secondary = TealLight,
    onSecondary = Color.White,
    tertiary = CrimsonLight,
    background = Graphite900,
    onBackground = Mist,
    surface = Graphite800,
    onSurface = Mist,
    surfaceVariant = Graphite700,
    onSurfaceVariant = MistMuted,
    surfaceContainer = Graphite800,
    surfaceContainerHigh = Graphite700,
    surfaceContainerHighest = Graphite600,
    outline = Color(0xFF3D4657),
    outlineVariant = Color(0xFF2A313E)
)

private val LightColors = lightColorScheme(
    primary = BrassDeep,
    onPrimary = Color.White,
    secondary = Teal,
    onSecondary = Color.White,
    tertiary = Crimson,
    background = Stone50,
    onBackground = Ink,
    surface = Color(0xFFFBF8F2),
    onSurface = Ink,
    surfaceVariant = Stone100,
    onSurfaceVariant = InkMuted,
    surfaceContainer = Stone100,
    surfaceContainerHigh = Stone200,
    surfaceContainerHighest = Color(0xFFD2C7B4),
    outline = Color(0xFFB9AE9B),
    outlineVariant = Color(0xFFD8CFBF)
)

@Immutable
data class BoardPalette(
    val plateLight: Color,
    val plateDark: Color,
    val plateEdge: Color,
    val engraving: Color,
    val trackBand: Color,
    val nodeFill: Color,
    val nodeShade: Color,
    val nodeRim: Color,
    val safeFill: Color,
    val safeRing: Color,
    val safeGlyph: Color,
    val homeFill: Color,
    val label: Color
)

val ClassicBoard = BoardPalette(
    plateLight = Color(0xFFC6A57A),
    plateDark = Color(0xFF94714A),
    plateEdge = Color(0xFF5B4128),
    engraving = Color(0x405B4128),
    trackBand = Color(0x55553A22),
    nodeFill = Color(0xFFEADFC8),
    nodeShade = Color(0xFFC9B893),
    nodeRim = Color(0xFF6A4E33),
    safeFill = Color(0xFFF6ECCF),
    safeRing = Color(0xFFB08A3A),
    safeGlyph = Color(0xFF7A5C22),
    homeFill = Color(0xFF3A2A1A),
    label = Color(0xCC3A2A1A)
)

val MidnightBoard = BoardPalette(
    plateLight = Color(0xFF2C3647),
    plateDark = Color(0xFF1A202B),
    plateEdge = Color(0xFF0B0F15),
    engraving = Color(0x33A9B6CC),
    trackBand = Color(0x660A0E14),
    nodeFill = Color(0xFF465368),
    nodeShade = Color(0xFF323C4D),
    nodeRim = Color(0xFF151A23),
    safeFill = Color(0xFF5A5746),
    safeRing = Color(0xFFC9A95B),
    safeGlyph = Color(0xFFE4CD8E),
    homeFill = Color(0xFF0E1218),
    label = Color(0xCCC9D2E0)
)

@Immutable
data class PlayerColors(val base: Color, val light: Color, val dark: Color)

fun playerColors(player: Player): PlayerColors = when (player) {
    Player.PLAYER_ONE -> PlayerColors(Crimson, CrimsonLight, CrimsonDark)
    Player.PLAYER_TWO -> PlayerColors(Teal, TealLight, TealDark)
}

val LocalBoardPalette = staticCompositionLocalOf { ClassicBoard }

@Composable
fun SafeSquareTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    boardTheme: BoardTheme = BoardTheme.CLASSIC,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val palette = if (boardTheme == BoardTheme.MIDNIGHT) MidnightBoard else ClassicBoard
    CompositionLocalProvider(LocalBoardPalette provides palette) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = Typography,
            content = content
        )
    }
}
