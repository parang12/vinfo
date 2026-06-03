package com.example.vinfo.data.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThemeSettingsStore(context: Context) {
    private val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val _themeMode = MutableStateFlow(readThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun saveThemeMode(mode: ThemeMode): Boolean {
        val saved = sharedPreferences.edit().putString(KEY_THEME_MODE, mode.storedValue).commit()
        if (saved) {
            _themeMode.value = mode
        }
        return saved
    }

    private fun readThemeMode(): ThemeMode {
        return ThemeMode.fromStoredValue(sharedPreferences.getString(KEY_THEME_MODE, null))
    }

    companion object {
        private const val PREF_NAME = "vinfo_theme_settings"
        private const val KEY_THEME_MODE = "theme_mode"
    }
}
