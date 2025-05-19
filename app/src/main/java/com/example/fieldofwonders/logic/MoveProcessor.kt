package com.example.fieldofwonders.logic

import com.example.fieldofwonders.data.DrumSector
import com.example.fieldofwonders.data.GameState
import com.example.fieldofwonders.data.Player

class MoveProcessor(private val gameStateManager: GameStateManager) {

   private val guessHandler = GuessHandler()

   fun makeMove(sectorOrNullForGuess: DrumSector?, input: String): Pair<Boolean, String> {
      val currentState = gameStateManager.gameState.value
      if (currentState == null || currentState.isGameOver) {
         println("MoveProcessor: Cannot make move, game not running or already over.")
         return Pair(false, "Игра не идет")
      }

      return if (input.isEmpty() && sectorOrNullForGuess != null) {
         // Фаза 1: Обработка вращения барабана
         processSpinResult(currentState, sectorOrNullForGuess)
      } else if (input.isNotEmpty() && sectorOrNullForGuess != null) {
         // Фаза 2: Обработка угадывания
         val sectorForGuess: DrumSector = sectorOrNullForGuess
         // Проверяем, позволяет ли сектор угадывать (на всякий случай, хотя spinResult должен был перейти ход)
         if (sectorForGuess is DrumSector.Bankrupt || sectorForGuess is DrumSector.Zero) {
            println("MoveProcessor: ERROR - Attempting to guess on Bankrupt or Zero sector. This shouldn't happen.")
            // Возвращаем ошибку или просто сообщение о переходе хода, как будто спин только что произошел
            return processSpinResult(currentState, sectorForGuess) // Повторно обработаем сектор для перехода хода
         }
         processGuess(currentState, sectorForGuess, input)
      }
      else {
         val errorMsg = when {
            input.isNotEmpty() && sectorOrNullForGuess == null -> "Guess attempted without a sector."
            input.isEmpty() && sectorOrNullForGuess == null -> "Spin attempted without providing a sector." // Это не должно вызываться так
            else -> "Unknown invalid call."
         }
         println("MoveProcessor: Invalid call - $errorMsg")
         Pair(false, "Некорректный ход ($errorMsg)")
      }
   }

   private fun processSpinResult(state: GameState, sector: DrumSector): Pair<Boolean, String> {
      println("MoveProcessor: Phase 1 - Processing Spin Result. Sector: $sector, Player: ${state.currentPlayerIndex}")

      var turnEnds = false
      var players = state.players
      var nextPlayerIndex = state.currentPlayerIndex
      var newLastSector: DrumSector? = sector

      when (sector) {
         is DrumSector.Bankrupt -> {
            players = updatePlayerScore(players, state.currentPlayerIndex, 0)
            turnEnds = true
         }
         is DrumSector.Zero -> {
            turnEnds = true
         }
         is DrumSector.Plus, is DrumSector.Double, is DrumSector.Points -> {
            // Ничего дополнительно делать не нужно, ход продолжается
         }
      }

      if (turnEnds) {
         nextPlayerIndex = (state.currentPlayerIndex + 1) % state.players.size
         newLastSector = null
         println("MoveProcessor: Phase 1 - Turn ends due to sector. Next player: $nextPlayerIndex")
      } else {
         println("MoveProcessor: Phase 1 - Player continues turn. Sector $sector saved.")
      }

      val newState = state.copy(
         players = players,
         currentPlayerIndex = nextPlayerIndex,
         lastSector = newLastSector,
         moveCount = state.moveCount + 1
      )
      gameStateManager.updateState(newState)

      return Pair(!turnEnds, "")
   }

   private fun processGuess(state: GameState, sectorForGuessInput: DrumSector, input: String): Pair<Boolean, String> {
      println("MoveProcessor: Phase 2 - Processing Guess. Input: '$input', Sector: $sectorForGuessInput, Player: ${state.currentPlayerIndex}")
      var message: String
      var success = false
      var revealedWord = state.revealedWord
      var usedLetters = state.usedLetters
      var scoreChange = 0
      var turnEnds: Boolean
      var isGameOver = false

      val isFullWordGuess = input.length > 1 || (input.length == 1 && state.currentWord.text.length == 1 && input.equals(state.currentWord.text, ignoreCase = true))

      if (isFullWordGuess) {
         val (wordSuccess, wordMessageResult) = guessHandler.guessWord(input, state)
         success = wordSuccess
         turnEnds = true

         if (success) {
            println("MoveProcessor: Word '$input' correct! Game Over.")
            revealedWord = state.currentWord.text
            isGameOver = true
            scoreChange = 1000
            message = "Правильно! Слово угадано! Вы заработали $scoreChange очков."
            println("MoveProcessor: Word correct, +$scoreChange points, turn ends, game over.")
         } else {
            println("MoveProcessor: Word '$input' incorrect.")
            message = "${guessHandler.guessWord(input, state).second}. Переход хода."
            println("MoveProcessor: Word incorrect, turn ends.")
         }
      } else {
         val letter = input.uppercase()[0]

         if (state.lastSector is DrumSector.Plus) {
            println("MoveProcessor: Plus Sector Action. Revealing letter '$letter'. Player: ${state.currentPlayerIndex}")

            val oldRevealedBeforePlus = state.revealedWord
            revealedWord = revealLetter(state.currentWord.text, state.revealedWord, letter)
            success = revealedWord != oldRevealedBeforePlus || (state.currentWord.text.equals(letter.toString(), ignoreCase = true) && oldRevealedBeforePlus.contains('*'))


            if (success) {
               usedLetters = state.usedLetters + letter
               scoreChange = 0
               message = "Буква '$letter' открыта по сектору Плюс! Крутите барабан."
               turnEnds = false
               println("MoveProcessor: Plus Sector - Revealed '$letter'. RevealedWord: $revealedWord. UsedLetters: $usedLetters.")

               if (!revealedWord.contains('*')) {
                  println("MoveProcessor: Plus Sector - Last letter revealed via Plus! Game Over.")
                  isGameOver = true
                  turnEnds = true
               }
            } else {
               println("MoveProcessor: Plus Sector - WARNING - Letter '$letter' did not change revealedWord or was already fully revealed.")
               message = "Не удалось открыть букву '$letter' по сектору Плюс (возможно, уже открыта или ошибка). Крутите барабан." // Игрок все равно крутит дальше
               turnEnds = false // Даже если что-то пошло не так с выбором, ход не должен перейти просто так
            }

         } else {
            println("MoveProcessor: Normal Letter Guess. Letter '$letter'.")
            val (guessSuccess, resultData) = guessHandler.guessLetter(letter, state)
            success = guessSuccess
            message = resultData.first
            usedLetters = resultData.second

            if (success) {
               val oldRevealed = state.revealedWord
               revealedWord = revealLetter(state.currentWord.text, state.revealedWord, letter)
               val openedCount = revealedWord.count { it != '*' } - oldRevealed.count { it != '*' }
               println("MoveProcessor: Letter '$letter' correct. Opened $openedCount letters.")

               scoreChange = calculateScore(state, state.lastSector!!, openedCount)

               message = "Буква '$letter' есть! Вы заработали $scoreChange очков."
               turnEnds = false
               message += " Крутите барабан!"
               println("MoveProcessor: Letter correct, player continues turn (turnEnds=false).")

               if (!revealedWord.contains('*')) {
                  println("MoveProcessor: Last letter revealed! Game Over.")
                  isGameOver = true
                  turnEnds = true
               }
            } else {
               message = "Буква '$letter'. $message. Переход хода."
               turnEnds = true
               println("MoveProcessor: Letter incorrect/used, turn ends (turnEnds=true).")
            }
         }
      }

      println("MoveProcessor: Consolidating results. GameOver=$isGameOver, Success=$success, TurnEnds=$turnEnds, ScoreChange=$scoreChange")

      if (isGameOver && success) {
         if (!isFullWordGuess) {
            scoreChange = 1000
            if (!message.contains("Слово угадано!")) {
               message = (if (message.endsWith(" Крутите барабан!")) message.removeSuffix(" Крутите барабан!") else message).trim() + " Слово полностью открыто! Вы получаете 1000 очков."
            }
         }
         println("MoveProcessor: Game over and success. Final scoreChange set to $scoreChange.")
      }

      var players = state.players
      if (scoreChange != 0) {
         val currentScore = players[state.currentPlayerIndex].score
         players = updatePlayerScore(players, state.currentPlayerIndex, currentScore + scoreChange)
      }

      val nextPlayerIndex = if (turnEnds && !isGameOver) {
         (state.currentPlayerIndex + 1) % state.players.size
      } else {
         state.currentPlayerIndex
      }

      val newLastSector: DrumSector? = null

      println("MoveProcessor: Finalizing state update. NextPlayer=$nextPlayerIndex, ScoreChange=$scoreChange, RevealedWord: $revealedWord")

      val newState = state.copy(
         players = players,
         currentPlayerIndex = nextPlayerIndex,
         revealedWord = revealedWord,
         usedLetters = usedLetters,
         lastSector = newLastSector,
         isGameOver = isGameOver,
         moveCount = state.moveCount + 1
      )
      gameStateManager.updateState(newState)

      return Pair(success, message)
   }

   private fun updatePlayerScore(players: List<Player>, playerIndex: Int, newScore: Int): List<Player> {
      println("UpdatePlayerScore: Updating player $playerIndex to score $newScore")
      return players.mapIndexed { index, player ->
         if (index == playerIndex) {
            println("UpdatePlayerScore: Player $index found. Old score: ${player.score}, New score: $newScore")
            player.copy(score = newScore.coerceAtLeast(0))
         } else {
            player
         }
      }
   }

   // FIX 10: Добавляем параметр state
   private fun calculateScore(state: GameState, sector: DrumSector, openedCount: Int): Int {
      // Если открыто 0 букв (например, угадывание слова), базовые очки не начисляем,
      // но секторы типа Double/Plus могут дать бонус за само угадывание.
      val basePoints = if (openedCount > 0) {
         when (sector) {
            is DrumSector.Points -> sector.value * openedCount
            // Правило для Double: Удваивает очки ЗА БУКВЫ, выпавшие на ЭТОМ ходу?
            // Или удваивает очки сектора Points? Сделаем второе.
            is DrumSector.Double -> (state.lastSector as? DrumSector.Points)?.value?.times(openedCount)?.times(2) ?: (500 * openedCount * 2) // Удвоить очки предыдущего сектора Points ИЛИ стандарт 500*2
            is DrumSector.Plus -> 500 * openedCount // Сектор Плюс дает бонус 500 за каждую открытую букву
            else -> 0
         }
      } else {
         // Очки за угадывание слова (openedCount = 0)
         when (sector) {
            is DrumSector.Points -> sector.value // Даем очки сектора за слово
            is DrumSector.Double -> (state.lastSector as? DrumSector.Points)?.value?.times(2) ?: 1000 // Удвоенные очки пред. сектора или 1000
            is DrumSector.Plus -> 1000 // Бонус за слово на секторе Плюс
            else -> 0
         }
      }
      println("CalculateScore: Sector=$sector, OpenedCount=$openedCount, BasePoints=$basePoints")
      return basePoints.coerceAtLeast(0) // Гарантируем неотрицательность
   }

   private fun revealLetter(word: String, revealed: String, letter: Char): String {
      return word.mapIndexed { i, c ->
         if (c.equals(letter, ignoreCase = true)) c else revealed[i]
      }.joinToString("")
   }
}