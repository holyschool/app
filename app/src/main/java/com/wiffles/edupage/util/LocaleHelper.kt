package com.wiffles.edupage.util

import android.content.Context
import android.content.res.Configuration
import com.wiffles.edupage.data.AppLanguage
import java.util.Locale

object LocaleHelper {

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

