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
   isBotActing: Boolean,
   triggerFinalReveal: Boolean,
   onSpinRequest: () -> Unit,
   onGuess: (String) -> Unit,
   isPlusSectorActionActive: Boolean,
   onPlusCellClick: (Char) -> Unit
) {
   Column(
      modifier = Modifier
         .fillMaxSize()
         .padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
   ) {
      //PlayerInfo(gameState)

      val currentPlayerInGameState = gameState.players.getOrNull(gameState.currentPlayerIndex)
      println("GameScreen: About to draw Leaderboard. For gameState.currentPlayerIndex=${gameState.currentPlayerIndex}, isBot=${currentPlayerInGameState?.isBot}, Name=${currentPlayerInGameState?.name}. isBotActing_VM=$isBotActing")

      Leaderboard(gameState)
      WordDisplay(
         gameState = gameState,
         triggerFinalReveal = triggerFinalReveal,
         isPlusActionActive = isPlusSectorActionActive,
         onPlusCellClick = onPlusCellClick
      )

      if (message.isNotEmpty()) {
         Text(text = message, modifier = Modifier.padding(vertical = 8.dp))
         Spacer(modifier = Modifier.height(8.dp))
      }

      DrumSpinner(
         gameState = gameState,
         isBotActing = isBotActing,
         onSpinRequest = onSpinRequest
      )

      Spacer(modifier = Modifier.height(16.dp))

      KeyboardInput(
         gameState = gameState,
         isPlayerTurnOverall = !gameState.players[gameState.currentPlayerIndex].isBot && !isBotActing,
         isPlusSectorActionActive = isPlusSectorActionActive,
         onGuess = onGuess
      )
   }
}