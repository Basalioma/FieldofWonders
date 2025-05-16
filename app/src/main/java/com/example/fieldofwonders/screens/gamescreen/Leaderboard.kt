package com.example.fieldofwonders.screens.gamescreen

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fieldofwonders.data.GameState

@Composable
fun Leaderboard(gameState: GameState) {
   Text(text = "Лидерборд:", fontSize = 18.sp)
   LazyColumn(modifier = Modifier.fillMaxWidth(0.7f).height(100.dp)) {
      items(gameState.players.sortedByDescending { it.score }.size) { index ->
         val player = gameState.players.sortedByDescending { it.score }[index]
         Text(
            text = "${index + 1}. ${player.name}: ${player.score}",
            fontSize = 16.sp,
            color = if (index == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
         )
      }
   }
}