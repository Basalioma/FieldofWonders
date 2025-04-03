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
            // FIX 6: Убираем Элвис, sectorOrNullForGuess не null здесь
            val sectorForGuess: DrumSector = sectorOrNullForGuess
            // Проверяем, позволяет ли сектор угадывать (на всякий случай, хотя spinResult должен был перейти ход)
            if (sectorForGuess is DrumSector.Bankrupt || sectorForGuess is DrumSector.Zero) {
                println("MoveProcessor: ERROR - Attempting to guess on Bankrupt or Zero sector. This shouldn't happen.")
                // Возвращаем ошибку или просто сообщение о переходе хода, как будто спин только что произошел
                return processSpinResult(currentState, sectorForGuess) // Повторно обработаем сектор для перехода хода
            }
            // FIX 7: Передаем non-null сектор в processGuess
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

    // Фаза 1: Обработка результата вращения барабана
    private fun processSpinResult(state: GameState, sector: DrumSector): Pair<Boolean, String> {
        println("MoveProcessor: Phase 1 - Processing Spin Result. Sector: $sector, Player: ${state.currentPlayerIndex}")
        var message = "Выпал сектор: $sector."
        var turnEnds = false
        var nextPlayerIndex = state.currentPlayerIndex
        var players = state.players
        // FIX 8: Удаляем неиспользуемую переменную currentScore
        // var currentScore = players[state.currentPlayerIndex].score
        var newLastSector: DrumSector? = sector // Сохраняем сектор для возможного угадывания

        when (sector) {
            is DrumSector.Bankrupt -> {
                message += " Ваш счет сгорел! Переход хода."
                players = updatePlayerScore(players, state.currentPlayerIndex, 0) // Обнуляем счет
                turnEnds = true
            }
            is DrumSector.Zero -> {
                message += " Вы пропускаете ход."
                turnEnds = true
            }
            is DrumSector.Plus -> {
                message += " Назовите букву!" // Сектор Плюс обычно для открытия буквы по номеру, но здесь - угадать
                // Игрок должен угадывать, ход не переходит сразу.
            }
            is DrumSector.Double -> {
                message += " Очки удвоятся! Назовите букву или слово."
                // Игрок должен угадывать, ход не переходит сразу.
            }
            is DrumSector.Points -> {
                message += " Назовите букву или слово."
                // Игрок должен угадывать, ход не переходит сразу.
            }
        }

        if (turnEnds) {
            nextPlayerIndex = (state.currentPlayerIndex + 1) % state.players.size
            newLastSector = null // Сбрасываем сектор при переходе хода
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
        // Успех = ход НЕ закончился (игрок может угадывать)
        return Pair(!turnEnds, message)
    }

    // Фаза 2: Обработка угадывания буквы или слова
    // FIX 7: Параметр sector теперь non-null
    private fun processGuess(state: GameState, sector: DrumSector, input: String): Pair<Boolean, String> {
        println("MoveProcessor: Phase 2 - Processing Guess. Input: '$input', Sector: $sector, Player: ${state.currentPlayerIndex}")
        val isLetterGuess = input.length == 1
        var message: String
        var success: Boolean
        var revealedWord = state.revealedWord
        var usedLetters = state.usedLetters
        var scoreChange = 0
        // FIX 9: Инициализируем turnEnds в зависимости от типа угадывания
        var turnEnds: Boolean
        var isGameOver = false

        if (isLetterGuess) {
            val letter = input.uppercase()[0]
            val (guessSuccess, resultData) = guessHandler.guessLetter(letter, state)
            success = guessSuccess
            message = resultData.first
            usedLetters = resultData.second

            if (success) {
                val oldRevealed = revealedWord
                // Обновляем revealedWord локально перед расчетом очков
                revealedWord = revealLetter(state.currentWord.text, state.revealedWord, letter)
                val openedCount = revealedWord.count { it != '*' } - oldRevealed.count { it != '*' }
                println("MoveProcessor: Phase 2 - Letter '$letter' correct. Opened $openedCount letters.")
                // FIX 10: Передаем state в calculateScore
                scoreChange = calculateScore(state, sector, openedCount)
                message = "Буква '$letter' есть! Вы заработали $scoreChange очков."
                // FIX 11: Игрок крутит снова, если угадал букву (правило игры)
                turnEnds = false
                message += " Крутите барабан!"
                println("MoveProcessor: Phase 2 - Letter correct, player continues turn (turnEnds=false).")
                // Проверяем, не открыли ли мы последнюю букву
                if (!revealedWord.contains('*')) {
                    println("MoveProcessor: Phase 2 - Last letter revealed! Game Over.")
                    message += " И это последняя буква! Слово угадано!"
                    isGameOver = true
                    turnEnds = true // Игра закончена, ход не продолжается
                }

            } else {
                message = "Буква '$letter'. $message. Переход хода."
                turnEnds = true // Не угадал или повтор - ход переходит
                println("MoveProcessor: Phase 2 - Letter incorrect/used, turn ends (turnEnds=true).")
            }
        } else {
            // Угадывание слова
            val (wordSuccess, wordMessage) = guessHandler.guessWord(input, state)
            success = wordSuccess
            message = wordMessage
            turnEnds = true // Ход всегда заканчивается после попытки угадать слово

            if (success) {
                println("MoveProcessor: Phase 2 - Word '$input' correct! Game Over.")
                revealedWord = state.currentWord.text
                isGameOver = true
                // FIX 10: Передаем state в calculateScore (даже если openedCount=0)
                // Очки за слово = очки сектора (или фиксированная сумма?)
                scoreChange = calculateScore(state, sector, 0) // Передаем 0, т.к. не открываем букву
                // Добавим бонус за угаданное слово, если нужно
                if (scoreChange == 0 && sector !is DrumSector.Bankrupt && sector !is DrumSector.Zero) scoreChange = 500 // Бонус, если сектор был не очковый
                message = "Правильно! $message. Вы заработали $scoreChange очков."
                println("MoveProcessor: Phase 2 - Word correct, turn ends, game over.")
            } else {
                println("MoveProcessor: Phase 2 - Word '$input' incorrect.")
                message = "$message. Переход хода."
                // Очки не меняются, ход переходит
                println("MoveProcessor: Phase 2 - Word incorrect, turn ends.")
            }
        }

        // Обновляем счет игрока
        var players = state.players
        if (scoreChange != 0) { // Сравниваем с 0, т.к. очки могут и уменьшиться (хотя здесь нет)
            val currentScore = players[state.currentPlayerIndex].score
            players = updatePlayerScore(players, state.currentPlayerIndex, currentScore + scoreChange)
        }

        // Определяем следующего игрока, если ход заканчивается и игра не окончена
        val nextPlayerIndex = if (turnEnds && !isGameOver) {
            (state.currentPlayerIndex + 1) % state.players.size
        } else {
            state.currentPlayerIndex // Ход не переходит или игра закончена
        }

        // Сбрасываем сектор, если ход переходит или игра окончена
        // Если ход НЕ переходит (угадал букву), сектор НЕ сбрасываем (он уже null из-за логики?)
        // Нет, если ход не переходит, значит игрок должен крутить снова, lastSector должен стать null.
        // Значит, lastSector сбрасывается ВСЕГДА после фазы угадывания.
        val newLastSector: DrumSector? = null // Всегда null после угадывания

        println("MoveProcessor: Phase 2 - Result: Success=$success, GameOver=$isGameOver, TurnEnds=$turnEnds, NextPlayer=$nextPlayerIndex, ScoreChange=$scoreChange")

        val newState = state.copy(
            players = players,
            currentPlayerIndex = nextPlayerIndex,
            revealedWord = revealedWord,
            usedLetters = usedLetters,
            lastSector = newLastSector, // Всегда сбрасываем после угадывания
            isGameOver = isGameOver,
            moveCount = state.moveCount + 1 // Увеличиваем счетчик ходов
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