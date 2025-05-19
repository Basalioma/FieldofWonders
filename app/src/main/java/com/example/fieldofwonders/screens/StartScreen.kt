package com.example.fieldofwonders.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StartScreen(
   onStart: (List<String>) -> Unit,
   onSettings: () -> Unit,
   onRules: () -> Unit
) {
   Box(
      modifier = Modifier
         .fillMaxSize()
         .padding(16.dp)
   ) {
      Column(
         modifier = Modifier.align(Alignment.Center),
         horizontalAlignment = Alignment.CenterHorizontally
      ) {
         Text(
            text = "ПОЛЕ ЧУДЕС",
            fontSize = 36.sp,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
         )
         Spacer(modifier = Modifier.height(32.dp))

         Button(
            onClick = { onStart(listOf("Игрок 1")) },
            modifier = Modifier
               .fillMaxWidth(0.7f)
               .height(56.dp)
         ) {
            Text("Начать игру", fontSize = 18.sp)
         }
         Spacer(modifier = Modifier.height(16.dp))
         OutlinedButton(
            onClick = onRules,
            modifier = Modifier
               .fillMaxWidth(0.7f)
               .height(56.dp)
         ) {
            Text("Правила игры", fontSize = 18.sp)
         }
      }

      IconButton(
         onClick = onSettings,
         modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = 16.dp)
            .size(48.dp)
      ) {
         Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "Настройки",
            tint = MaterialTheme.colorScheme.onBackground
         )
      }

      Text(
         text = "v 1.2.0",
         fontSize = 14.sp,
         color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
         modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(bottom = 8.dp)
      )
   }
}