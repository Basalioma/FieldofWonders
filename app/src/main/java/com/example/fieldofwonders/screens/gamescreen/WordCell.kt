package com.example.fieldofwonders.screens.gamescreen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
   letter: Char,
   displayLetter: Char,
   triggerFinalReveal: Boolean,
   index: Int,
   cellSize: Dp,
   fontSize: TextUnit,
   isPlusActionActive: Boolean,
   onCellClick: (Char) -> Unit
) {
   val density = LocalDensity.current.density
   val cellIsCurrentlyHidden = displayLetter == '*'
   val rotationTargetValue =
   if (!cellIsCurrentlyHidden || (cellIsCurrentlyHidden && triggerFinalReveal)) 180f else 0f

   val rotation by animateFloatAsState(
      targetValue = rotationTargetValue,
      animationSpec = tween(
         durationMillis = 600,
         delayMillis = if (triggerFinalReveal && cellIsCurrentlyHidden) index * 60 else 0
      ),
      label = "FlipRotation"
   )

   val clickableForPlus = isPlusActionActive && cellIsCurrentlyHidden


   Card(
      modifier = modifier
         .size(cellSize)
         .graphicsLayer {
            rotationY = rotation
            cameraDistance = 8f * density
         }
         .then(
            if (clickableForPlus) {
               Modifier.clickable {
                  onCellClick(letter)
               }
            } else Modifier
         ),
      shape = MaterialTheme.shapes.medium,
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      border = BorderStroke(
         1.dp,
         if (clickableForPlus) MaterialTheme.colorScheme.primary
         else MaterialTheme.colorScheme.outlineVariant
      ),
      colors = CardDefaults.cardColors(
         containerColor = if (rotation <= 90f) MaterialTheme.colorScheme.surfaceBright
         else MaterialTheme.colorScheme.surfaceContainerLow
      )
   ) {
      Box(
         contentAlignment = Alignment.Center,
         modifier = Modifier.fillMaxSize()
      ) {
         val textToShow = if (rotation > 90f) displayLetter.toString().uppercase()
                          else ""
         val textColor = if (rotation > 90f) MaterialTheme.colorScheme.onSurface
                         else Color.Transparent

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