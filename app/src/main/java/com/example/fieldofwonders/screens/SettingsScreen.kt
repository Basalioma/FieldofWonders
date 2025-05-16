package com.example.fieldofwonders.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(
   // Принимаем текущие значения и колбэки
   isMusicEnabled: Boolean,
   isSoundEffectsEnabled: Boolean,
   onMusicToggle: (Boolean) -> Unit,
   onSoundEffectsToggle: (Boolean) -> Unit,
   onBack: () -> Unit
) {
   Column(
      modifier = Modifier
         .fillMaxSize()
         .padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
   ) {
      Text("Настройки", fontSize = 24.sp, style = MaterialTheme.typography.headlineMedium)
      Spacer(modifier = Modifier.height(32.dp))

      // Переключатель музыки
      Row(
         modifier = Modifier.fillMaxWidth(),
         verticalAlignment = Alignment.CenterVertically,
         horizontalArrangement = Arrangement.SpaceBetween
      ) {
         Text("Фоновая музыка", fontSize = 18.sp)
         Switch(
            checked = isMusicEnabled,
            onCheckedChange = onMusicToggle // Вызываем колбэк при изменении
         )
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Переключатель звуковых эффектов
      Row(
         modifier = Modifier.fillMaxWidth(),
         verticalAlignment = Alignment.CenterVertically,
         horizontalArrangement = Arrangement.SpaceBetween
      ) {
         Text("Звуковые эффекты", fontSize = 18.sp)
         Switch(
            checked = isSoundEffectsEnabled,
            onCheckedChange = onSoundEffectsToggle // Вызываем колбэк при изменении
         )
      }

      Spacer(modifier = Modifier.height(48.dp))

      // Кнопка Назад
      Button(onClick = onBack) {
         Text("Назад")
      }
   }
}