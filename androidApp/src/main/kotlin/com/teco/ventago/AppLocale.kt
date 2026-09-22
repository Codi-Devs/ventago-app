package com.teco.ventago

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object AppLocale {
    const val LANGUAGE_TAG = "es"

    fun apply() {
        val locale = Locale.forLanguageTag(LANGUAGE_TAG)
        Locale.setDefault(locale)
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(LANGUAGE_TAG))
    }

    fun wrap(base: Context): Context {
        val locale = Locale.forLanguageTag(LANGUAGE_TAG)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLocales(LocaleList(locale))
        return base.createConfigurationContext(config)
    }
}
