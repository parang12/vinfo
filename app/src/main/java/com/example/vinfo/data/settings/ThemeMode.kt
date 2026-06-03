package com.example.vinfo.data.settings

enum class ThemeMode(val storedValue: String, val label: String) {
    LIGHT("light", "라이트 모드"),
    DARK("dark", "다크 모드"),
    SYSTEM("system", "시스템 기본값");

    companion object {
        fun fromStoredValue(value: String?): ThemeMode {
            return entries.firstOrNull { it.storedValue == value } ?: SYSTEM
        }
    }
}
