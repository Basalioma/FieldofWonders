package com.example.fieldofwonders.logic

import com.example.fieldofwonders.data.GameState
import kotlin.random.Random

class BotLogic {

    fun makeBotMove(game: GameState): String {
        println("BotLogic: Generating guess for state: revealed='${game.revealedWord}', used=${game.usedLetters}")
        val alphabet = "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ"
        val availableLetters = alphabet.filter { it !in game.usedLetters }

        val canTryGuessWord = game.moveCount >= 4 // Разрешаем угадывать слово начиная с 5-го хода (индекс 4)

        var shouldGuessWord = false
        if (canTryGuessWord) {
            val revealedCount = game.revealedWord.count { it != '*' }
            val wordLength = game.currentWord.text.length

        val chanceToGuessWord = when {
            revealedCount >= wordLength - 1 -> 0.5f
            revealedCount >= wordLength - 2 && wordLength > 5 -> 0.2f
            revealedCount > wordLength * 0.6 -> 0.1f
            revealedCount > wordLength * 0.4 && revealedCount > 2 -> 0.05f
            else -> 0.0f
        }

            val randomRoll = Random.nextFloat()
            shouldGuessWord = randomRoll < chanceToGuessWord
            println("BotLogic: Word guess check (move ${game.moveCount+1}): CanTry=$canTryGuessWord, Revealed=$revealedCount/$wordLength, Chance=$chanceToGuessWord, Roll=$randomRoll, ShouldGuess=$shouldGuessWord")
        } else {
            println("BotLogic: Word guess check (move ${game.moveCount+1}): Cannot try yet (moveCount < 4).")
        }

        return if (shouldGuessWord) {
            println("BotLogic: Decided to guess the word.")
            game.currentWord.text
        } else if (availableLetters.isNotEmpty()) {
            val guess = availableLetters.random()
            println("BotLogic: Decided to guess letter '$guess'. Available: $availableLetters")
            guess.toString()
        } else {
            println("BotLogic: No available letters left, forced to guess word.")
            game.currentWord.text
        }
    }
}