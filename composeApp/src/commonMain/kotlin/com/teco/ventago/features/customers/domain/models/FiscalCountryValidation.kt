package com.teco.ventago.features.customers.domain.models

import com.teco.ventago.features.customers.ui.form.viewmodel.CustomerCountries

object FiscalCountryValidation {
    fun normalize(country: String?): String = country.orEmpty().trim().uppercase()

    fun error(country: String?, foreignReceiver: Boolean = false, foreignDestination: Boolean = false): String? {
        if (!foreignReceiver && !foreignDestination) return null
        val code = normalize(country)
        if (code.isEmpty()) return "Selecciona el país del receptor extranjero"
        if (CustomerCountries.options.none { it.code == code }) return "Selecciona un país válido del catálogo fiscal"
        if (code == "PA") return if (foreignDestination) {
            "El país del receptor no puede ser Panamá para una operación en el extranjero"
        } else {
            "El país del cliente extranjero no puede ser Panamá"
        }
        return null
    }
}
