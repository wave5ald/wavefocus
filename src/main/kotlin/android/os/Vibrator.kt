package android.os

/** Desktop stand-ins for Android vibration APIs (no-ops on a PC). */
class Vibrator {
    fun vibrate(milliseconds: Long) = Unit
    fun vibrate(pattern: LongArray, repeat: Int) = Unit
    fun vibrate(effect: VibrationEffect) = Unit
    fun cancel() = Unit
}

class VibratorManager {
    val defaultVibrator: Vibrator = Vibrator()
}

class VibrationEffect private constructor(val pattern: LongArray, val repeat: Int) {
    companion object {
        fun createWaveform(timings: LongArray, repeat: Int): VibrationEffect = VibrationEffect(timings, repeat)
        fun createOneShot(milliseconds: Long, amplitude: Int): VibrationEffect =
            VibrationEffect(longArrayOf(0, milliseconds), -1)
    }
}

val VibratorImpl = Vibrator()
val VibratorManagerImpl = VibratorManager()
