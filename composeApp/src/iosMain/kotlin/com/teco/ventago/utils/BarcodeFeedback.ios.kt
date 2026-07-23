package com.teco.ventago.utils

import platform.AudioToolbox.AudioServicesPlaySystemSound

actual fun playBarcodeScanBeep() {
    AudioServicesPlaySystemSound(1103u)
}
