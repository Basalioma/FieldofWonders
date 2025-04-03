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
// import com.example.fieldofwonders.data.DrumSector // Не нужен напрямую здесь
import com.example.fieldofwonders.data.GameState

@Composable
fun KeyboardInput(
    gameState: GameState,
    isPlayerTurn: Boolean, // Получаем флаг снаружи
    // message: String, // Сообщение теперь отображается в GameScreen
    onGuess: (String) -> Unit
) {
    var wordGuess by remember { mutableStateOf("") }
    // val showKeyboard by remember { mutableStateOf(false) } // Управляется теперь снаружи + условиями
    val alphabet = "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ"
    val scrollState = rememberScrollState()

    // Определяем, нужно ли показывать клавиатуру
    val shouldShowKeyboard = isPlayerTurn && gameState.lastSector != null
    // Доп. проверка: Сектор должен позволять угадывать (не Банкрот, не Ноль и т.д.)
    // Это можно проверять и здесь, но лучше если MoveProcessor сбросит lastSector при таких секторах
    // && gameState.lastSector !is DrumSector.Zero
    // && gameState.lastSector !is DrumSector.Bankrupt

    println("Keyboard Render: isPlayerTurn=$isPlayerTurn, lastSector=${gameState.lastSector}, shouldShowKeyboard=$shouldShowKeyboard, usedLetters=${gameState.usedLetters}")

    // Обертка Column нужна всегда для потенциального сообщения ниже, но контент - условно
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally // Центрируем контент
    ) {
        if (shouldShowKeyboard) {
            // Используем Box чтобы клавиатура не прыгала при скрытии/показе
            // Можно оставить Column, если поведение устраивает
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp), // Паддинг для клавиатуры
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    alphabet.chunked(7).forEach { rowLetters -> // По 7 букв для лучшего вида
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.Center // Центрируем кнопки в ряду
                        ) {
                            rowLetters.forEach { letter ->
                                val isUsed = gameState.usedLetters.contains(letter)
                                Button(
                                    onClick = {
                                        if (!isUsed) {
                                            println("Keyboard Action: Guessing letter='$letter'")
                                            onGuess(letter.toString()) // Вызываем колбэк
                                        }
                                        // Не нужно else с println, кнопка будет disabled
                                    },
                                    enabled = !isUsed, // Кнопка неактивна, если буква использована
                                    modifier = Modifier
                                        .size(45.dp) // Немного уменьшим размер
                                        .padding(1.dp),
                                    contentPadding = PaddingValues(0.dp), // Убрать внутренний паддинг кнопки
                                    colors = ButtonDefaults.buttonColors(
                                        // Явные цвета для активной/неактивной кнопки
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) {
                                    Text(
                                        text = letter.toString(),
                                        fontSize = 18.sp // Четче буква
                                    )
                                }
                            }
                        }
                    } // Конец цикла по рядам букв

                    Spacer(modifier = Modifier.height(16.dp))

                    // Поле для ввода слова
                    TextField(
                        value = wordGuess,
                        onValueChange = { newValue ->
                            // Ограничиваем ввод только буквами русского алфавита в верхнем регистре
                            wordGuess = newValue.uppercase().filter { it in alphabet }
                        },
                        label = { Text("Назови слово целиком") },
                        modifier = Modifier.fillMaxWidth(0.8f) // Не на всю ширину
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Кнопка угадать слово
                    Button(
                        onClick = {
                            val finalGuess = wordGuess.trim()
                            if (finalGuess.isNotEmpty()) {
                                println("Keyboard Action: Guessing word='$finalGuess'")
                                onGuess(finalGuess) // Вызываем колбэк
                                wordGuess = "" // Очищаем поле после попытки
                            } else {
                                println("Keyboard Action: Empty word guess, ignoring")
                            }
                        },
                        enabled = wordGuess.isNotBlank(), // Активна только если что-то введено
                        modifier = Modifier.fillMaxWidth(0.8f) // Не на всю ширину
                    ) {
                        Text("Угадать слово")
                    }
                    Spacer(modifier = Modifier.height(16.dp)) // Отступ снизу
                } // Конец внутреннего Column клавиатуры
            } // Конец Box
        } else {
            // Можно показать заглушку или пустое место, если клавиатура не нужна
            // Text("Клавиатура не активна")
            Spacer(modifier = Modifier.height(200.dp)) // Зарезервировать место, чтобы экран не прыгал
        }

        // Сообщение было перенесено в GameScreen
        // Spacer(modifier = Modifier.height(16.dp))
        // Text(text = message, fontSize = 16.sp)
    }
}