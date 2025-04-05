package com.example.fieldofwonders.logic

import com.example.fieldofwonders.data.GameState
import com.example.fieldofwonders.data.Player
import com.example.fieldofwonders.data.Word
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameStateManager {
    private val _gameState = MutableStateFlow<GameState?>(null)
    val gameState: StateFlow<GameState?> = _gameState.asStateFlow()

    // Метод для старта игры с КОНКРЕТНЫМ словом, выбранным ViewModel
    fun startGameWithWord(playerNames: List<String>, word: Word) {
        val players = createPlayers(playerNames)
        val initialRevealed = "*".repeat(word.text.length)
        println("GameStateManager: Starting game with word: ${word.text}, players: ${players.joinToString { it.name }}")
        _gameState.value = GameState(
            currentWord = word, // Используем переданное слово
            players = players,
            currentPlayerIndex = 0,
            revealedWord = initialRevealed,
            usedLetters = emptySet(),
            moveCount = 0,
            lastSector = null,
            isGameOver = false
        )
    }

    // Метод создания игроков остается
    private fun createPlayers(names: List<String>): List<Player> {
        val validNames = names.filter { it.isNotBlank() }
        val players = validNames.mapIndexed { index, name -> Player(name, isBot = false, id = index) }.toMutableList()
        val botNames = listOf("Бот-читатель", "Бот-везунчик", "Бот-эрудит")
        var botIdCounter = validNames.size
        while (players.size < 3) {
            players.add(Player(botNames.random(), isBot = true, score = 0, id = botIdCounter++))
        }
        return players.toList()
    }

    // Метод для обновления состояния из MoveProcessor
    fun updateState(newState: GameState) {
        println("GameStateManager: Updating state. CurrentPlayer: ${newState.currentPlayerIndex}, LastSector: ${newState.lastSector}, Revealed: ${newState.revealedWord}, GameOver: ${newState.isGameOver}")
        _gameState.value = newState
    }

    fun resetGame() {
        println("GameStateManager: Resetting game state to null.")
        _gameState.value = null
    }
}