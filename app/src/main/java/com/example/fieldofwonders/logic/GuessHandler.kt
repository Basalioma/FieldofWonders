package com.example.fieldofwonders.logic

import com.example.fieldofwonders.data.GameState

class GuessHandler {
    fun guessLetter(letter: Char, game: GameState): Pair<Boolean, Pair<String, Set<Char>>> {
        if (game.usedLetters.contains(letter)) {
            return Pair(false, Pair("Буква уже названа", game.usedLetters))
        }
        val newUsedLetters = game.usedLetters + letter
        val newRevealed = revealLetters(game.currentWord.text, game.revealedWord, letter)
        val success = newRevealed != game.revealedWord
        return Pair(success, Pair(if (success) "Буква открыта" else "Нет такой буквы", newUsedLetters))
    }

    fun guessWord(wordGuess: String, game: GameState): Pair<Boolean, String> {
        val isCorrect = wordGuess.equals(game.currentWord.text, ignoreCase = true)
        return Pair(isCorrect, if (isCorrect) "Слово угадано" else "Слово неверное")
    }

    private fun revealLetters(word: String, revealed: String, letter: Char): String {
        return word.mapIndexed { i, c ->
            if (c.equals(letter, ignoreCase = true)) c else revealed[i]
        }.joinToString("")
    }
}