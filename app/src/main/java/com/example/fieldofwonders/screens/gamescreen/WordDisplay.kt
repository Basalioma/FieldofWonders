package com.example.fieldofwonders.screens.gamescreen

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
//import com.google.accompanist.flowlayout.FlowRow
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.example.fieldofwonders.data.GameState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordDisplay(gameState: GameState, triggerFinalReveal: Boolean) {
    val word = gameState.currentWord.text // Берем полное слово для определения isInitiallyHidden
    val revealedWord = gameState.revealedWord // Берем текущее открытое слово
    val hint = gameState.currentWord.hint

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Отображение ячеек слова с автоматическим переносом
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            maxItemsInEachRow = 10
        ) {
            word.forEachIndexed { index, letter ->
                val displayLetter = revealedWord.getOrElse(index) { '*' }
                val wasInitiallyHidden = displayLetter == '*'

                WordCell(
                    letter = letter,
                    isInitiallyHidden = wasInitiallyHidden,
                    triggerFinalReveal = triggerFinalReveal,
                    index = index,
                    cellSize = 36.dp,
                    fontSize = 18.sp,
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