package com.teco.ventago.core

import android.content.Context
import com.fingerprintjs.android.fingerprint.Fingerprinter
import com.fingerprintjs.android.fingerprint.FingerprinterFactory

actual open class FingerPrintService(context: Context) {
    private var fingerprint = "NONE"

    init {
        val fingerprinter = FingerprinterFactory.create(context)
        fingerprinter.getFingerprint(version = Fingerprinter.Version.V_5) { fingerprint ->
            this.fingerprint = fingerprint
        }
    }

    actual fun getFingerPrint(): String {
        return fingerprint
    }
}