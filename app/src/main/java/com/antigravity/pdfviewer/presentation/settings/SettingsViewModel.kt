package com.antigravity.pdfviewer.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.antigravity.pdfviewer.data.local.preferences.UserPreferencesRepository
import com.antigravity.pdfviewer.domain.model.AppTheme
import com.antigravity.pdfviewer.domain.model.CachePreset
import com.antigravity.pdfviewer.domain.model.NightReadingMode
import com.antigravity.pdfviewer.domain.model.ReadingMode
import com.antigravity.pdfviewer.domain.model.UserSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val settings: StateFlow<UserSettings> = preferencesRepository.userSettingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserSettings()
    )

    fun setTheme(theme: AppTheme) = viewModelScope.launch {
        preferencesRepository.setTheme(theme)
    }

    fun setUseSingleLineToolbar(enabled: Boolean) = viewModelScope.launch {
        preferencesRepository.setUseSingleLineToolbar(enabled)
    }

    fun setFullScreenMode(enabled: Boolean) = viewModelScope.launch {
        preferencesRepository.setFullScreenMode(enabled)
    }

    fun setScreenSleepLock(enabled: Boolean) = viewModelScope.launch {
        preferencesRepository.setScreenSleepLock(enabled)
    }

    fun setRememberLastPage(enabled: Boolean) = viewModelScope.launch {
        preferencesRepository.setRememberLastPage(enabled)
    }

    fun setAutoSaveWarning(enabled: Boolean) = viewModelScope.launch {
        preferencesRepository.setAutoSaveWarning(enabled)
    }

    fun setRightToLeftSupport(enabled: Boolean) = viewModelScope.launch {
        preferencesRepository.setRightToLeftSupport(enabled)
    }

    fun setEnableJavaScript(enabled: Boolean) = viewModelScope.launch {
        preferencesRepository.setEnableJavaScript(enabled)
    }

    fun setNightReadingMode(mode: NightReadingMode) = viewModelScope.launch {
        preferencesRepository.setNightReadingMode(mode)
    }

    fun setKeepScreenAwake(keepAwake: Boolean) = viewModelScope.launch {
        preferencesRepository.setKeepScreenAwake(keepAwake)
    }

    fun setDefaultReadingMode(mode: ReadingMode) = viewModelScope.launch {
        preferencesRepository.setDefaultReadingMode(mode)
    }

    fun setDefaultZoom(zoom: Float) = viewModelScope.launch {
        preferencesRepository.setDefaultZoom(zoom)
    }

    fun setRememberPosition(remember: Boolean) = viewModelScope.launch {
        preferencesRepository.setRememberPosition(remember)
    }

    fun setCachePreset(preset: CachePreset) = viewModelScope.launch {
        preferencesRepository.setCachePreset(preset)
    }

    fun setPrefetchPages(count: Int) = viewModelScope.launch {
        preferencesRepository.setPrefetchPages(count)
    }

    fun setHardwareAcceleration(enabled: Boolean) = viewModelScope.launch {
        preferencesRepository.setHardwareAcceleration(enabled)
    }

    fun setMaxRecentDocuments(max: Int) = viewModelScope.launch {
        preferencesRepository.setMaxRecentDocuments(max)
    }

    class Factory(
        private val preferencesRepository: UserPreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(preferencesRepository) as T
        }
    }
}
