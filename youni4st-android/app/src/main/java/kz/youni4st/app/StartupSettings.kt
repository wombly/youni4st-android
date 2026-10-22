package kz.youni4st.app

object StartupSettings {
    // Change this value to adjust the background/lock timeout. 0 disables replay.
    const val RESTART_AFTER_BACKGROUND_MS = 5 * 60 * 1000L
    const val INTRO_DURATION_MS = 2000
    fun shouldReplay(elapsed:Long) = RESTART_AFTER_BACKGROUND_MS>0 && elapsed>=RESTART_AFTER_BACKGROUND_MS
}

/** A remounted account UI must not replay the same background-return animation. */
class IntroReplayGate {
    private var handled=0
    fun consume(signal:Int):Boolean {
        if(signal==0){handled=0;return false}
        if(signal<=handled)return false
        handled=signal;return true
    }
}

/** Brief fading feedback, independent of loading and below the intro duration. */
object StartupHaptics {
    fun play(context:android.content.Context) {
        val vibrator=if(android.os.Build.VERSION.SDK_INT>=31)context.getSystemService(android.os.VibratorManager::class.java)?.defaultVibrator else context.getSystemService(android.os.Vibrator::class.java)
        if(vibrator?.hasVibrator()!=true)return
        runCatching {
            if(vibrator.hasAmplitudeControl())vibrator.vibrate(android.os.VibrationEffect.createWaveform(timings,amplitudes,-1))
            else vibrator.vibrate(android.os.VibrationEffect.createWaveform(fallbackTimings,-1))
        }
    }
    fun cancel(context:android.content.Context) {
        if(android.os.Build.VERSION.SDK_INT>=31)context.getSystemService(android.os.VibratorManager::class.java)?.defaultVibrator?.cancel()
        else context.getSystemService(android.os.Vibrator::class.java)?.cancel()
    }
    val timings get()=longArrayOf(0,40,60,80,100)
    val amplitudes get()=intArrayOf(0,110,80,45,15)
    val fallbackTimings get()=longArrayOf(0,35,245)
}
