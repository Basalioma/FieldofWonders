package com.example.fieldofwonders.logic

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fieldofwonders.data.DrumSector
import com.example.fieldofwonders.data.GameState
import com.example.fieldofwonders.data.Word
import com.example.fieldofwonders.settings.GameSettings
import com.example.fieldofwonders.settings.SoundManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class GameViewModel(application: Application) : AndroidViewModel(application) {

    // --- Менеджеры и Логика ---
    private val appContext = application.applicationContext

    private val wordLoader = WordLoader(appContext)
    private val gameStateManager = GameStateManager()
    private val drumManager = DrumManager()
    private val moveProcessor = MoveProcessor(gameStateManager)
    private val botLogic = BotLogic()

    private val gameSettings = GameSettings(appContext)
    private val soundManager = SoundManager(appContext, gameSettings)

    // SharedPreferences для состояния пула вопросов
    private val wordPoolPrefs = appContext.getSharedPreferences("word_pool_state", Context.MODE_PRIVATE)

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

    // --- Внутреннее состояние пула вопросов ---
    private var allWords: List<Word> = emptyList() // Мастер-лист слов
    private var currentWordVersion: Int = 0        // Версия загруженного списка слов
    private var currentShuffledIndices: MutableList<Int> = mutableListOf() // Текущий порядок индексов
    private var usedIndicesSet: MutableSet<Int> = mutableSetOf() // Использованные индексы в текущем цикле

    companion object {
        private const val PREF_POOL_VERSION = "pool_word_version"
        private const val PREF_USED_INDICES = "pool_used_indices"
        private const val PREF_SHUFFLED_ORDER = "pool_shuffled_order" // Ключ для сохранения порядка
    }

    // --- Инициализация ---
    init {
        viewModelScope.launch {
            loadAndPrepareWordPool() // Загружаем слова и состояние пула
            _isReady.value = true // Устанавливаем готовность ПОСЛЕ подготовки пула
            println("GameViewModel: Set isReady = true")
            playBackgroundMusic()
        }
        observeBotTurn() // Запускаем наблюдатель за ходом бота
    }

    // Загрузка слов и восстановление/инициализация состояния пула
    private suspend fun loadAndPrepareWordPool() {
        println("GameViewModel: Loading words and preparing pool...")
        val wordData = wordLoader.loadWords() // Получаем слова и версию
        allWords = wordData.words
        currentWordVersion = wordData.version
        println("GameViewModel: Loaded ${allWords.size} words, version $currentWordVersion.")

        if (allWords.isEmpty()) {
            println("GameViewModel ERROR: Word list is empty! Cannot prepare pool.")
            _message.value = "Ошибка: не удалось загрузить слова для игры."
            // Устанавливаем готовность, но играть будет нельзя
            _isReady.value = true
            return // Выходим, если слов нет
        }

        // Загружаем сохраненное состояние пула
        val savedVersion = wordPoolPrefs.getInt(PREF_POOL_VERSION, -1)
        val savedUsedIndicesStrings = wordPoolPrefs.getStringSet(PREF_USED_INDICES, emptySet()) ?: emptySet()
        val savedShuffledOrderString = wordPoolPrefs.getString(PREF_SHUFFLED_ORDER, null)

        println("GameViewModel: Loaded saved pool state - Version: $savedVersion, Used count: ${savedUsedIndicesStrings.size}, Has saved order: ${savedShuffledOrderString != null}")

        // Проверяем версию
        if (currentWordVersion == savedVersion && savedShuffledOrderString != null) {
            // Версия совпадает и есть сохраненный порядок - восстанавливаем
            println("GameViewModel: Word version matches saved state. Restoring pool...")
            usedIndicesSet = savedUsedIndicesStrings.mapNotNull { it.toIntOrNull() }.toMutableSet()
            currentShuffledIndices = savedShuffledOrderString.split(',')
                .mapNotNull { it.toIntOrNull() }
                .toMutableList()

            // Проверка консистентности (на всякий случай)
            if (currentShuffledIndices.size != allWords.size || usedIndicesSet.any { it >= allWords.size }) {
                println("GameViewModel WARN: Saved pool state inconsistent. Resetting pool.")
                resetWordPool() // Сбрасываем, если что-то не так
            } else {
                println("GameViewModel: Pool restored. Used indices: ${usedIndicesSet.size}/${currentShuffledIndices.size}")
            }

        } else {
            // Версия не совпадает или нет сохраненного состояния - создаем новый пул
            if (currentWordVersion != savedVersion) {
                println("GameViewModel: Word version mismatch (current: $currentWordVersion, saved: $savedVersion). Resetting pool.")
            } else {
                println("GameViewModel: No saved shuffled order found. Resetting pool.")
            }
            resetWordPool()
        }
    }

    // Создает новый перемешанный пул и сбрасывает использованные индексы
    private fun resetWordPool() {
        if (allWords.isEmpty()) return // Нечего сбрасывать

        println("GameViewModel: Resetting and shuffling word pool...")
        currentShuffledIndices = allWords.indices.toMutableList() // Список индексов [0, 1, 2, ...]
        currentShuffledIndices.shuffle() // Перемешиваем
        usedIndicesSet.clear() // Очищаем использованные

        // Сохраняем новое состояние
        saveWordPoolState()
        println("GameViewModel: New pool created. Order: ${currentShuffledIndices.take(5)}... Used: ${usedIndicesSet.size}")
    }

    // Сохраняет текущее состояние пула в SharedPreferences
    private fun saveWordPoolState() {
        if (allWords.isEmpty()) return // Не сохраняем, если нет слов

        val usedIndicesStrings = usedIndicesSet.map { it.toString() }.toSet()
        val shuffledOrderString = currentShuffledIndices.joinToString(",")

        wordPoolPrefs.edit {
            putInt(PREF_POOL_VERSION, currentWordVersion)
            putStringSet(PREF_USED_INDICES, usedIndicesStrings)
            putString(PREF_SHUFFLED_ORDER, shuffledOrderString)
            apply() // Сохраняем асинхронно
        }
    }

    // Получает следующее неиспользованное слово
    private suspend fun getNextWord(): Word? = withContext(Dispatchers.Default) { // Выносим поиск в фоновый поток
        if (allWords.isEmpty()) {
            println("GameViewModel ERROR: Cannot get next word, word list is empty.")
            return@withContext null
        }

        // Ищем первый неиспользованный индекс в текущем порядке
        var nextIndex: Int? = null
        for (index in currentShuffledIndices) {
            if (!usedIndicesSet.contains(index)) {
                nextIndex = index
                break
            }
        }

        // Если не нашли (цикл закончился)
        if (nextIndex == null) {
            println("GameViewModel: Word pool cycle complete. Resetting pool...")
            resetWordPool() // Перемешиваем заново
            // Берем первый индекс из нового пула
            nextIndex = currentShuffledIndices.firstOrNull() // Должен быть не null, если allWords не пуст
        }

        if (nextIndex != null) {
            println("GameViewModel: Next word index selected: $nextIndex")
            usedIndicesSet.add(nextIndex) // Помечаем как использованный
            saveWordPoolState() // Сохраняем обновленный список использованных
            return@withContext allWords.getOrNull(nextIndex) // Возвращаем слово
        } else {
            println("GameViewModel ERROR: Could not determine next word index even after reset.")
            return@withContext null // Не должно произойти, если allWords не пуст
        }
    }

    // --- Действия Игрока (вызываются из UI) ---
    fun startGame(playerNames: List<String>) {
        viewModelScope.launch { // Запускаем в корутине, т.к. getNextWord - suspend
            _message.value = ""
            _isBotActing.value = false
            val nextWord = getNextWord() // Получаем следующее слово
            if (nextWord != null) {
                gameStateManager.startGameWithWord(playerNames, nextWord) // Нужен новый метод в GameStateManager
                println("GameViewModel: Starting game with word: ${nextWord.text}")
                playBackgroundMusic()
            } else {
                _message.value = "Не удалось выбрать слово для игры!"
                println("GameViewModel ERROR: Failed to get a word to start the game.")
                // Что делать в этом случае? Возможно, показать ошибку и не переходить на экран игры.
            }
        }
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
            if (sector is DrumSector.Bankrupt || sector is DrumSector.Zero || !spinSuccess) {
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
        pauseBackgroundMusic()
        gameStateManager.resetGame()
        _message.value = ""
        _isBotActing.value = false
        println("GameViewModel: Game state reset. Word pool state remains.")
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
                        //_message.value = spinMsg
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