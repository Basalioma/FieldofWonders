package com.example.fieldofwonders.screens.gamescreen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import com.example.fieldofwonders.data.GameState

@Composable
fun KeyboardInput(
   gameState: GameState,
   isPlayerTurnOverall: Boolean,
   isPlusSectorActionActive: Boolean,
   onGuess: (String) -> Unit
) {
   var wordGuess by remember { mutableStateOf("") }
   val alphabet = "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ"
   val scrollState = rememberScrollState()

   val showLetterKeyboard = isPlayerTurnOverall && !isPlusSectorActionActive && gameState.lastSector != null
   val showWordGuessArea = isPlayerTurnOverall && gameState.lastSector != null

   println("Keyboard Render: isPlayerTurnOverall=$isPlayerTurnOverall, lastSector=${gameState.lastSector}, showLetterKeyboard=$showLetterKeyboard, showWordGuessArea=$showWordGuessArea, usedLetters=${gameState.usedLetters}")

   Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally
   ) {
      if (showLetterKeyboard) {
         Box(modifier = Modifier.fillMaxWidth()) {
            Column(
               modifier = Modifier
                  .fillMaxWidth()
                  .verticalScroll(scrollState)
                  .padding(horizontal = 16.dp),
               horizontalAlignment = Alignment.CenterHorizontally
            ) {
               alphabet.chunked(7).forEach { rowLetters ->
                  Row(
                     modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                     horizontalArrangement = Arrangement.Center
                  ) {
                     rowLetters.forEach { letter ->
                        val isUsed = gameState.usedLetters.contains(letter)
                        Button(
                           onClick = {
                              if (!isUsed) {
                                 println("Keyboard Action: Guessing letter='$letter'")
                                 onGuess(letter.toString())
                              }
                           },
                           enabled = !isUsed,
                           modifier = Modifier
                              .size(45.dp)
                              .padding(1.dp),
                           contentPadding = PaddingValues(0.dp),
                           colors = ButtonDefaults.buttonColors(
                              containerColor = MaterialTheme.colorScheme.primary,
                              contentColor = MaterialTheme.colorScheme.onPrimary,
                              disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                              disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                           )
                        ) {
                           Text(
                              text = letter.toString(),
                              fontSize = 18.sp
                           )
                        }
                     }
                  }
               }
               Spacer(modifier = Modifier.height(16.dp))
            }
         }
      } else if (isPlusSectorActionActive && isPlayerTurnOverall) {
         Text(
            "Сектор Плюс! Выберите букву на поле или угадайте слово целиком.",
            modifier = Modifier.padding(bottom = 8.dp, top = 16.dp),
            textAlign = TextAlign.Center
         )
         Spacer(modifier = Modifier.height(100.dp))
      } else {
         Spacer(modifier = Modifier.height(150.dp))
      }

      if (showWordGuessArea) {
         TextField(
            value = wordGuess,
            onValueChange = { newValue ->
               wordGuess = newValue.uppercase().filter { it in alphabet }
            },
            label = { Text("Назови слово целиком") },
            modifier = Modifier.fillMaxWidth(0.8f)
         )

         Spacer(modifier = Modifier.height(8.dp))

         Button(
            onClick = {
               val finalGuess = wordGuess.trim()
               if (finalGuess.isNotEmpty()) {
                  println("Keyboard Action: Guessing word='$finalGuess'")
                  onGuess(finalGuess)
                  wordGuess = ""
               } else {
                  println("Keyboard Action: Empty word guess, ignoring")
               }
            },
            enabled = wordGuess.isNotBlank(),
            modifier = Modifier.fillMaxWidth(0.8f)
         ) {
            Text("Угадать слово")
         }
         Spacer(modifier = Modifier.height(16.dp))
      } else if (!showLetterKeyboard) {
         Spacer(modifier = Modifier.height(80.dp))
      }
   }
}