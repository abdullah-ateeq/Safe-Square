package com.example.safesquare.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

enum class Sfx { DICE, MOVE, SAFE, CAPTURE, HOME, WIN }

/**
 * Restrained procedurally-synthesized sound effects. Clips are rendered once to small WAV
 * files in the cache directory and played through [SoundPool], so the APK ships no audio assets.
 */
class SoundEngine(context: Context) {
    private val appContext = context.applicationContext
    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()
    private val sampleIds = ConcurrentHashMap<Sfx, Int>()
    private val readySamples: MutableSet<Int> = ConcurrentHashMap.newKeySet()

    init {
        pool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) readySamples.add(sampleId)
        }
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val dir = File(appContext.cacheDir, "sfx").apply { mkdirs() }
            Sfx.entries.forEach { sfx ->
                val file = File(dir, "${sfx.name.lowercase()}_v1.wav")
                if (!file.exists()) file.writeBytes(wav(synthesize(sfx)))
                sampleIds[sfx] = pool.load(file.absolutePath, 1)
            }
        }
    }

    fun play(sfx: Sfx) {
        val id = sampleIds[sfx]?.takeIf { it in readySamples } ?: return
        val volume = when (sfx) {
            Sfx.MOVE -> 0.35f
            Sfx.DICE -> 0.55f
            else -> 0.7f
        }
        pool.play(id, volume, volume, 1, 0, 1f)
    }

    private fun synthesize(sfx: Sfx): FloatArray = when (sfx) {
        Sfx.DICE -> mix(
            480,
            listOf(0, 60, 125, 210, 300).map { ms -> ms to click(28, 2400f, 0.55f) }
        )
        Sfx.MOVE -> mix(60, listOf(0 to woodTick()))
        Sfx.SAFE -> mix(
            420,
            listOf(0 to bell(659.25f, 260, 0.32f), 110 to bell(987.77f, 300, 0.28f))
        )
        Sfx.CAPTURE -> mix(360, listOf(0 to thud(), 0 to click(40, 900f, 0.4f)))
        Sfx.HOME -> mix(
            520,
            listOf(0 to bell(523.25f, 220, 0.3f), 90 to bell(659.25f, 240, 0.3f), 180 to bell(783.99f, 320, 0.3f))
        )
        Sfx.WIN -> mix(
            1200,
            listOf(
                0 to bell(523.25f, 300, 0.26f),
                120 to bell(659.25f, 320, 0.26f),
                240 to bell(783.99f, 360, 0.26f),
                360 to bell(1046.5f, 800, 0.26f),
                360 to bell(523.25f, 800, 0.16f)
            )
        )
    }

    private fun samples(ms: Int) = SAMPLE_RATE * ms / 1000

    private fun mix(totalMs: Int, parts: List<Pair<Int, FloatArray>>): FloatArray {
        val out = FloatArray(samples(totalMs))
        parts.forEach { (offsetMs, clip) ->
            val start = samples(offsetMs)
            for (i in clip.indices) {
                val j = start + i
                if (j < out.size) out[j] += clip[i]
            }
        }
        // Short fade-out avoids clicks at the clip boundary.
        val fade = samples(12).coerceAtMost(out.size)
        for (i in 0 until fade) out[out.size - 1 - i] *= i.toFloat() / fade
        return out
    }

    private fun bell(freq: Float, ms: Int, amp: Float): FloatArray = FloatArray(samples(ms)) { i ->
        val t = i.toFloat() / SAMPLE_RATE
        val env = exp(-t * 7f) * minOf(1f, t * 400f)
        val wave = sin(2 * PI * freq * t) + 0.25 * sin(2 * PI * freq * 2.01 * t)
        (wave * env * amp).toFloat()
    }

    private fun click(ms: Int, tone: Float, amp: Float): FloatArray {
        val random = Random(tone.toInt())
        return FloatArray(samples(ms)) { i ->
            val t = i.toFloat() / SAMPLE_RATE
            val env = exp(-t * 180f)
            val noise = random.nextFloat() * 2f - 1f
            ((noise * 0.6f + sin(2 * PI * tone * t).toFloat() * 0.4f) * env * amp)
        }
    }

    private fun woodTick(): FloatArray = FloatArray(samples(45)) { i ->
        val t = i.toFloat() / SAMPLE_RATE
        val env = exp(-t * 120f)
        ((sin(2 * PI * 820 * t) * 0.7 + sin(2 * PI * 1640 * t) * 0.3) * env * 0.5).toFloat()
    }

    private fun thud(): FloatArray {
        var phase = 0.0
        return FloatArray(samples(300)) { i ->
            val t = i.toFloat() / SAMPLE_RATE
            val freq = 70 + 130 * exp(-t * 18.0)
            phase += 2 * PI * freq / SAMPLE_RATE
            (sin(phase) * exp(-t * 11.0) * 0.7).toFloat()
        }
    }

    private fun wav(data: FloatArray): ByteArray {
        val pcmBytes = data.size * 2
        val buffer = ByteBuffer.allocate(44 + pcmBytes).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray()).putInt(36 + pcmBytes).put("WAVE".toByteArray())
        buffer.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1)
            .putInt(SAMPLE_RATE).putInt(SAMPLE_RATE * 2).putShort(2).putShort(16)
        buffer.put("data".toByteArray()).putInt(pcmBytes)
        data.forEach { buffer.putShort((it.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()) }
        return buffer.array()
    }

    private companion object {
        const val SAMPLE_RATE = 22050
    }
}
