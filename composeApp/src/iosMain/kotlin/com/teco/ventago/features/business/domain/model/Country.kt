package com.teco.ventago.features.business.domain.model

import platform.Foundation.NSLocale
import platform.Foundation.NSLocaleCountryCode
import platform.Foundation.NSLocaleCurrencyCode
import platform.Foundation.commonISOCurrencyCodes
import platform.Foundation.componentsFromLocaleIdentifier
import platform.Foundation.localeIdentifierFromComponents
import platform.Foundation.systemLocale

actual open class Country actual constructor() {
    actual var country: String = ""
    actual var currencyCode: String = ""

    actual companion object {
        actual fun loadData(): ArrayList<Country> {
            var countries = ArrayList<Country>()
            for (item in NSLocale.commonISOCurrencyCodes()) {
                if (item == null) continue
                val countryName = NSLocale.systemLocale().displayNameForKey(NSLocaleCountryCode,  item)
                val countryCode = item as String

                val components = NSLocale.componentsFromLocaleIdentifier(countryCode)
                val identifier = NSLocale.localeIdentifierFromComponents(components)
                val locale = NSLocale(localeIdentifier = identifier)
                val currencyCode = locale.objectForKey(NSLocaleCurrencyCode)
                val country = Country()
                country.country = countryName as String
                country.currencyCode = currencyCode as String
                countries.add(country)
            }

            return countries
        }
    }


}