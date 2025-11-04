package com.teco.ventago.features.business.domain.model

import java.util.Locale
import java.util.TreeMap

actual open class Country actual constructor() {
    actual var country: String = ""
    actual var currencyCode: String = ""

    actual companion object {
        actual fun loadData(): ArrayList<Country> {
            val locales = Locale.getAvailableLocales()

            // We use TreeMap so that the order of the data in the map sorted
            // based on the country name.

            // We use TreeMap so that the order of the data in the map sorted
            // based on the country name.
            val data: MutableMap<String, String> = TreeMap()
            for (locale in locales) {
                try {
                    data[locale.displayCountry] =
                        java.util.Currency.getInstance(locale).currencyCode
                } catch (e: java.lang.Exception) {
                    // when the locale is not supported
                }
            }

            val countries = ArrayList<Country>()
            for (item in data) {
                val country = Country()
                country.country = item.key
                country.currencyCode = item.value
                countries.add(country)
            }
            return countries
        }
    }


}