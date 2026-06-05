package com.teco.ventago.features.pos.ui.customer.add.viewmodel

internal const val MIN_ADDRESS_NON_BLANK_CHARACTERS = 5

internal fun hasMinimumAddressCharacters(addressLine: String?): Boolean {
    if (addressLine == null) {
        return false
    }

    return addressLine.count { !it.isWhitespace() } >= MIN_ADDRESS_NON_BLANK_CHARACTERS
}

