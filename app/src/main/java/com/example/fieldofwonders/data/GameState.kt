package com.example.fieldofwonders.data

// Импорты остаются те же
// import com.example.fieldofwonders.data.Word
// import com.example.fieldofwonders.data.Player
// import com.example.fieldofwonders.data.DrumSector

data class GameState(
   val currentWord: Word,
   val players: List<Player>,
   val currentPlayerIndex: Int,
   val revealedWord: String,
   val usedLetters: Set<Char>,
   // val lastMoveResult: Boolean? = null, // Можно удалить, если не используется активно
   val moveCount: Int = 0,
   val lastSector: DrumSector? = null,
   val isGameOver: Boolean = false // <-- Добавляем флаг конца игры
   // val scoreMultiplier: Int = 1 // <-- Можно добавить для сектора Удвоение, если нужно
)