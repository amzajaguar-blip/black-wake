import sys

with open("app/src/main/java/com/blackwake/game/SynthAudioEngine.kt", "r") as f:
    code = f.read()

state_old = """    var hasSiren: Boolean = false
    var isOxygenLow: Boolean = false
    var hullIntegrity: Float = 100f"""
state_new = """    var hasSiren: Boolean = false
    var isOxygenLow: Boolean = false
    var hullIntegrity: Float = 100f
    var isMuted: Boolean = false"""
code = code.replace(state_old, state_new)

fill_old = """            // Clamp and convert to short
            buffer[i] = (leftOut.coerceIn(-1f, 1f) * 32767).toInt().toShort()
            buffer[i + 1] = (rightOut.coerceIn(-1f, 1f) * 32767).toInt().toShort()
        }"""
fill_new = """            // Clamp and convert to short
            val finalLeft = if (isMuted) 0f else leftOut.coerceIn(-1f, 1f)
            val finalRight = if (isMuted) 0f else rightOut.coerceIn(-1f, 1f)
            buffer[i] = (finalLeft * 32767).toInt().toShort()
            buffer[i + 1] = (finalRight * 32767).toInt().toShort()
        }"""
code = code.replace(fill_old, fill_new)

pickup_old = """    fun playPickup() {
        // Simple ping"""
pickup_new = """    fun playPickup() {
        if (isMuted) return
        // Simple ping"""
code = code.replace(pickup_old, pickup_new)

explosion_old = """    fun playExplosion() {
        // Noise burst"""
explosion_new = """    fun playExplosion() {
        if (isMuted) return
        // Noise burst"""
code = code.replace(explosion_old, explosion_new)

tone_old = """    fun playTone(freq: Float, durationSec: Float) {
        CoroutineScope(Dispatchers.IO).launch {"""
tone_new = """    fun playTone(freq: Float, durationSec: Float) {
        if (isMuted) return
        CoroutineScope(Dispatchers.IO).launch {"""
code = code.replace(tone_old, tone_new)

with open("app/src/main/java/com/blackwake/game/SynthAudioEngine.kt", "w") as f:
    f.write(code)

