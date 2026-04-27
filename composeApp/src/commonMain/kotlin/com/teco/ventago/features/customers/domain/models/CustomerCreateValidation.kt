package com.teco.ventago.features.customers.domain.models

import com.teco.ventago.features.invoicing.domain.models.FeCustomerType

object CustomerCreateValidation {
    fun requiredLocationMessage(
        customerType: FeCustomerType,
        addressLine: String?,
        province: String?,
        district: String?,
        corregimiento: String?
    ): String? {
        if (customerType == FeCustomerType.FOREIGNER) return null
        if (province.isNullOrBlank()) return "La provincia es requerida."
        if (district.isNullOrBlank()) return "El distrito es requerido."
        if (corregimiento.isNullOrBlank()) return "El corregimiento es requerido."
        if (addressLine.isNullOrBlank()) return "La direccion es requerida."
        return null
    }
}

fun String?.nullIfBlank(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
