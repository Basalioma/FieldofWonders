package com.example.fieldofwonders.logic

import com.example.fieldofwonders.data.GameState
import com.example.fieldofwonders.data.Player
import com.example.fieldofwonders.data.Word
// import com.example.fieldofwonders.logic.WordLoader // Не используется напрямую здесь, уберем импорт если он не нужен
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update // Импортируем update

class GameStateManager(private val wordLoader: WordLoader) {
    private var words: List<Word> = emptyList()
    // Делаем StateFlow публичным, а MutableStateFlow приватным
    private val _gameState = MutableStateFlow<GameState?>(null)
    val gameState: StateFlow<GameState?> = _gameState.asStateFlow()
    // Убираем публичный доступ к mutableGameState, все изменения через MoveProcessor/Manager
    // val mutableGameState: MutableStateFlow<GameState?> get() = _gameState

    suspend fun loadWords() {
        words = wordLoader.loadWords()
        // Не инициализируем игру здесь, только загружаем слова
        println("Words loaded: ${words.size} words.")
        // Можно инициализировать пустое состояние или оставить null до startGame
        // _gameState.value = null // Или какое-то дефолтное состояние "ожидания"
    }

    fun startGame(playerNames: List<String>) {
        if (words.isEmpty()) {
            println("Error: Cannot start game, words not loaded.")
            // Можно попробовать загрузить синхронно или показать ошибку
            return
        }
        val players = createPlayers(playerNames)
        val randomWord = words.random() // Выбираем слово
        val initialRevealed = "*".repeat(randomWord.text.length)
        println("Starting game with word: ${randomWord.text}, players: ${players.joinToString { it.name }}")
        _gameState.value = GameState(
            currentWord = randomWord,
            players = players,
            currentPlayerIndex = 0,
            revealedWord = initialRevealed,
            usedLetters = emptySet(),
            moveCount = 0,
            lastSector = null,
            isGameOver = false // Убедимся, что игра не закончена при старте
        )
    }

    private fun createPlayers(names: List<String>): List<Player> {
        // Фильтруем пустые имена на всякий случай
        val validNames = names.filter { it.isNotBlank() }
        val players = validNames.mapIndexed { index, name -> Player(name, isBot = false, id = index) }.toMutableList() // Добавляем ID
        val botNames = listOf("Бот-читатель", "Бот-везунчик", "Бот-эрудит")
        var botIdCounter = validNames.size
        while (players.size < 3) {
            players.add(Player(botNames.random(), isBot = true, score = 0, id = botIdCounter++)) // Добавляем ID и бота
        }
        return players.toList() // Возвращаем неизменяемый список
    }

    // Метод для обновления состояния из MoveProcessor
    fun updateState(newState: GameState) {
        println("GameStateManager: Updating state. CurrentPlayer: ${newState.currentPlayerIndex}, LastSector: ${newState.lastSector}, Revealed: ${newState.revealedWord}, GameOver: ${newState.isGameOver}")
        _gameState.value = newState
        // Или использовать update для атомарности, если будут сложные модификации
        // _gameState.update { newState }
    }


    fun resetGame() {
        println("GameStateManager: Resetting game.")
        _gameState.value = null // Сбрасываем состояние
    }
}