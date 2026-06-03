package com.example.vinfo.data.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeTest {
    @Test
    fun `fromStoredValue resolves known theme modes`() {
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromStoredValue("light"))
        assertEquals(ThemeMode.DARK, ThemeMode.fromStoredValue("dark"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStoredValue("system"))
    }

    @Test
    fun `fromStoredValue falls back to system mode`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStoredValue(null))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStoredValue("unexpected"))
    }
}
