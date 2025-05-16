package com.example.fieldofwonders.screens.gamescreen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fieldofwonders.data.GameState

@Composable
fun PlayerInfo(gameState: GameState) {
   Column {
      Text(
         text = "Текущий игрок: ${gameState.players[gameState.currentPlayerIndex].name}",
         fontSize = 18.sp
      )
      Spacer(modifier = Modifier.height(8.dp))
   }
}