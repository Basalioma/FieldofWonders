package com.example.fieldofwonders.logic

import android.app.Application // Используем Application Context
import androidx.lifecycle.AndroidViewModel // Наследуемся от AndroidViewModel для доступа к Context
import androidx.lifecycle.viewModelScope
import com.example.fieldofwonders.data.DrumSector
import com.example.fieldofwonders.data.GameState
import com.example.fieldofwonders.settings.GameSettings
import com.example.fieldofwonders.settings.SoundManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch



// Наследуемся от AndroidViewModel для получения Application Context
class GameViewModel(application: Application) : AndroidViewModel(application) {

    // --- Менеджеры и Логика ---
    // Получаем applicationContext из AndroidViewModel
    private val appContext = application.applicationContext

    private val wordLoader = WordLoader(appContext)
    val gameStateManager = GameStateManager(wordLoader)
    val drumManager = DrumManager()
    private val moveProcessor = MoveProcessor(gameStateManager)
    private val botLogic = BotLogic()

    val gameSettings = GameSettings(appContext)
    private val soundManager = SoundManager(appContext, gameSettings)


    // --- Состояние для UI ---
    val gameState: StateFlow<GameState?> = gameStateManager.gameState

    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message.asStateFlow()

    private val _isBotActing = MutableStateFlow(false)
    val isBotActing: StateFlow<Boolean> = _isBotActing.asStateFlow()

    // Предоставляем доступ к состояниям настроек для SettingsScreen
    val isMusicEnabled: StateFlow<Boolean> = gameSettings.isMusicEnabled
    val isSoundEffectsEnabled: StateFlow<Boolean> = gameSettings.isSoundEffectsEnabled

    // Флаг готовности ViewModel (слова загружены)
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    // --- Инициализация ---
    init {
        viewModelScope.launch {
            println("GameViewModel: Loading words...")
            gameStateManager.loadWords()
            println("GameViewModel: Words loaded.")
            _isReady.value = true // Устанавливаем флаг готовности
            println("GameViewModel: Set isReady = true")
            playBackgroundMusic() // Запускаем музыку после загрузки
        }
        observeBotTurn()
    }

    // --- Действия Игрока (вызываются из UI) ---
    fun startGame(playerNames: List<String>) {
        _message.value = ""
        _isBotActing.value = false
        gameStateManager.startGame(playerNames)
        playBackgroundMusic() // Запускаем музыку при старте игры
    }

    fun requestSpin() {
        val state = gameState.value
        if (state != null && !state.isGameOver && !state.players[state.currentPlayerIndex].isBot && !_isBotActing.value) {
            println("ViewModel Action: Player Spin requested.")
            val sector = drumManager.spin()
            println("ViewModel Action: Spun sector = $sector")
            val (spinSuccess, msg) = moveProcessor.makeMove(sector, "")
            _message.value = msg
            // Решаем, проигрывать ли звук для сектора (например, если Банкрот)
            if (sector is DrumSector.Bankrupt || sector is DrumSector.Zero /* || !spinSuccess */ ) {
                playSoundEffect(false) // Звук ошибки/пропуска хода
            } else {
                // Можно добавить звук вращения или успеха сектора
                // playSoundEffect(true)
            }
        } else {
            println("ViewModel Action: Spin ignored.")
            _message.value = "Сейчас не ваш ход!"
        }
    }

    fun makeGuess(input: String) {
        val state = gameState.value
        if (state != null && !state.isGameOver && !state.players[state.currentPlayerIndex].isBot && !_isBotActing.value) {
            if (state.lastSector == null) {
                println("ViewModel Action: Guess ignored (must spin first).")
                _message.value = "Сначала крутите барабан!"
            } else if (input.isNotEmpty()) {
                println("ViewModel Action: Guessing '$input' for sector ${state.lastSector}")
                val (success, msg) = moveProcessor.makeMove(state.lastSector, input)
                _message.value = msg
                // Проигрываем звук ИЗ ViewModel после получения результата
                playSoundEffect(success)
            } else {
                println("ViewModel Action: Empty guess ignored.")
            }
        } else {
            println("ViewModel Action: Guess ignored.")
            _message.value = "Сейчас не ваш ход!"
        }
    }

    fun resetGame() {
        pauseBackgroundMusic() // Останавливаем музыку перед сбросом
        gameStateManager.resetGame()
        _message.value = ""
        _isBotActing.value = false
    }

    // --- Управление звуком и настройками (вызывается из UI) ---
    fun playBackgroundMusic() {
        soundManager.playBackgroundMusic()
    }

    fun pauseBackgroundMusic() {
        soundManager.pauseBackgroundMusic()
    }

    fun toggleMusic(enabled: Boolean) {
        // Вызываем метод в GameSettings для обновления значения и сохранения
        gameSettings.setMusicEnabled(enabled)
        // Логика включения/выключения музыки остается
        if (enabled) {
            playBackgroundMusic()
        } else {
            pauseBackgroundMusic()
        }
    }

    fun toggleSoundEffects(enabled: Boolean) {
        // Вызываем метод в GameSettings для обновления значения и сохранения
        gameSettings.setSoundEffectsEnabled(enabled)
    }

    private fun playSoundEffect(isSuccess: Boolean) {
        soundManager.playSoundEffect(isSuccess)
    }

    // --- Логика Хода Бота ---
    private fun observeBotTurn() {
        viewModelScope.launch {
            // Используем combine для отслеживания gameState И isBotActing
            combine(gameStateManager.gameState, _isBotActing) { state, acting ->
                // Эта лямбда будет вызываться при изменении ЛЮБОГО из потоков
                // state - последнее значение gameState
                // acting - последнее значение _isBotActing
                Triple(state, acting, state?.players?.getOrNull(state.currentPlayerIndex)?.isBot) // Передаем тройку для удобства
            }.collect { (state, acting, isBotPlayer) ->
                // Анализируем последнюю комбинацию значений
                println(">>> ViewModel Observer COMBINED: State Player=${state?.currentPlayerIndex}, isBot=$isBotPlayer, isGameOver=${state?.isGameOver}, isActing=$acting")

                // Условие для запуска хода бота
                if (state != null && !state.isGameOver && isBotPlayer == true && !acting) {
                    println(">>> ViewModel Observer COMBINED: Bot's Turn Detected for player ${state.currentPlayerIndex}. Launching bot turn cycle...")
                    // Запускаем цикл хода бота, только если он еще не запущен
                    // (Проверка !acting гарантирует это)
                    launchBotTurn()
                }
                // Блок 'else if' для сброса флага больше не нужен здесь,
                // т.к. сброс происходит в finally блока launchBotTurn,
                // а combine сам среагирует на изменение acting на false.
            }
        }
    }

    private fun launchBotTurn() {
        viewModelScope.launch {
            println("ViewModel Bot Turn CYCLE: Starting/Resuming...")

            // Устанавливаем флаг в НАЧАЛЕ цикла (после возможной начальной задержки)
            var initialDelayDone = false // Флаг, чтобы задержка была только раз
            if (!_isBotActing.value) {
                delay(1500) // Задержка только при ПЕРВОМ входе в ход бота
                // Повторно проверяем состояние после задержки
                val stateBeforeActing = gameState.value
                if (stateBeforeActing != null && stateBeforeActing.players.getOrNull(stateBeforeActing.currentPlayerIndex)?.isBot == true) {
                    _isBotActing.value = true
                    initialDelayDone = true
                    _message.value = "Ход Бота ${stateBeforeActing.players[stateBeforeActing.currentPlayerIndex].name}..."
                    println("ViewModel Bot Turn CYCLE: Set isBotActing=true")
                } else {
                    println("ViewModel Bot Turn CYCLE: State changed during initial delay or not bot's turn, aborting.")
                    return@launch // Выходим, если что-то не так после задержки
                }
            } else {
                // Если флаг уже true (не должно быть при правильной работе combine, но на всякий случай)
                println("ViewModel Bot Turn CYCLE: WARNING - Entered launchBotTurn while isBotActing was already true.")
                // Не меняем флаг и не делаем задержку
            }


            try {
                while (true) {
                    val currentState = gameStateManager.gameState.value
                    val currentPlayer = currentState?.players?.getOrNull(currentState.currentPlayerIndex)

                    if (currentState == null || currentState.isGameOver || currentPlayer?.isBot != true) {
                        val exitReason = when {
                            currentState == null -> "GameState is null"
                            currentState.isGameOver -> "Game is Over"
                            currentPlayer?.isBot != true -> "Not Bot's turn anymore (Player ${currentState.currentPlayerIndex})"
                            else -> "Unknown"
                        }
                        println("ViewModel Bot Turn CYCLE: Exiting loop because $exitReason.")
                        break
                    }

                    // Если мы только что установили isBotActing, пропускаем первую проверку currentPlayer?.isBot != true
                    // Это нужно, т.к. combine мог среагировать на isBotActing=true до обновления gameState
                    if (!initialDelayDone && !_isBotActing.value) {
                        println("ViewModel Bot Turn CYCLE: Waiting for isBotActing flag to propagate...")
                        delay(50) // Короткая пауза
                        initialDelayDone = true // Не делать эту проверку снова
                        continue // Переходим к следующей итерации, чтобы получить свежее состояние
                    }


                    var currentTurnEnds = false
                    var actionSuccess = false

                    if (currentState.lastSector == null) {
                        println("ViewModel Bot Turn CYCLE: Action - Spin needed.")
                        delay(1000)
                        val sector = drumManager.spin()
                        println("ViewModel Bot Turn CYCLE: Spun sector = $sector")

                        println("ViewModel Bot Turn CYCLE: Processing spin result...")
                        val (spinSuccess, spinMsg) = moveProcessor.makeMove(sector, "")
                        _message.value = spinMsg
                        actionSuccess = spinSuccess
                        println("ViewModel Bot Turn CYCLE: Spin processed. actionSuccess=$actionSuccess, Message: '$spinMsg'")
                        if (sector is DrumSector.Bankrupt || sector is DrumSector.Zero || !actionSuccess) {
                            if (!actionSuccess && sector !is DrumSector.Bankrupt && sector !is DrumSector.Zero) {
                                println("ViewModel Bot Turn CYCLE: Spin resulted in turn end (spinSuccess=false).")
                            }
                            playSoundEffect(false) // Звук для конца хода из-за сектора
                            currentTurnEnds = true
                        }

                    } else { // lastSector != null, угадываем
                        println("ViewModel Bot Turn CYCLE: Action - Guess needed for sector ${currentState.lastSector}.")
                        delay(1500)
                        val botGuess = botLogic.makeBotMove(currentState)
                        println("ViewModel Bot Turn CYCLE: Bot decided to guess = '$botGuess'")

                        if (botGuess.isNotEmpty()) {
                            println("ViewModel Bot Turn CYCLE: Processing guess...")
                            val (guessSuccess, guessMsg) = moveProcessor.makeMove(currentState.lastSector, botGuess)
                            _message.value = guessMsg
                            playSoundEffect(guessSuccess)
                            actionSuccess = guessSuccess
                            println("ViewModel Bot Turn CYCLE: Guess processed. actionSuccess=$actionSuccess, Message: '$guessMsg'")

                            val stateAfterGuess = gameStateManager.gameState.value
                            if (stateAfterGuess == null || stateAfterGuess.isGameOver || stateAfterGuess.currentPlayerIndex != currentState.currentPlayerIndex) {
                                currentTurnEnds = true
                                println("ViewModel Bot Turn CYCLE: Turn ended after guess (state change).")
                            } else {
                                currentTurnEnds = false // Угадал букву, ход продолжается
                                println("ViewModel Bot Turn CYCLE: Guess successful, player continues turn.")
                            }
                        } else {
                            println("ViewModel Bot Turn CYCLE: Bot returned empty guess. Assuming turn ends.")
                            currentTurnEnds = true
                        }
                    }

                    delay(500) // Пауза между действиями

                    if (currentTurnEnds) {
                        println("ViewModel Bot Turn CYCLE: Current action ended the turn. Breaking loop.")
                        break
                    }
                    println("ViewModel Bot Turn CYCLE: Player continues turn, looping...")

                } // Конец while(true)

            } catch (e: Exception) {
                println("!!!!!!!!!! ViewModel Bot Turn CYCLE ERROR: ${e.message} !!!!!!!!!!")
                e.printStackTrace()
                _message.value = "Произошла ошибка во время хода бота."
            } finally {
                println("<<< ViewModel Bot Turn CYCLE Finished. Resetting isBotActing=false")
                delay(100) // Может быть полезно оставить небольшую задержку
                // Сбрасываем флаг только если он еще true (на случай ошибок или гонок)
                if(_isBotActing.value) {
                    _isBotActing.value = false
                } else {
                    println("<<< ViewModel Bot Turn CYCLE: isBotActing was already false in finally block.")
                }
            }
        }
    }

    // Освобождение ресурсов SoundManager при уничтожении ViewModel
    override fun onCleared() {
        super.onCleared()
        soundManager.releaseBackgroundMusic() // Вызываем метод релиза
        println("GameViewModel: onCleared - SoundManager released.")
    }
}