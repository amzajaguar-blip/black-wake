package com.blackwake.game

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.*
import kotlin.math.sin

object SynthAudioEngine {
    private const val SAMPLE_RATE = 22050
    private var track: AudioTrack? = null
    private var isPlaying = false
    private val buffer = ShortArray(2048)
    
    // State
    var engineRpm: Float = 0f // 0 to 1
    var isBoosting: Boolean = false
    var detectionLevel: Float = 0f
    var pursuerCount: Int = 0
    var hasSiren: Boolean = false
    var isOxygenLow: Boolean = false
    var hullIntegrity: Float = 100f
    var isMuted: Boolean = false
    
    private var phaseEngine = 0f
    private var phaseWind = 0f
    private var phaseBass = 0f
    private var phasePursuer = 0f
    private var phaseSiren = 0f
    private var sirenTimer = 0f
    private var phaseO2 = 0f
    private var o2Timer = 0f
    private var phaseRumble = 0f
    private var phaseCreak = 0f
    private var creakTimer = 0f
    
    private val rand = java.util.Random()
    
    fun start() {
        if (isPlaying) return
        val minSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        track = AudioTrack(
            AudioManager.STREAM_MUSIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT,
            minSize,
            AudioTrack.MODE_STREAM
        )
        
        isPlaying = true
        track?.play()
        
        CoroutineScope(Dispatchers.IO).launch {
            while (isPlaying) {
                fillBuffer()
                track?.write(buffer, 0, buffer.size)
            }
        }
    }
    
    fun stop() {
        isPlaying = false
        track?.stop()
        track?.release()
        track = null
    }
    
    private fun fillBuffer() {
        for (i in 0 until buffer.size step 2) {
            // Engine tone (Sawtooth-ish)
            val baseFreq = 60f + (engineRpm * 100f)
            val freq = if (isBoosting) baseFreq * 1.5f else baseFreq
            phaseEngine += (freq * 2f * Math.PI / SAMPLE_RATE).toFloat()
            if (phaseEngine > 2f * Math.PI) phaseEngine -= (2f * Math.PI).toFloat()
            val engineSample = ((phaseEngine / (2f * Math.PI)) * 2f - 1f).toFloat() * 0.3f
            
            // Wind / Sea noise
            val windIntensity = 0.1f + (engineRpm * 0.4f)
            val noise = (rand.nextFloat() * 2f - 1f) * windIntensity
            
            // Tension Bass (Heartbeat / Chase pulse)
            val bassFreq = 30f + (detectionLevel * 40f)
            phaseBass += (bassFreq * 2f * Math.PI / SAMPLE_RATE).toFloat()
            val bassSample = sin(phaseBass) * (detectionLevel * 0.5f)
            
            // Pursuer Engine
            var pursuerSample = 0f
            if (pursuerCount > 0) {
                phasePursuer += (40f * 2f * Math.PI / SAMPLE_RATE).toFloat()
                if (phasePursuer > 2f * Math.PI) phasePursuer -= (2f * Math.PI).toFloat()
                pursuerSample = ((phasePursuer / (2f * Math.PI)) * 2f - 1f).toFloat() * 0.2f * pursuerCount
            }
            
            // Siren
            var sirenSample = 0f
            if (hasSiren) {
                sirenTimer += 1f / SAMPLE_RATE
                val sirenFreq = 600f + kotlin.math.sin(sirenTimer * 5f) * 200f
                phaseSiren += (sirenFreq * 2f * Math.PI / SAMPLE_RATE).toFloat()
                sirenSample = sin(phaseSiren).toFloat() * 0.15f
            }
            
            // Low Oxygen Warning (Low-frequency pulse)
            var o2Sample = 0f
            if (isOxygenLow) {
                o2Timer += 1f / SAMPLE_RATE
                // Pulse twice a second
                val pulse = (sin(o2Timer * Math.PI * 4f) * 0.5f + 0.5f).toFloat()
                phaseO2 += (150f * 2f * Math.PI / SAMPLE_RATE).toFloat()
                o2Sample = sin(phaseO2).toFloat() * 0.4f * pulse
            }
            
            // Hull Damage Spatial Audio (Rumbles and Creaks)
            val damageIntensity = (1f - (hullIntegrity / 100f)).coerceIn(0f, 1f)
            
            // Low frequency rumble (35Hz) increasing with damage
            phaseRumble += (35f * 2f * Math.PI / SAMPLE_RATE).toFloat()
            if (phaseRumble > 2f * Math.PI) phaseRumble -= (2f * Math.PI).toFloat()
            val rumbleBase = kotlin.math.sin(phaseRumble).toFloat() * damageIntensity * 0.7f
            // Spatial pan for rumble
            creakTimer += 1f / SAMPLE_RATE
            val rumblePan = (kotlin.math.sin(creakTimer * 1.5f) * 0.5f + 0.5f).toFloat() 
            
            // Water pressure creaks (structural stress) when highly damaged
            var creakSample = 0f
            var creakPan = 0.5f
            if (damageIntensity > 0.4f) {
                // Irregular pulsing LFO for creaks
                val creakLfo = (kotlin.math.sin(creakTimer * 0.8f) * 0.5f + 0.5f).toFloat()
                if (creakLfo > 0.85f) {
                    val intensity = (creakLfo - 0.85f) * (1f/0.15f) * damageIntensity
                    phaseCreak += (180f * 2f * Math.PI / SAMPLE_RATE).toFloat()
                    if (phaseCreak > 2f * Math.PI) phaseCreak -= (2f * Math.PI).toFloat()
                    val saw = ((phaseCreak / (2f * Math.PI)) * 2f - 1f).toFloat()
                    creakSample = saw * rand.nextFloat() * intensity * 0.6f
                    creakPan = (kotlin.math.sin(creakTimer * 12f) * 0.5f + 0.5f).toFloat()
                }
            }

            // Mix
            val baseMonoMix = engineSample + noise + bassSample + pursuerSample + sirenSample + o2Sample
            
            val leftOut = baseMonoMix + rumbleBase * (1f - rumblePan) + creakSample * (1f - creakPan)
            val rightOut = baseMonoMix + rumbleBase * rumblePan + creakSample * creakPan
            
            // Clamp and convert to short
            val finalLeft = if (isMuted) 0f else leftOut.coerceIn(-1f, 1f)
            val finalRight = if (isMuted) 0f else rightOut.coerceIn(-1f, 1f)
            buffer[i] = (finalLeft * 32767).toInt().toShort()
            buffer[i + 1] = (finalRight * 32767).toInt().toShort()
        }
    }
    
    // Fire-and-forget SFX
    fun playPickup() {
        if (isMuted) return
        // Simple ping
        playTone(880f, 0.1f)
    }
    
    fun playExplosion() {
        if (isMuted) return
        // Noise burst
        CoroutineScope(Dispatchers.IO).launch {
            val t = AudioTrack(
                AudioManager.STREAM_MUSIC, SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, 4096, AudioTrack.MODE_STREAM
            )
            t.play()
            val b = ShortArray(4096)
            for (i in b.indices) {
                val envelope = 1f - (i.toFloat() / b.size)
                b[i] = ((rand.nextFloat() * 2f - 1f) * 32767 * envelope).toInt().toShort()
            }
            t.write(b, 0, b.size)
            t.stop()
            t.release()
        }
    }
    
    fun playTone(freq: Float, durationSec: Float) {
        if (isMuted) return
        CoroutineScope(Dispatchers.IO).launch {
            val t = AudioTrack(
                AudioManager.STREAM_MUSIC, SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, 2048, AudioTrack.MODE_STREAM
            )
            t.play()
            val b = ShortArray((SAMPLE_RATE * durationSec).toInt())
            var p = 0f
            for (i in b.indices) {
                val envelope = 1f - (i.toFloat() / b.size)
                p += (freq * 2f * Math.PI / SAMPLE_RATE).toFloat()
                b[i] = (sin(p) * 32767 * 0.5f * envelope).toInt().toShort()
            }
            t.write(b, 0, b.size)
            t.stop()
            t.release()
        }
    }
}
