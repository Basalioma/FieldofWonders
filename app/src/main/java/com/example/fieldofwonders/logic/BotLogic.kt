package com.example.fieldofwonders.logic

import com.example.fieldofwonders.data.GameState
// import com.example.fieldofwonders.data.DrumSector // Больше не нужен
// import com.example.fieldofwonders.logic.DrumManager // Больше не нужен
// import com.example.fieldofwonders.logic.MoveProcessor // Больше не нужен
import kotlin.random.Random

// Убираем зависимости, если они не нужны для стратегии угадывания
class BotLogic {

    // Теперь не suspend и просто возвращает строку для угадывания
    fun makeBotMove(game: GameState): String {
        println("BotLogic: Generating guess for state: revealed='${game.revealedWord}', used=${game.usedLetters}")
        // Простейшая логика выбора буквы (без учета частотности и т.д.)
        val alphabet = "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ"
        val availableLetters = alphabet.filter { it !in game.usedLetters }

        // Улучшенная логика угадывания слова (шанс зависит от открытых букв)
        val revealedCount = game.revealedWord.count { it != '*' }
        val wordLength = game.currentWord.text.length
        val chanceToGuessWord = when {
            revealedCount >= wordLength - 1 -> 0.8f // Почти угадано, большой шанс
            revealedCount > wordLength / 2 -> 0.3f  // Больше половины открыто
            revealedCount > 2 -> 0.1f             // Хотя бы несколько букв
            else -> 0.0f                          // Мало букв, не рискуем
        }

        val shouldGuessWord = Random.nextFloat() < chanceToGuessWord
        println("BotLogic: Chance to guess word = $chanceToGuessWord, Rolled = ${Random.nextFloat()}, ShouldGuessWord = $shouldGuessWord")


        return if (shouldGuessWord) {
            println("BotLogic: Decided to guess the word.")
            game.currentWord.text // Пытаемся угадать слово
        } else if (availableLetters.isNotEmpty()) {
            val guess = availableLetters.random()
            println("BotLogic: Decided to guess letter '$guess'. Available: $availableLetters")
            guess.toString() // Угадываем случайную доступную букву
        } else {
            // Если доступных букв нет (очень странно, но возможно), пытаемся угадать слово
            println("BotLogic: No available letters left, forced to guess word.")
            game.currentWord.text
        }
    }
}