import sys

with open("app/src/main/java/com/blackwake/game/SynthAudioEngine.kt", "r") as f:
    code = f.read()

state_old = """    var hasSiren: Boolean = false"""
state_new = """    var hasSiren: Boolean = false
    var isOxygenLow: Boolean = false"""
code = code.replace(state_old, state_new)

phase_old = """    private var phaseSiren = 0f
    private var sirenTimer = 0f"""
phase_new = """    private var phaseSiren = 0f
    private var sirenTimer = 0f
    private var phaseO2 = 0f
    private var o2Timer = 0f"""
code = code.replace(phase_old, phase_new)

mix_old = """            // Siren
            var sirenSample = 0f
            if (hasSiren) {
                sirenTimer += 1f / SAMPLE_RATE
                val sirenFreq = 600f + kotlin.math.sin(sirenTimer * 5f) * 200f
                phaseSiren += (sirenFreq * 2f * Math.PI / SAMPLE_RATE).toFloat()
                sirenSample = sin(phaseSiren).toFloat() * 0.15f
            }
            
            // Mix
            val mixed = engineSample + noise + bassSample + pursuerSample + sirenSample"""
mix_new = """            // Siren
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
            
            // Mix
            val mixed = engineSample + noise + bassSample + pursuerSample + sirenSample + o2Sample"""
code = code.replace(mix_old, mix_new)

with open("app/src/main/java/com/blackwake/game/SynthAudioEngine.kt", "w") as f:
    f.write(code)

print("SynthAudioEngine patched for O2 warning")
