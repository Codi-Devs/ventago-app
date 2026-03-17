package com.teco.ventago.core.beta

/**
 * Known beta feature keys. Extend as new beta features are added.
 */
enum class BetaFeature(val key: String) {
    QUOTES("quotes"),
    RECURRING_INVOICING("recurring_invoicing"),
    MULTI_USERS("multi_users"),
    EXPENSES_QR("expenses_qr"),
    EXPENSES_OCR("expenses_ocr");

    companion object {
        fun fromKey(key: String): BetaFeature? = entries.firstOrNull { it.key == key }
    }
}
