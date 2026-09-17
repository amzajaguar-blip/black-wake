import sys

with open("app/src/main/java/com/blackwake/game/SynthAudioEngine.kt", "r") as f:
    code = f.read()

old_state = """    // State
    var engineRpm: Float = 0f // 0 to 1
    var isBoosting: Boolean = false
    var detectionLevel: Float = 0f
    
    private var phaseEngine = 0f
    private var phaseWind = 0f
    private var phaseBass = 0f"""
    
new_state = """    // State
    var engineRpm: Float = 0f // 0 to 1
    var isBoosting: Boolean = false
    var detectionLevel: Float = 0f
    var pursuerCount: Int = 0
    var hasSiren: Boolean = false
    
    private var phaseEngine = 0f
    private var phaseWind = 0f
    private var phaseBass = 0f
    private var phasePursuer = 0f
    private var phaseSiren = 0f
    private var sirenTimer = 0f"""

code = code.replace(old_state, new_state)

old_mix = """            // Mix
            val mixed = engineSample + noise + bassSample"""

new_mix = """            // Pursuer Engine
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
            
            // Mix
            val mixed = engineSample + noise + bassSample + pursuerSample + sirenSample"""
            
code = code.replace(old_mix, new_mix)

with open("app/src/main/java/com/blackwake/game/SynthAudioEngine.kt", "w") as f:
    f.write(code)

print("SynthAudioEngine patched")
