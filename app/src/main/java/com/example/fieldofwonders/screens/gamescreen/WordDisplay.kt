package com.example.fieldofwonders.screens.gamescreen

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.example.fieldofwonders.data.GameState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordDisplay(gameState: GameState,
                triggerFinalReveal: Boolean,
                isPlusActionActive: Boolean,
                onPlusCellClick: (Char) -> Unit
               ) {
   val wordOriginal = gameState.currentWord.text
   val revealedWordCurrent = gameState.revealedWord
   val hint = gameState.currentWord.hint

   Column(
      modifier = Modifier
         .fillMaxWidth()
         .padding(vertical = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
   ) {
      FlowRow(
         modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
         horizontalArrangement = Arrangement.Center,
         verticalArrangement = Arrangement.spacedBy(6.dp),
         maxItemsInEachRow = 10
      ) {
         wordOriginal.forEachIndexed { index, actualLetterInWord ->
            val displayLetterToShow = revealedWordCurrent.getOrElse(index) { '*' }

            WordCell(
               letter = actualLetterInWord,
               displayLetter = displayLetterToShow,
               triggerFinalReveal = triggerFinalReveal,
               index = index,
               cellSize = 36.dp,
               fontSize = 18.sp,
               isPlusActionActive = isPlusActionActive,
               onCellClick = { clickedLetter ->
                  onPlusCellClick(clickedLetter)
               }
            )
         }
      }

      Spacer(modifier = Modifier.height(16.dp))

      androidx.compose.material3.Text(
         text = hint,
         fontSize = 16.sp,
         modifier = Modifier.padding(horizontal = 16.dp),
         textAlign = TextAlign.Center
      )
   }
}