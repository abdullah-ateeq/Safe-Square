package com.example.safesquare.game

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class BoardNode(
    val id: Int,
    val x: Float,
    val y: Float,
    val isSafe: Boolean = false,
    val entryOwner: Player? = null
)

data class BoardPoint(val x: Float, val y: Float)

/**
 * Visual geometry of the board in normalized 0..1 coordinates. Gameplay never reads these;
 * it only uses track indices from [SafeSquareRules]. Nodes run clockwise on screen from the
 * Player 1 entry at bottom-centre, through the Player 2 entry at top-centre.
 */
object BoardLayout {
    const val CENTER = 0.5f
    const val TRACK_HALF = 0.39f
    const val CORNER_RADIUS = 0.13f
    const val NODE_RADIUS = 0.042f
    const val TOKEN_RADIUS = 0.034f
    const val LANE_STEP = 0.088f
    const val HOME_RADIUS = 0.07f
    const val FINISHED_TOKEN_RADIUS = 0.022f
    const val YARD_HALF = 0.1f

    val nodes: List<BoardNode> = buildTrack()

    val playerOneYard = BoardPoint(0.305f, 0.695f)
    val playerTwoYard = BoardPoint(0.695f, 0.305f)

    fun yardCenter(player: Player): BoardPoint =
        if (player == Player.PLAYER_ONE) playerOneYard else playerTwoYard

    fun startSlot(player: Player, local: Int): BoardPoint {
        val yard = yardCenter(player)
        // Player 2 yard is rotated 180° so the triangle points toward its owner.
        val s = if (player == Player.PLAYER_ONE) 1f else -1f
        // Nudge the slots away from the START label.
        val c = BoardPoint(yard.x, yard.y - 0.016f * s)
        return when (local) {
            0 -> BoardPoint(c.x - 0.046f * s, c.y + 0.03f * s)
            1 -> BoardPoint(c.x + 0.046f * s, c.y + 0.03f * s)
            else -> BoardPoint(c.x, c.y - 0.046f * s)
        }
    }

    fun laneCell(player: Player, cell: Int): BoardPoint {
        val edge = CENTER + TRACK_HALF
        val offset = LANE_STEP * (cell + 1)
        return if (player == Player.PLAYER_ONE) {
            BoardPoint(CENTER, edge - offset)
        } else {
            BoardPoint(CENTER, 1f - edge + offset)
        }
    }

    fun homeSlot(player: Player, local: Int): BoardPoint {
        val s = if (player == Player.PLAYER_ONE) 1f else -1f
        return when (local) {
            0 -> BoardPoint(CENTER - 0.028f * s, CENTER + 0.022f * s)
            1 -> BoardPoint(CENTER + 0.028f * s, CENTER + 0.022f * s)
            else -> BoardPoint(CENTER, CENTER + 0.046f * s)
        }
    }

    /** Where a token with the given route progress is drawn. */
    fun positionFor(player: Player, local: Int, progress: Int): BoardPoint =
        when (val step = if (progress < 0) null else SafeSquareRules.getRoute(player)[progress]) {
            null -> startSlot(player, local)
            is SafeSquareRules.RouteStep.Track -> nodes[step.index].let { BoardPoint(it.x, it.y) }
            is SafeSquareRules.RouteStep.Lane -> laneCell(player, step.cell)
            SafeSquareRules.RouteStep.Home -> homeSlot(player, local)
        }

    private fun buildTrack(): List<BoardNode> {
        val h = TRACK_HALF
        val r = CORNER_RADIUS
        val c = CENTER
        val straight = 2 * h - 2 * r
        val arc = (PI * r / 2).toFloat()

        // Clockwise on screen (y grows downward), starting at bottom-centre heading left.
        val segments: List<Pair<Float, (Float) -> BoardPoint>> = listOf(
            straight / 2 to { t -> BoardPoint(c - t * straight / 2, c + h) },
            arc to arcSegment(c - h + r, c + h - r, r, 90.0),
            straight to { t -> BoardPoint(c - h, c + h - r - t * straight) },
            arc to arcSegment(c - h + r, c - h + r, r, 180.0),
            straight to { t -> BoardPoint(c - h + r + t * straight, c - h) },
            arc to arcSegment(c + h - r, c - h + r, r, 270.0),
            straight to { t -> BoardPoint(c + h, c - h + r + t * straight) },
            arc to arcSegment(c + h - r, c + h - r, r, 0.0),
            straight / 2 to { t -> BoardPoint(c + h - r - t * straight / 2, c + h) }
        )
        val perimeter = segments.sumOf { it.first.toDouble() }.toFloat()
        val size = SafeSquareRules.TRACK_SIZE

        return (0 until size).map { id ->
            var distance = perimeter * id / size
            var point = segments.last().second(1f)
            for ((length, at) in segments) {
                if (distance <= length) {
                    point = at(distance / length)
                    break
                }
                distance -= length
            }
            BoardNode(
                id = id,
                x = point.x,
                y = point.y,
                isSafe = SafeSquareRules.isSafeTrackIndex(id),
                entryOwner = Player.entries.firstOrNull { SafeSquareRules.entryIndex(it) == id }
            )
        }
    }

    private fun arcSegment(cx: Float, cy: Float, r: Float, startDeg: Double): (Float) -> BoardPoint = { t ->
        val angle = Math.toRadians(startDeg + 90.0 * t)
        BoardPoint(cx + r * cos(angle).toFloat(), cy + r * sin(angle).toFloat())
    }
}
