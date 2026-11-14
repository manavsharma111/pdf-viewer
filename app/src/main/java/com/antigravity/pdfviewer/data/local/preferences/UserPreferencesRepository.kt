package com.antigravity.pdfviewer.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.antigravity.pdfviewer.domain.model.AppTheme
import com.antigravity.pdfviewer.domain.model.CachePreset
import com.antigravity.pdfviewer.domain.model.NightReadingMode
import com.antigravity.pdfviewer.domain.model.ReadingMode
import com.antigravity.pdfviewer.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val THEME = stringPreferencesKey("app_theme")
        val USE_SINGLE_LINE_TOOLBAR = booleanPreferencesKey("use_single_line_toolbar")
        val FULL_SCREEN_MODE = booleanPreferencesKey("full_screen_mode")
        val SCREEN_SLEEP_LOCK = booleanPreferencesKey("screen_sleep_lock")
        val REMEMBER_LAST_PAGE = booleanPreferencesKey("remember_last_page")
        val AUTO_SAVE_WARNING = booleanPreferencesKey("auto_save_warning")
        val RIGHT_TO_LEFT_SUPPORT = booleanPreferencesKey("right_to_left_support")
        val ENABLE_JAVASCRIPT = booleanPreferencesKey("enable_javascript")

        val NIGHT_READING = stringPreferencesKey("night_reading_mode")
        val KEEP_SCREEN_AWAKE = booleanPreferencesKey("keep_screen_awake")
        val DEFAULT_READING_MODE = stringPreferencesKey("default_reading_mode")
        val DEFAULT_ZOOM = floatPreferencesKey("default_zoom")
        val REMEMBER_POSITION = booleanPreferencesKey("remember_position")
        val CACHE_SIZE = stringPreferencesKey("cache_size_preset")
        val PREFETCH_PAGES = intPreferencesKey("prefetch_pages")
        val HARDWARE_ACCELERATION = booleanPreferencesKey("hardware_acceleration")
        val MAX_RECENT_DOCS = intPreferencesKey("max_recent_documents")
    }

    val userSettingsFlow: Flow<UserSettings> = context.dataStore.data.map { preferences ->
        val themeStr = preferences[PreferencesKeys.THEME] ?: AppTheme.DARK.name
        val nightStr = preferences[PreferencesKeys.NIGHT_READING] ?: NightReadingMode.NONE.name
        val modeStr = preferences[PreferencesKeys.DEFAULT_READING_MODE] ?: ReadingMode.CONTINUOUS_VERTICAL.name
        val cacheStr = preferences[PreferencesKeys.CACHE_SIZE] ?: CachePreset.MEDIUM.name

        UserSettings(
            theme = runCatching { AppTheme.valueOf(themeStr) }.getOrDefault(AppTheme.DARK),
            useSingleLineToolbar = preferences[PreferencesKeys.USE_SINGLE_LINE_TOOLBAR] ?: true,
            fullScreenMode = preferences[PreferencesKeys.FULL_SCREEN_MODE] ?: true,
            screenSleepLock = preferences[PreferencesKeys.SCREEN_SLEEP_LOCK] ?: true,
            rememberLastPage = preferences[PreferencesKeys.REMEMBER_LAST_PAGE] ?: true,
            autoSaveWarning = preferences[PreferencesKeys.AUTO_SAVE_WARNING] ?: false,
            rightToLeftSupport = preferences[PreferencesKeys.RIGHT_TO_LEFT_SUPPORT] ?: false,
            enableJavaScript = preferences[PreferencesKeys.ENABLE_JAVASCRIPT] ?: true,

            nightReadingMode = runCatching { NightReadingMode.valueOf(nightStr) }.getOrDefault(NightReadingMode.NONE),
            keepScreenAwake = preferences[PreferencesKeys.KEEP_SCREEN_AWAKE] ?: false,
            defaultReadingMode = runCatching { ReadingMode.valueOf(modeStr) }.getOrDefault(ReadingMode.CONTINUOUS_VERTICAL),
            defaultZoom = preferences[PreferencesKeys.DEFAULT_ZOOM] ?: 1.0f,
            rememberPosition = preferences[PreferencesKeys.REMEMBER_POSITION] ?: true,
            cacheSizePreset = runCatching { CachePreset.valueOf(cacheStr) }.getOrDefault(CachePreset.MEDIUM),
            prefetchPages = preferences[PreferencesKeys.PREFETCH_PAGES] ?: 2,
            hardwareAcceleration = preferences[PreferencesKeys.HARDWARE_ACCELERATION] ?: true,
            maxRecentDocuments = preferences[PreferencesKeys.MAX_RECENT_DOCS] ?: 20
        )
    }

    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME] = theme.name
        }
    }

    suspend fun setUseSingleLineToolbar(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_SINGLE_LINE_TOOLBAR] = enabled
        }
    }

    suspend fun setFullScreenMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FULL_SCREEN_MODE] = enabled
        }
    }

    suspend fun setScreenSleepLock(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SCREEN_SLEEP_LOCK] = enabled
        }
    }

    suspend fun setRememberLastPage(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.REMEMBER_LAST_PAGE] = enabled
        }
    }

    suspend fun setAutoSaveWarning(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_SAVE_WARNING] = enabled
        }
    }

    suspend fun setRightToLeftSupport(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.RIGHT_TO_LEFT_SUPPORT] = enabled
        }
    }

    suspend fun setEnableJavaScript(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ENABLE_JAVASCRIPT] = enabled
        }
    }

    suspend fun setNightReadingMode(mode: NightReadingMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NIGHT_READING] = mode.name
        }
    }

    suspend fun setKeepScreenAwake(keepAwake: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEEP_SCREEN_AWAKE] = keepAwake
        }
    }

    suspend fun setDefaultReadingMode(mode: ReadingMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_READING_MODE] = mode.name
        }
    }

    suspend fun setDefaultZoom(zoom: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_ZOOM] = zoom
        }
    }

    suspend fun setRememberPosition(remember: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.REMEMBER_POSITION] = remember
        }
    }

    suspend fun setCachePreset(preset: CachePreset) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CACHE_SIZE] = preset.name
        }
    }

    suspend fun setPrefetchPages(count: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PREFETCH_PAGES] = count.coerceIn(0, 5)
        }
    }

    suspend fun setHardwareAcceleration(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HARDWARE_ACCELERATION] = enabled
        }
    }

    suspend fun setMaxRecentDocuments(max: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MAX_RECENT_DOCS] = max.coerceIn(5, 100)
        }
    }
}
