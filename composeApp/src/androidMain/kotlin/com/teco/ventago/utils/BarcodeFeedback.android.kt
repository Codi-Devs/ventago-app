package com.teco.ventago.utils

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper

actual fun playBarcodeScanBeep() {
    runCatching {
        val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
        Handler(Looper.getMainLooper()).postDelayed({ toneGenerator.release() }, 180)
    }
}
