package com.example.fieldofwonders.settings

import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle // Оставьте импорт, если используете handleLifecycleEvent
import com.example.fieldofwonders.settings.SoundManager // Убедитесь, что импорт правильный
// Импортируем StateFlow и MutableStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameSettings(context: Context) {
    private val prefs = context.getSharedPreferences("game_settings", Context.MODE_PRIVATE)

    // Используем MutableStateFlow внутри
    private val _isMusicEnabled = MutableStateFlow(prefs.getBoolean("music_enabled", true))
    // Публично предоставляем как StateFlow (только для чтения извне)
    val isMusicEnabled: StateFlow<Boolean> = _isMusicEnabled.asStateFlow()

    private val _isSoundEffectsEnabled = MutableStateFlow(prefs.getBoolean("sound_effects_enabled", true))
    // Публично предоставляем как StateFlow
    val isSoundEffectsEnabled: StateFlow<Boolean> = _isSoundEffectsEnabled.asStateFlow()

    // Метод для изменения состояния из ViewModel
    fun setMusicEnabled(enabled: Boolean) {
        // Проверяем, изменилось ли значение, чтобы не сохранять лишний раз
        if (_isMusicEnabled.value != enabled) {
            _isMusicEnabled.value = enabled
            saveSettings() // Сохраняем настройки при изменении
        }
    }

    // Метод для изменения состояния из ViewModel
    fun setSoundEffectsEnabled(enabled: Boolean) {
        // Проверяем, изменилось ли значение
        if (_isSoundEffectsEnabled.value != enabled) {
            _isSoundEffectsEnabled.value = enabled
            saveSettings() // Сохраняем настройки при изменении
        }
    }

    // Метод сохранения теперь приватный, вызывается при изменении
    private fun saveSettings() {
        prefs.edit {
            // Читаем актуальные значения из MutableStateFlow
            putBoolean("music_enabled", _isMusicEnabled.value)
            putBoolean("sound_effects_enabled", _isSoundEffectsEnabled.value)
        }
        println("GameSettings: Settings saved (Music: ${_isMusicEnabled.value}, SFX: ${_isSoundEffectsEnabled.value})")
    }

    // Логика обработки жизненного цикла остается здесь, если нужна
    fun handleLifecycleEvent(event: Lifecycle.Event, soundManager: SoundManager) {
        when (event) {
            // Важно: Теперь SoundManager должен проверять настройки через gameSettings.isMusicEnabled.value
            Lifecycle.Event.ON_STOP -> soundManager.pauseBackgroundMusic() // Было ON_PAUSE
            Lifecycle.Event.ON_START -> soundManager.playBackgroundMusic() // Было ON_RESUME
            Lifecycle.Event.ON_DESTROY -> soundManager.releaseBackgroundMusic()
            else -> {}
        }
    }
}