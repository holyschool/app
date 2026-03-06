package com.enderplusbayzuiship.edupage2.util

import android.content.Context
import android.content.res.Configuration
import com.enderplusbayzuiship.edupage2.data.AppLanguage
import java.util.Locale

/**
 * Wraps [base] with a locale-overridden [Context] so the app displays in the
 * language chosen in Settings.
 *
 * Call this from [android.app.Application.attachBaseContext] and
 * [android.app.Activity.attachBaseContext].
 *
 * When [AppLanguage.SYSTEM] is selected the base context is returned unchanged,
 * letting Android pick the locale from the device settings as usual.
 */
object LocaleHelper {

    /**
     * Read the saved language preference directly from SharedPreferences
     * (without Hilt, since DI is not yet ready during [attachBaseContext]).
     */
    fun wrap(base: Context): Context {
        val prefs = base.getSharedPreferences("edupage_app_prefs", Context.MODE_PRIVATE)
        val key = prefs.getString("app_language", AppLanguage.SYSTEM.key)
        val language = AppLanguage.fromKey(key)

        if (language == AppLanguage.SYSTEM || language.tag.isEmpty()) return base

        val locale = Locale.forLanguageTag(language.tag)
        Locale.setDefault(locale)

        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }
}
