package com.example.fieldofwonders.settings

import android.content.Context
import android.media.MediaPlayer
import com.example.fieldofwonders.R // Убедитесь, что R импортирован

class SoundManager(private val context: Context, private val settings: GameSettings?) { // Сделаем settings nullable на всякий случай

    // Используем ленивую инициализацию, чтобы избежать проблем с контекстом
    private val backgroundMusicPlayer: MediaPlayer by lazy {
        MediaPlayer.create(context, R.raw.background).apply { isLooping = true }
    }
    private var isMusicReleased = false // Флаг, что ресурсы музыки освобождены

    fun playBackgroundMusic() {
        // Проверяем настройки через .value и что ресурсы не освобождены
        if (!isMusicReleased && settings?.isMusicEnabled?.value == true && !backgroundMusicPlayer.isPlaying) {
            try {
                backgroundMusicPlayer.start()
                println("SoundManager: Background music started.")
            } catch (e: IllegalStateException) {
                println("SoundManager Error starting music: ${e.message}")
                // Попробуем пересоздать плеер, если он в плохом состоянии
                // releaseBackgroundMusicInternal() // Освобождаем старый
                // backgroundMusicPlayer = MediaPlayer.create(context, R.raw.background).apply { isLooping = true }
                // if (settings?.isMusicEnabled?.value == true) backgroundMusicPlayer.start()
                // Пересоздание может быть сложным, пока просто логируем
            }
        } else {
            println("SoundManager: Background music not starting (Released=$isMusicReleased, Enabled=${settings?.isMusicEnabled?.value}, Playing=${!isMusicReleased && backgroundMusicPlayer.isPlaying})")
        }
    }

    fun pauseBackgroundMusic() {
        if (!isMusicReleased && backgroundMusicPlayer.isPlaying) {
            try {
                backgroundMusicPlayer.pause()
                println("SoundManager: Background music paused.")
            } catch (e: IllegalStateException) {
                println("SoundManager Error pausing music: ${e.message}")
            }
        }
    }

    // Внутренний метод для освобождения ресурсов
    private fun releaseBackgroundMusicInternal() {
        if (!isMusicReleased) {
            try {
                if (backgroundMusicPlayer.isPlaying) {
                    backgroundMusicPlayer.stop()
                }
                backgroundMusicPlayer.release()
                isMusicReleased = true
                println("SoundManager: Background music player released.")
            } catch (e: Exception) { // Ловим все исключения при релизе
                println("SoundManager Error releasing music: ${e.message}")
                isMusicReleased = true // Считаем освобожденным даже при ошибке
            }
        }
    }

    // Публичный метод, который может вызываться извне (например, onCleared ViewModel)
    fun releaseBackgroundMusic() {
        releaseBackgroundMusicInternal()
    }


    fun playSoundEffect(isSuccess: Boolean) {
        // Проверяем настройки через .value
        if (settings?.isSoundEffectsEnabled?.value == true) {
            val soundResId = if (isSuccess) R.raw.ding else R.raw.error
            var player: MediaPlayer? = null // Делаем nullable
            try {
                player = MediaPlayer.create(context, soundResId)
                player?.setOnCompletionListener { mp ->
                    println("SoundManager: Sound effect completed, releasing player.")
                    mp?.release() // Используем safe call
                }
                player?.setOnErrorListener { mp, what, extra ->
                    println("SoundManager: Sound effect error ($what, $extra), releasing player.")
                    mp?.release() // Освобождаем при ошибке
                    true // Возвращаем true, если обработали ошибку
                }
                player?.start()
                println("SoundManager: Playing sound effect (success=$isSuccess)")
            } catch (e: Exception) {
                println("SoundManager Error playing sound effect: ${e.message}")
                player?.release() // Убедимся, что ресурсы освобождены при ошибке создания/запуска
            }
        } else {
            println("SoundManager: Sound effects disabled, not playing.")
        }
    }
}