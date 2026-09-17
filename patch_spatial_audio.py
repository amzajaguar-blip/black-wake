import sys

with open("app/src/main/java/com/blackwake/game/SynthAudioEngine.kt", "r") as f:
    code = f.read()

state_old = """    var hasSiren: Boolean = false
    var isOxygenLow: Boolean = false"""
state_new = """    var hasSiren: Boolean = false
    var isOxygenLow: Boolean = false
    var hullIntegrity: Float = 100f"""
code = code.replace(state_old, state_new)

vars_old = """    private var phaseO2 = 0f
    private var o2Timer = 0f"""
vars_new = """    private var phaseO2 = 0f
    private var o2Timer = 0f
    private var phaseRumble = 0f
    private var phaseCreak = 0f
    private var creakTimer = 0f"""
code = code.replace(vars_old, vars_new)


init_old = """    private val buffer = ShortArray(1024)"""
init_new = """    private val buffer = ShortArray(2048)"""
code = code.replace(init_old, init_new)

start_old = """        val minSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        track = AudioTrack(
            AudioManager.STREAM_MUSIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,"""
start_new = """        val minSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        track = AudioTrack(
            AudioManager.STREAM_MUSIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_STEREO,"""
code = code.replace(start_old, start_new)

fill_old = """        for (i in buffer.indices) {
            // Engine tone (Sawtooth-ish)"""
fill_new = """        for (i in 0 until buffer.size step 2) {
            // Engine tone (Sawtooth-ish)"""
code = code.replace(fill_old, fill_new)

mix_old = """            // Mix
            val mixed = engineSample + noise + bassSample + pursuerSample + sirenSample + o2Sample
            
            // Clamp and convert to short
            val finalSample = (mixed.coerceIn(-1f, 1f) * 32767).toInt().toShort()
            buffer[i] = finalSample
        }"""
mix_new = """            // Hull Damage Spatial Audio (Rumbles and Creaks)
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
            buffer[i] = (leftOut.coerceIn(-1f, 1f) * 32767).toInt().toShort()
            buffer[i + 1] = (rightOut.coerceIn(-1f, 1f) * 32767).toInt().toShort()
        }"""
code = code.replace(mix_old, mix_new)

with open("app/src/main/java/com/blackwake/game/SynthAudioEngine.kt", "w") as f:
    f.write(code)

print("Spatial audio patched in SynthAudioEngine")
