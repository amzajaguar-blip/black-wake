package com.blackwake.game

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import kotlin.math.max
import kotlin.math.sin

/**
 * Procedural sound bed (engine, sea, tension bass, pursuers, siren, oxygen alarm, hull stress) plus
 * short one-shot effects, mixed on a single audio thread into a single [AudioTrack].
 *
 * Parameters are written from the main thread and read by the audio thread, hence @Volatile.
 * All oscillator phases and LFO clocks wrap, so the sound stays clean in arbitrarily long sessions.
 */
object SynthAudioEngine {
    private const val SAMPLE_RATE = 22050
    private const val FRAMES = 512
    private const val MAX_VOICES = 12
    private const val PI_F = 3.1415927f
    private const val TWO_PI = 6.2831855f
    private const val DT = 1f / SAMPLE_RATE

    @Volatile var engineLevel = 0f
    @Volatile var boosting = false
    @Volatile var detection = 0f
    @Volatile var pursuers = 0
    @Volatile var hullRatio = 1f
    @Volatile var oxygenLow = false
    @Volatile var siren = false
    /** The bed plays only during a mission; it fades in and out instead of cutting. */
    @Volatile var bedEnabled = false
    @Volatile var muted = false

    @Volatile private var running = false
    @Volatile private var paused = false
    private var thread: Thread? = null

    private class Voice(val noise: Boolean, val freq: Float, val gain: Float, val total: Int) {
        var remaining = total
        var phase = 0f
    }

    private val pending = ArrayList<Voice>()
    private val voices = ArrayList<Voice>()

    @Synchronized
    fun start() {
        if (running) return
        running = true
        paused = false
        thread = Thread(::loop, "BlackWakeAudio").apply {
            isDaemon = true
            start()
        }
    }

    @Synchronized
    fun stop() {
        running = false
        thread = null
    }

    fun pause() {
        paused = true
    }

    fun resume() {
        paused = false
    }

    fun playTone(freq: Float, seconds: Float, gain: Float) = enqueue(Voice(false, freq, gain, (seconds * SAMPLE_RATE).toInt()))

    fun playNoise(seconds: Float, gain: Float) = enqueue(Voice(true, 0f, gain, (seconds * SAMPLE_RATE).toInt()))

    private fun enqueue(voice: Voice) {
        if (muted || paused || !running || voice.total <= 0) return
        synchronized(pending) {
            if (pending.size < MAX_VOICES) pending += voice
        }
    }

    private fun loop() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
        val track = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(
                    max(AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT), FRAMES * 8)
                )
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (e: Exception) {
            // Audio is best-effort: a device without a usable output keeps the game running silently.
            running = false
            return
        }

        val buffer = ShortArray(FRAMES * 2)
        var playing = false
        try {
            if (track.state != AudioTrack.STATE_INITIALIZED) return
            while (running) {
                if (paused) {
                    if (playing) {
                        track.pause()
                        track.flush()
                        playing = false
                    }
                    Thread.sleep(40)
                    continue
                }
                if (!playing) {
                    track.play()
                    playing = true
                }
                render(buffer)
                track.write(buffer, 0, buffer.size)
            }
        } catch (e: Exception) {
            running = false
        } finally {
            try {
                if (playing) track.stop()
            } catch (e: IllegalStateException) {
                // Already stopped.
            }
            track.release()
        }
    }

    // Audio-thread state.
    private var bedGain = 0f
    private var phEngine = 0f
    private var engineLp = 0f
    private var noiseLp = 0f
    private var phBass = 0f
    private var phPursuer = 0f
    private var phSiren = 0f
    private var sirenLfo = 0f
    private var phO2 = 0f
    private var o2Lfo = 0f
    private var phRumble = 0f
    private var panLfo = 0f
    private var creakLfo = 0f
    private var creakPanLfo = 0f
    private var phCreak = 0f
    private var noiseSeed = 0x2545F491

    private fun render(out: ShortArray) {
        synchronized(pending) {
            if (pending.isNotEmpty()) {
                voices.addAll(pending)
                pending.clear()
            }
        }

        val engine = engineLevel.coerceIn(0f, 1f)
        val tension = detection.coerceIn(0f, 1f)
        val chasers = pursuers.coerceIn(0, 3)
        val damage = (1f - hullRatio).coerceIn(0f, 1f)
        val alarm = siren
        val lowO2 = oxygenLow
        val bedTarget = if (bedEnabled && !muted) 1f else 0f
        val sfxGain = if (muted) 0f else 1f

        val engineInc = TWO_PI * (55f + engine * 90f) * (if (boosting) 1.35f else 1f) * DT
        val bassInc = TWO_PI * (32f + tension * 30f) * DT
        val pursuerInc = TWO_PI * 42f * DT
        val o2Inc = TWO_PI * 150f * DT
        val rumbleInc = TWO_PI * 35f * DT
        val creakInc = TWO_PI * 180f * DT

        for (i in 0 until FRAMES) {
            bedGain += (bedTarget - bedGain) * 0.0005f

            phEngine += engineInc
            if (phEngine >= TWO_PI) phEngine -= TWO_PI
            engineLp += ((phEngine / PI_F - 1f) - engineLp) * 0.08f
            var bed = engineLp * (0.10f + engine * 0.18f)

            noiseLp += (nextNoise() - noiseLp) * 0.05f
            bed += noiseLp * (0.35f + engine * 0.35f)

            phBass += bassInc
            if (phBass >= TWO_PI) phBass -= TWO_PI
            bed += sin(phBass) * tension * tension * 0.35f

            if (chasers > 0) {
                phPursuer += pursuerInc
                if (phPursuer >= TWO_PI) phPursuer -= TWO_PI
                bed += (phPursuer / PI_F - 1f) * 0.06f * chasers
            }

            if (alarm) {
                sirenLfo += DT
                if (sirenLfo >= SIREN_LFO_PERIOD) sirenLfo -= SIREN_LFO_PERIOD
                phSiren += TWO_PI * (600f + sin(sirenLfo * 5f) * 200f) * DT
                if (phSiren >= TWO_PI) phSiren -= TWO_PI
                bed += sin(phSiren) * 0.08f
            }

            if (lowO2) {
                o2Lfo += DT
                if (o2Lfo >= 0.5f) o2Lfo -= 0.5f
                phO2 += o2Inc
                if (phO2 >= TWO_PI) phO2 -= TWO_PI
                bed += sin(phO2) * 0.2f * (sin(o2Lfo * 4f * PI_F) * 0.5f + 0.5f)
            }

            // Hull stress: a wandering low rumble and, past 40% damage, irregular creaks.
            phRumble += rumbleInc
            if (phRumble >= TWO_PI) phRumble -= TWO_PI
            panLfo += DT
            if (panLfo >= PAN_LFO_PERIOD) panLfo -= PAN_LFO_PERIOD
            val rumble = sin(phRumble) * damage * 0.35f
            val rumblePan = sin(panLfo * 1.5f) * 0.5f + 0.5f
            var creak = 0f
            var creakPan = 0.5f
            if (damage > 0.4f) {
                creakLfo += DT
                if (creakLfo >= CREAK_LFO_PERIOD) creakLfo -= CREAK_LFO_PERIOD
                val lfo = sin(creakLfo * 0.8f) * 0.5f + 0.5f
                if (lfo > 0.85f) {
                    phCreak += creakInc
                    if (phCreak >= TWO_PI) phCreak -= TWO_PI
                    creakPanLfo += DT
                    if (creakPanLfo >= CREAK_PAN_PERIOD) creakPanLfo -= CREAK_PAN_PERIOD
                    creak = (phCreak / PI_F - 1f) * (nextNoise() * 0.5f + 0.5f) * (lfo - 0.85f) / 0.15f * damage * 0.3f
                    creakPan = sin(creakPanLfo * 12f) * 0.5f + 0.5f
                }
            }

            var sfx = 0f
            for (v in voices) {
                if (v.remaining <= 0) continue
                val envelope = v.remaining.toFloat() / v.total
                sfx += if (v.noise) {
                    nextNoise() * v.gain * envelope
                } else {
                    v.phase += TWO_PI * v.freq * DT
                    if (v.phase >= TWO_PI) v.phase -= TWO_PI
                    sin(v.phase) * v.gain * envelope
                }
                v.remaining--
            }

            val left = (bed + rumble * (1f - rumblePan) + creak * (1f - creakPan)) * bedGain + sfx * sfxGain
            val right = (bed + rumble * rumblePan + creak * creakPan) * bedGain + sfx * sfxGain
            out[2 * i] = (softClip(left) * 32767f).toInt().toShort()
            out[2 * i + 1] = (softClip(right) * 32767f).toInt().toShort()
        }
        voices.removeAll { it.remaining <= 0 }
    }

    private const val SIREN_LFO_PERIOD = TWO_PI / 5f
    private const val PAN_LFO_PERIOD = TWO_PI / 1.5f
    private const val CREAK_LFO_PERIOD = TWO_PI / 0.8f
    private const val CREAK_PAN_PERIOD = TWO_PI / 12f

    /** Cubic soft clipper: transparent at low levels, no hard edges when the mix gets loud. */
    private fun softClip(x: Float): Float {
        val c = (x * 0.7f).coerceIn(-1f, 1f)
        return 1.5f * c - 0.5f * c * c * c
    }

    private fun nextNoise(): Float {
        var x = noiseSeed
        x = x xor (x shl 13)
        x = x xor (x ushr 17)
        x = x xor (x shl 5)
        noiseSeed = x
        return (x and 0xFFFF) / 32767.5f - 1f
    }
}
