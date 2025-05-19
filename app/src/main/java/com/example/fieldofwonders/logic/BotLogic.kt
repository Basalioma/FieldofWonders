package com.example.fieldofwonders.logic

import com.example.fieldofwonders.data.GameState
import kotlin.random.Random

class BotLogic {

   fun makeBotMove(game: GameState): String {
      println("BotLogic: Generating guess for state: revealed='${game.revealedWord}', used=${game.usedLetters}")
      val alphabet = "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ"
      val availableLetters = alphabet.filter { it !in game.usedLetters }

      val canTryGuessWord = game.moveCount >= 4

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

   fun makeBotPlusSectorMove(game: GameState): String {
      println("BotLogic: Plus Sector - Generating letter for bot to reveal.")
      val alphabet = "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ"

      val potentialLettersToReveal = mutableListOf<Char>()
      for (charInWord in game.currentWord.text.uppercase()) {
         if (charInWord.isLetter() && charInWord !in game.usedLetters && charInWord !in potentialLettersToReveal) {
            potentialLettersToReveal.add(charInWord)
         }
      }
      println("BotLogic: Plus Sector - Potential letters in word not yet in usedLetters: $potentialLettersToReveal")

      if (potentialLettersToReveal.isNotEmpty()) {
         val letterToReveal = potentialLettersToReveal.random()
         println("BotLogic: Plus Sector - Bot chose to reveal letter '$letterToReveal'.")
         return letterToReveal.toString()
      } else {
         val availableAlphabetLetters = alphabet.filter { it !in game.usedLetters }
         if (availableAlphabetLetters.isNotEmpty()) {
            val fallbackLetter = availableAlphabetLetters.random()
            println("BotLogic: Plus Sector - No specific letter to reveal from word (already in usedLetters or word fully known via usedLetters). Bot chose fallback letter '$fallbackLetter'.")
            return fallbackLetter.toString()
         } else {
            println("BotLogic: Plus Sector - All alphabet letters used. Bot defaults to 'А'.")
            return "А"
         }
      }
   }
}