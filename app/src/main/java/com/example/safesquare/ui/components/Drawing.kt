package com.example.safesquare.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.sp
import com.example.safesquare.game.BoardLayout
import com.example.safesquare.game.BoardPoint
import com.example.safesquare.game.Player
import com.example.safesquare.game.SafeSquareRules
import com.example.safesquare.ui.theme.BoardPalette
import com.example.safesquare.ui.theme.Ivory
import com.example.safesquare.ui.theme.playerColors

fun BoardPoint.toOffset(boardSize: Float) = Offset(x * boardSize, y * boardSize)

/** Heater-shield outline centred at [center]; [size] is the half-height. */
fun shieldPath(center: Offset, size: Float): Path = Path().apply {
    val w = size * 0.82f
    moveTo(center.x - w, center.y - size * 0.78f)
    lineTo(center.x - w * 0.35f, center.y - size * 0.86f)
    lineTo(center.x, center.y - size)
    lineTo(center.x + w * 0.35f, center.y - size * 0.86f)
    lineTo(center.x + w, center.y - size * 0.78f)
    lineTo(center.x + w, center.y - size * 0.05f)
    quadraticTo(center.x + w * 0.92f, center.y + size * 0.62f, center.x, center.y + size)
    quadraticTo(center.x - w * 0.92f, center.y + size * 0.62f, center.x - w, center.y - size * 0.05f)
    close()
}

fun DrawScope.drawShield(center: Offset, size: Float, color: Color, style: DrawStyle = Fill) {
    drawPath(shieldPath(center, size), color, style = style)
}

fun DrawScope.drawTrackNode(center: Offset, radius: Float, palette: BoardPalette) {
    drawCircle(Color.Black.copy(alpha = 0.22f), radius, center + Offset(0f, radius * 0.14f))
    drawCircle(
        Brush.radialGradient(
            listOf(palette.nodeFill, palette.nodeShade),
            center = center - Offset(radius * 0.3f, radius * 0.35f),
            radius = radius * 1.5f
        ),
        radius,
        center
    )
    drawCircle(palette.nodeRim.copy(alpha = 0.7f), radius, center, style = Stroke(radius * 0.09f))
}

fun DrawScope.drawSafeNode(center: Offset, radius: Float, palette: BoardPalette, highlight: Boolean) {
    drawCircle(Color.Black.copy(alpha = 0.24f), radius, center + Offset(0f, radius * 0.14f))
    val fillTop = if (highlight) palette.safeFill else palette.nodeFill
    drawCircle(
        Brush.radialGradient(
            listOf(fillTop, palette.nodeShade),
            center = center - Offset(radius * 0.3f, radius * 0.35f),
            radius = radius * 1.6f
        ),
        radius,
        center
    )
    val ring = if (highlight) palette.safeRing else palette.nodeRim
    drawCircle(ring, radius, center, style = Stroke(radius * 0.13f))
    drawCircle(ring.copy(alpha = 0.55f), radius * 0.74f, center, style = Stroke(radius * 0.05f))
    val glyph = if (highlight) palette.safeGlyph else palette.nodeRim.copy(alpha = 0.75f)
    drawShield(center, radius * 0.46f, glyph)
    drawShield(center, radius * 0.46f, glyph.copy(alpha = 0.4f), Stroke(radius * 0.05f))
}

fun DrawScope.drawToken(center: Offset, radius: Float, player: Player, alpha: Float = 1f) {
    val colors = playerColors(player)
    drawCircle(Color.Black.copy(alpha = 0.14f * alpha), radius * 1.14f, center + Offset(radius * 0.08f, radius * 0.26f))
    drawCircle(Color.Black.copy(alpha = 0.3f * alpha), radius * 1.0f, center + Offset(radius * 0.05f, radius * 0.16f))
    drawCircle(
        Brush.radialGradient(
            listOf(colors.light, colors.base, colors.dark),
            center = center - Offset(radius * 0.35f, radius * 0.4f),
            radius = radius * 1.7f
        ),
        radius,
        center,
        alpha = alpha
    )
    drawCircle(colors.dark, radius, center, alpha = alpha, style = Stroke(radius * 0.09f))
    drawCircle(Color.White.copy(alpha = 0.16f * alpha), radius * 0.76f, center, style = Stroke(radius * 0.07f))
    val symbol = Ivory.copy(alpha = 0.92f * alpha)
    if (player == Player.PLAYER_ONE) {
        val d = radius * 0.4f
        val diamond = Path().apply {
            moveTo(center.x, center.y - d)
            lineTo(center.x + d * 0.8f, center.y)
            lineTo(center.x, center.y + d)
            lineTo(center.x - d * 0.8f, center.y)
            close()
        }
        drawPath(diamond, symbol)
    } else {
        drawCircle(symbol, radius * 0.3f, center, style = Stroke(radius * 0.13f))
    }
    // Specular highlight for a subtle raised bevel.
    drawOval(
        Color.White.copy(alpha = 0.22f * alpha),
        topLeft = center + Offset(-radius * 0.62f, -radius * 0.72f),
        size = Size(radius * 0.7f, radius * 0.36f)
    )
}

private val pipLayout: Map<Int, List<Pair<Float, Float>>> = mapOf(
    1 to listOf(0.5f to 0.5f),
    2 to listOf(0.27f to 0.27f, 0.73f to 0.73f),
    3 to listOf(0.27f to 0.27f, 0.5f to 0.5f, 0.73f to 0.73f),
    4 to listOf(0.27f to 0.27f, 0.73f to 0.27f, 0.27f to 0.73f, 0.73f to 0.73f),
    5 to listOf(0.27f to 0.27f, 0.73f to 0.27f, 0.5f to 0.5f, 0.27f to 0.73f, 0.73f to 0.73f),
    6 to listOf(0.27f to 0.25f, 0.73f to 0.25f, 0.27f to 0.5f, 0.73f to 0.5f, 0.27f to 0.75f, 0.73f to 0.75f)
)

/** Ivory die with recessed pips, drawn in a square of [side] at [topLeft]. */
fun DrawScope.drawDie(topLeft: Offset, side: Float, value: Int?, pipColor: Color, dimmed: Boolean = false) {
    val corner = CornerRadius(side * 0.2f)
    val a = if (dimmed) 0.55f else 1f
    drawRoundRect(Color.Black.copy(alpha = 0.3f * a), topLeft + Offset(0f, side * 0.06f), Size(side, side), corner)
    drawRoundRect(
        Brush.linearGradient(
            listOf(Color(0xFFFBF6EA), Color(0xFFE2D7C0)),
            start = topLeft,
            end = topLeft + Offset(side, side)
        ),
        topLeft,
        Size(side, side),
        corner,
        alpha = a
    )
    drawRoundRect(
        Color(0xFF8C7C5E).copy(alpha = 0.6f * a),
        topLeft,
        Size(side, side),
        corner,
        style = Stroke(side * 0.025f)
    )
    val pips = value?.let { pipLayout[it] } ?: return
    val r = side * 0.085f
    pips.forEach { (px, py) ->
        val c = topLeft + Offset(px * side, py * side)
        drawCircle(Color.White.copy(alpha = 0.6f * a), r, c + Offset(0f, r * 0.25f))
        drawCircle(pipColor.copy(alpha = a), r, c)
    }
}

/**
 * Draws the static board: plate, track, safe squares, home lanes, start yards and the
 * central HOME medallion. Tokens are drawn separately so they can animate independently.
 */
fun DrawScope.drawBoard(
    palette: BoardPalette,
    highlightSafe: Boolean,
    textMeasurer: TextMeasurer?,
    safeLit: (Int) -> Float = { 1f }
) {
    val s = size.minDimension
    fun p(x: Float, y: Float) = Offset(x * s, y * s)

    // Plate
    val plateCorner = CornerRadius(s * 0.06f)
    drawRoundRect(Color.Black.copy(alpha = 0.35f), Offset(0f, s * 0.012f), Size(s, s), plateCorner)
    drawRoundRect(
        Brush.linearGradient(listOf(palette.plateLight, palette.plateDark), start = Offset.Zero, end = Offset(s, s)),
        cornerRadius = plateCorner,
        size = Size(s, s)
    )
    drawRoundRect(palette.plateEdge, size = Size(s, s), cornerRadius = plateCorner, style = Stroke(s * 0.007f))
    val inset = s * 0.028f
    drawRoundRect(
        palette.engraving,
        topLeft = Offset(inset, inset),
        size = Size(s - inset * 2, s - inset * 2),
        cornerRadius = CornerRadius(s * 0.04f),
        style = Stroke(s * 0.003f)
    )

    // Track band
    val h = BoardLayout.TRACK_HALF
    val c = BoardLayout.CENTER
    fun trackPath(half: Float, radius: Float) = Path().apply {
        addRoundRect(RoundRect((c - half) * s, (c - half) * s, (c + half) * s, (c + half) * s, CornerRadius(radius * s)))
    }
    drawPath(trackPath(h, BoardLayout.CORNER_RADIUS), palette.trackBand, style = Stroke(s * 0.108f))
    val bandEdge = s * 0.0025f
    drawPath(trackPath(h + 0.057f, BoardLayout.CORNER_RADIUS + 0.057f), palette.engraving, style = Stroke(bandEdge))
    drawPath(trackPath(h - 0.057f, BoardLayout.CORNER_RADIUS - 0.057f), palette.engraving, style = Stroke(bandEdge))

    // Decorative engraved shields in the free quadrants
    drawShield(p(0.305f, 0.305f), s * 0.05f, palette.engraving, Stroke(s * 0.004f))
    drawShield(p(0.695f, 0.695f), s * 0.05f, palette.engraving, Stroke(s * 0.004f))

    // Home lanes and lane connectors
    Player.entries.forEach { player ->
        val colors = playerColors(player)
        val lastTrack = BoardLayout.nodes[(SafeSquareRules.entryIndex(player) + SafeSquareRules.LAST_TRACK_PROGRESS) % SafeSquareRules.TRACK_SIZE]
        val firstLane = BoardLayout.laneCell(player, 0)
        drawLine(
            colors.base.copy(alpha = 0.55f),
            p(lastTrack.x, lastTrack.y),
            firstLane.toOffset(s),
            strokeWidth = s * 0.006f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(s * 0.012f, s * 0.008f))
        )
        for (cell in 0 until SafeSquareRules.HOME_LANE_SIZE) {
            val centerPt = BoardLayout.laneCell(player, cell).toOffset(s)
            val half = s * 0.034f
            drawRoundRect(Color.Black.copy(alpha = 0.22f), centerPt - Offset(half, half - s * 0.004f), Size(half * 2, half * 2), CornerRadius(s * 0.012f))
            drawRoundRect(
                Brush.linearGradient(listOf(colors.light, colors.base), start = centerPt - Offset(half, half), end = centerPt + Offset(half, half)),
                centerPt - Offset(half, half),
                Size(half * 2, half * 2),
                CornerRadius(s * 0.012f),
                alpha = 0.78f
            )
            drawRoundRect(colors.dark, centerPt - Offset(half, half), Size(half * 2, half * 2), CornerRadius(s * 0.012f), style = Stroke(s * 0.003f))
            // Chevron pointing toward HOME
            val dir = if (player == Player.PLAYER_ONE) -1f else 1f
            val chev = Path().apply {
                moveTo(centerPt.x - half * 0.4f, centerPt.y - dir * half * 0.15f)
                lineTo(centerPt.x, centerPt.y + dir * half * 0.25f)
                lineTo(centerPt.x + half * 0.4f, centerPt.y - dir * half * 0.15f)
            }
            drawPath(chev, Ivory.copy(alpha = 0.55f), style = Stroke(s * 0.004f))
        }
    }

    // Central HOME medallion, split between the two players
    val homeCenter = p(c, c)
    val homeR = BoardLayout.HOME_RADIUS * s
    drawCircle(Color.Black.copy(alpha = 0.3f), homeR, homeCenter + Offset(0f, s * 0.006f))
    drawCircle(palette.homeFill, homeR, homeCenter)
    drawArc(playerColors(Player.PLAYER_TWO).base.copy(alpha = 0.45f), 180f, 180f, true, homeCenter - Offset(homeR, homeR), Size(homeR * 2, homeR * 2))
    drawArc(playerColors(Player.PLAYER_ONE).base.copy(alpha = 0.45f), 0f, 180f, true, homeCenter - Offset(homeR, homeR), Size(homeR * 2, homeR * 2))
    drawCircle(palette.safeRing, homeR, homeCenter, style = Stroke(s * 0.005f))
    drawLine(palette.safeRing.copy(alpha = 0.6f), homeCenter - Offset(homeR, 0f), homeCenter + Offset(homeR, 0f), s * 0.003f)

    // Start yards
    Player.entries.forEach { player ->
        val colors = playerColors(player)
        val yc = BoardLayout.yardCenter(player).toOffset(s)
        val half = BoardLayout.YARD_HALF * s
        drawRoundRect(colors.base.copy(alpha = 0.16f), yc - Offset(half, half), Size(half * 2, half * 2), CornerRadius(s * 0.025f))
        drawRoundRect(colors.base.copy(alpha = 0.55f), yc - Offset(half, half), Size(half * 2, half * 2), CornerRadius(s * 0.025f), style = Stroke(s * 0.004f))
        for (local in 0 until SafeSquareRules.TOKENS_PER_PLAYER) {
            val slot = BoardLayout.startSlot(player, local).toOffset(s)
            drawCircle(Color.Black.copy(alpha = 0.18f), BoardLayout.TOKEN_RADIUS * s * 1.08f, slot)
            drawCircle(colors.dark.copy(alpha = 0.35f), BoardLayout.TOKEN_RADIUS * s * 1.08f, slot, style = Stroke(s * 0.003f))
        }
        if (textMeasurer != null) {
            val label = textMeasurer.measure(
                "START",
                TextStyle(color = palette.label, fontSize = (s * 0.026f / density).sp, fontWeight = FontWeight.SemiBold, letterSpacing = (s * 0.006f / density).sp)
            )
            val ly = if (player == Player.PLAYER_ONE) yc.y + half * 0.58f else yc.y - half * 0.58f - label.size.height
            drawText(label, topLeft = Offset(yc.x - label.size.width / 2f, ly))
        }
    }

    // Track nodes
    val nodeR = BoardLayout.NODE_RADIUS * s
    BoardLayout.nodes.forEach { node ->
        val center = p(node.x, node.y)
        if (node.isSafe) {
            val lit = safeLit(node.id)
            drawSafeNode(center, nodeR, palette, highlightSafe && lit > 0.5f)
        } else {
            drawTrackNode(center, nodeR, palette)
        }
        node.entryOwner?.let { owner ->
            drawCircle(playerColors(owner).base, nodeR * 1.24f, center, style = Stroke(s * 0.0065f))
        }
    }
}
