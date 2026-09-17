package com.blackwake.game

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import kotlin.math.abs

/**
 * Crossfades the three soundtrack beds (route, pursuit, uplink) according to the mission state.
 * Players are created lazily, loop forever and keep their position while silent.
 * Must be driven from the main thread.
 */
class MusicDirector(context: Context) {
    enum class Mood { ROUTE, PURSUIT, UPLINK }

    private val app = context.applicationContext
    private val tracks = intArrayOf(R.raw.music_route, R.raw.music_pursuit, R.raw.music_uplink)
    private val players = arrayOfNulls<MediaPlayer>(tracks.size)
    private val prepared = BooleanArray(tracks.size)
    private val failed = BooleanArray(tracks.size)
    private val levels = FloatArray(tracks.size)
    private var paused = false

    /**
     * @param volume overall music level for the current screen, 0..1.
     */
    fun update(dt: Float, mood: Mood, volume: Float, muted: Boolean) {
        for (i in tracks.indices) {
            val target = if (!paused && !muted && i == mood.ordinal) volume else 0f
            val diff = target - levels[i]
            val stepSize = dt * FADE_PER_SECOND
            levels[i] = if (abs(diff) <= stepSize) target else levels[i] + stepSize * (if (diff > 0f) 1f else -1f)
            apply(i)
        }
    }

    fun pause() {
        paused = true
        for (i in tracks.indices) {
            levels[i] = 0f
            apply(i)
        }
    }

    fun resume() {
        paused = false
    }

    fun release() {
        for (i in players.indices) {
            players[i]?.release()
            players[i] = null
            prepared[i] = false
        }
    }

    private fun apply(index: Int) {
        val level = levels[index]
        val player = players[index] ?: if (level > 0f) create(index) else null
        player ?: return
        if (!prepared[index]) return
        try {
            player.setVolume(level, level)
            if (level > 0f && !player.isPlaying) player.start()
            if (level <= 0f && player.isPlaying) player.pause()
        } catch (e: IllegalStateException) {
            player.release()
            players[index] = null
            prepared[index] = false
            failed[index] = true
        }
    }

    private fun create(index: Int): MediaPlayer? {
        if (failed[index]) return null
        return try {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                app.resources.openRawResourceFd(tracks[index]).use { fd ->
                    setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                }
                isLooping = true
                setVolume(0f, 0f)
                setOnErrorListener { mp, _, _ ->
                    mp.release()
                    players[index] = null
                    prepared[index] = false
                    failed[index] = true
                    true
                }
                // Asynchronous: decoding a multi-megabyte track on the main thread stalls the frame.
                setOnPreparedListener {
                    prepared[index] = true
                    apply(index)
                }
                prepareAsync()
            }.also { players[index] = it }
        } catch (e: Exception) {
            failed[index] = true
            null
        }
    }

    companion object {
        private const val FADE_PER_SECOND = 0.6f

        fun moodFor(run: RunState, sector: Sector): Mood = when {
            sector.extract -> Mood.UPLINK
            run.activePursuers > 0 || run.detection > 0.64f -> Mood.PURSUIT
            else -> Mood.ROUTE
        }

        /** Keeps the score under the radio and effects, as the soundtrack design asks. */
        fun volumeFor(mode: GameMode): Float = when (mode) {
            GameMode.RUNNING -> 0.45f
            GameMode.PAUSED -> 0.2f
            else -> 0.35f
        }
    }
}
