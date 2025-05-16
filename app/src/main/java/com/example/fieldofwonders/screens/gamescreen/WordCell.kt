package com.example.fieldofwonders.screens.gamescreen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

@Composable
fun WordCell(
   modifier: Modifier = Modifier,
   letter: Char, // Буква ('А') или заглушка ('*')
   isInitiallyHidden: Boolean, // Была ли ячейка скрыта изначально ('*')
   triggerFinalReveal: Boolean, // Флаг для финального показа
   index: Int, // Индекс для задержки анимации
   cellSize: Dp,
   fontSize: TextUnit
) {
   // Определяем, должна ли ячейка быть показана СЕЙЧАС
   val shouldBeRevealed = !isInitiallyHidden || (isInitiallyHidden && triggerFinalReveal)

   // Анимация вращения
   val rotation by animateFloatAsState(
      targetValue = if (shouldBeRevealed) 180f else 0f,
      animationSpec = tween(
         durationMillis = 600,
         // Задержка только для финального открытия, чтобы было красиво
         delayMillis = if (triggerFinalReveal && isInitiallyHidden) index * 60 else 0
      ),
      label = "FlipRotation"
   )

   val density = LocalDensity.current.density

   Card(
      modifier = modifier.size(cellSize)
         .size(cellSize)
         .graphicsLayer {
            rotationY = rotation
            cameraDistance = 8f * density
         },
      shape = MaterialTheme.shapes.medium,
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      colors = CardDefaults.cardColors(
         containerColor = if (rotation <= 90f)
            MaterialTheme.colorScheme.surfaceBright // Цвет "закрытой"
         else
            MaterialTheme.colorScheme.surfaceContainerLow // Цвет "открытой"
      )
   ) {
      Box(
         contentAlignment = Alignment.Center,
         modifier = Modifier.fillMaxSize()
      ) {
         val textToShow = if (rotation <= 90f) "" else letter.toString().uppercase()
         val textColor = if (rotation <= 90f) Color.Transparent else MaterialTheme.colorScheme.onSurface

         Text(
            text = textToShow,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.graphicsLayer {
               if (rotation > 90f) {
                  scaleX = -1f
               }
            }
         )
      }
   }
}