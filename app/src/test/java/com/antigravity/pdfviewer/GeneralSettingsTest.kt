package com.antigravity.pdfviewer

import com.antigravity.pdfviewer.domain.model.AppTheme
import com.antigravity.pdfviewer.domain.model.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeneralSettingsTest {

    @Test
    fun testDefaultGeneralSettingsMatchesReference() {
        val settings = UserSettings()

        // Verify default toggle states match Image 1
        assertTrue(settings.useSingleLineToolbar)
        assertTrue(settings.fullScreenMode)
        assertTrue(settings.screenSleepLock)
        assertTrue(settings.rememberLastPage)
        assertFalse(settings.autoSaveWarning)
        assertFalse(settings.rightToLeftSupport)
        assertTrue(settings.enableJavaScript)
    }

    @Test
    fun testThemesMatchReference() {
        assertEquals("Xodo Light", AppTheme.LIGHT.displayName)
        assertEquals("Xodo Dark", AppTheme.DARK.displayName)
        assertEquals("OLED Ultra Dark", AppTheme.OLED_ULTRA_DARK.displayName)
        assertEquals("Premium Dark", AppTheme.PREMIUM_DARK.displayName)
        assertEquals("Night Sea", AppTheme.NIGHT_SEA.displayName)

        assertTrue(AppTheme.OLED_ULTRA_DARK.isPremium)
        assertTrue(AppTheme.PREMIUM_DARK.isPremium)
        assertTrue(AppTheme.NIGHT_SEA.isPremium)
        assertFalse(AppTheme.LIGHT.isPremium)
        assertFalse(AppTheme.DARK.isPremium)
    }
}
