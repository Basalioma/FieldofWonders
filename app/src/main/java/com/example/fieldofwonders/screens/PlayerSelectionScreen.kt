package com.example.fieldofwonders.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log
import kotlinx.coroutines.delay

@Composable
fun PlayerSelectionScreen(
    onStartGame: (List<String>) -> Unit,
    onBack: () -> Unit
) {
    var mainPlayer by remember { mutableStateOf("") }
    val extraPlayers = remember { mutableStateListOf<String>() }
    val visibleStates = remember { mutableStateListOf<Boolean>() } // Состояние видимости
    val removingIndices = remember { mutableStateListOf<Int>() } // Индексы для удаления с анимацией
    var errorMessage by remember { mutableStateOf("") }

    // Синхронизация visibleStates с extraPlayers
    LaunchedEffect(extraPlayers.size, removingIndices.size) {
        while (visibleStates.size < extraPlayers.size) {
            visibleStates.add(true) // Новое поле появляется
        }
        // Если есть удаляемые элементы, ждём завершения анимации
        if (removingIndices.isNotEmpty()) {
            delay(300L) // Ждём длительность анимации exit
            removingIndices.forEach { index ->
                if (index < extraPlayers.size) {
                    extraPlayers.removeAt(index)
                    visibleStates.removeAt(index)
                }
            }
            removingIndices.clear()
        }
    }

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
                text = "Выбор игроков",
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Основное поле для первого игрока
            TextField(
                value = mainPlayer,
                onValueChange = { mainPlayer = it },
                label = { Text("Игрок 1 (обязательно)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Дополнительные поля с анимацией
            extraPlayers.forEachIndexed { index, playerName ->
                key(index) {
                    AnimatedVisibility(
                        visible = visibleStates.getOrNull(index) ?: false,
                        enter = fadeIn(animationSpec = tween(300)) + expandVertically(animationSpec = tween(300)),
                        exit = fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.8f, animationSpec = tween(300))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextField(
                                value = playerName,
                                onValueChange = { newValue ->
                                    extraPlayers[index] = newValue
                                    Log.d("PlayerSelection", "Updated player $index to: $newValue")
                                },
                                label = { Text("Игрок ${index + 2} (обязательно)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    if (!removingIndices.contains(index)) {
                                        visibleStates[index] = false // Запускаем анимацию исчезновения
                                        removingIndices.add(index) // Помечаем для удаления
                                        Log.d("PlayerSelection", "Marked player $index for removal, visibleStates: $visibleStates")
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Remove,
                                    contentDescription = "Удалить игрока",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Кнопка для добавления нового игрока
            if (extraPlayers.size < 2) {
                IconButton(
                    onClick = {
                        extraPlayers.add("")
                        Log.d("PlayerSelection", "Added new player, size: ${extraPlayers.size}")
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Добавить игрока",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Сообщение об ошибке
            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Кнопка "Начать игру"
            Button(
                onClick = {
                    val allPlayers = mutableListOf(mainPlayer) + extraPlayers
                    val nonEmptyPlayers = allPlayers.filter { it.isNotBlank() }

                    when {
                        mainPlayer.isBlank() -> errorMessage = "Введите имя первого игрока"
                        extraPlayers.any { it.isBlank() } -> errorMessage = "Все поля должны быть заполнены"
                        nonEmptyPlayers.size > 3 -> errorMessage = "Максимум 3 игрока"
                        else -> {
                            errorMessage = ""
                            onStartGame(nonEmptyPlayers)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(56.dp)
            ) {
                Text("Начать игру", fontSize = 18.sp)
            }
        }

        // Кнопка "Назад"
        TextButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 8.dp)
        ) {
            Text("Назад", fontSize = 16.sp)
        }
    }
}