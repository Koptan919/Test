package com.noor.app.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

enum class AppLanguage(val tag: String) { SYSTEM(""), ARABIC("ar"), ENGLISH("en") }

/**
 * Stores the in-app language in SharedPreferences so it can be read synchronously in
 * attachBaseContext, then wraps contexts with the matching locale and layout direction.
 */
object LocaleManager {
    private const val PREFS = "noor_locale"
    private const val KEY = "language"

    fun get(context: Context): AppLanguage {
        val value = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        return AppLanguage.entries.firstOrNull { it.name == value } ?: AppLanguage.SYSTEM
    }

    fun set(context: Context, language: AppLanguage) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, language.name).commit()
    }

    fun wrap(context: Context): Context {
        val language = get(context)
        if (language == AppLanguage.SYSTEM) return context
        val locale = Locale.forLanguageTag(language.tag)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }

    fun isArabic(context: Context): Boolean =
        wrap(context).resources.configuration.locales[0].language == "ar"
}
