package com.example.fieldofwonders.screens.gamescreen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.fieldofwonders.data.GameState

@Composable
fun GameScreen(
    gameState: GameState,
    message: String,
    isBotActing: Boolean, // Добавляем флаг
    triggerFinalReveal: Boolean,
    onSpinRequest: () -> Unit, // Добавляем колбэк
    onGuess: (String) -> Unit // Оставляем колбэк
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PlayerInfo(gameState)
        Leaderboard(gameState)
        WordDisplay(
            gameState = gameState,
            triggerFinalReveal = triggerFinalReveal
        )

        // Отображаем сообщение от игры
        if (message.isNotEmpty()) {
            Text(text = message, modifier = Modifier.padding(vertical = 8.dp))
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Барабан теперь принимает onSpinRequest и флаг isBotActing
        DrumSpinner(
            gameState = gameState,
            isBotActing = isBotActing,
            onSpinRequest = onSpinRequest
        )

        Spacer(modifier = Modifier.height(16.dp)) // Пространство перед клавиатурой

        // Клавиатура принимает onGuess и флаг isBotActing
        KeyboardInput(
            gameState = gameState,
            isPlayerTurn = !gameState.players[gameState.currentPlayerIndex].isBot && !isBotActing, // Определяем, ход ли игрока
            onGuess = onGuess
        )
    }
}