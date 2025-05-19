package com.example.fieldofwonders.screens.gamescreen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator // Для индикации хода бота
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fieldofwonders.data.GameState

@Composable
fun DrumSpinner(
   gameState: GameState,
   isBotActing: Boolean,
   onSpinRequest: () -> Unit
) {
   val currentPlayer = gameState.players[gameState.currentPlayerIndex]
   val canPlayerSpin = !currentPlayer.isBot && gameState.lastSector == null && !isBotActing
   val showWaitingForGuess = !currentPlayer.isBot && gameState.lastSector != null && !isBotActing

   println("DrumSpinner Render: isBot=${currentPlayer.isBot}, isBotActing=$isBotActing, canPlayerSpin=$canPlayerSpin, showWaitingForGuess=$showWaitingForGuess, lastSector=${gameState.lastSector}")

   Column(horizontalAlignment = Alignment.CenterHorizontally) {
      gameState.lastSector?.let {
         Text("Выпал сектор: $it", fontSize = 18.sp)
         Spacer(modifier = Modifier.height(8.dp))
      }

      when {
         canPlayerSpin -> {
            Button(
               onClick = onSpinRequest,
               modifier = Modifier.fillMaxWidth(0.7f)
            ) {
               Text("Крутить барабан")
            }
         }
         // Если ход бота или бот сейчас действует
         currentPlayer.isBot || isBotActing -> {
            Text("Ход Бота...", fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            CircularProgressIndicator()
         }
         else -> {
            Text("Ожидание...", fontSize = 16.sp)
         }
      }
   }
   Spacer(modifier = Modifier.height(16.dp))
}