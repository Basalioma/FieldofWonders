package com.example.fieldofwonders

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fieldofwonders.logic.GameViewModel
import com.example.fieldofwonders.screens.*
import com.example.fieldofwonders.screens.gamescreen.GameScreen
import com.example.fieldofwonders.ui.theme.FieldOfWondersTheme
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      setContent {
         FieldOfWondersTheme {
            Surface(
               modifier = Modifier.fillMaxSize(),
               color = MaterialTheme.colorScheme.background
            ) {
               PoleChudesApp()
            }
         }
      }
   }
}

@Composable
fun PoleChudesApp(gameViewModel: GameViewModel = viewModel()) {

   // Собираем состояние из ViewModel
   val gameState by gameViewModel.gameState.collectAsState()
   val message by gameViewModel.message.collectAsState()
   val isBotActing by gameViewModel.isBotActing.collectAsState()

   // Состояния настроек для SettingsScreen (если он их использует)
   val isMusicEnabled by gameViewModel.isMusicEnabled.collectAsState()
   val isSoundEffectsEnabled by gameViewModel.isSoundEffectsEnabled.collectAsState()

   // Получаем новое состояние для "Плюс" сектора
   val isPlusSectorActionActive by gameViewModel.isPlusSectorActionActive.collectAsState()

   var screenState by remember { mutableStateOf("loading") }
   val lifecycleOwner = LocalLifecycleOwner.current

   // Состояние для запуска финальной анимации
   var showFinalRevealAnimation by remember { mutableStateOf(false) }

   // --- Эффекты ---

   // 1. Эффект для перехода на стартовый экран ПОСЛЕ готовности ViewModel
   LaunchedEffect(Unit) {
      if (screenState == "loading") {
         println("PoleChudesApp: Waiting for ViewModel to be ready...")
         gameViewModel.isReady.first { isReadyValue -> isReadyValue }
         println("PoleChudesApp: ViewModel is ready, navigating from loading to start.")
         screenState = "start"
      }
   }

   // 2. Эффект для жизненного цикла звука
   DisposableEffect(lifecycleOwner) {
      val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
         when (event) {
            Lifecycle.Event.ON_STOP -> gameViewModel.pauseBackgroundMusic()
            Lifecycle.Event.ON_START -> gameViewModel.playBackgroundMusic()
            else -> {}
         }
      }
      lifecycleOwner.lifecycle.addObserver(observer)
      onDispose {
         lifecycleOwner.lifecycle.removeObserver(observer)
      }
   }

   // 3. Эффект для навигации на EndScreen и ЗАПУСКА АНИМАЦИИ
   LaunchedEffect(gameState?.isGameOver) {
      val currentGameState = gameState // Захватываем состояние
      if (currentGameState?.isGameOver == true && screenState == "game") {
         println("PoleChudesApp: Game over detected. Starting final reveal animation.")
         showFinalRevealAnimation = true
         val animationBaseDuration = 600L
         val staggerDelay = 60L
         val wordLength = currentGameState.currentWord.text.length
         val totalAnimationTime = animationBaseDuration + (wordLength * staggerDelay) + 300L

         println("PoleChudesApp: Waiting ${totalAnimationTime}ms for final animation...")
         delay(totalAnimationTime)

         println("PoleChudesApp: Animation finished. Navigating to EndScreen.")
         screenState = "end"
      }
   }

   // --- UI Навигация ---
   Surface(modifier = Modifier.fillMaxSize()) {
      when (screenState) {
         "loading" -> LoadingScreen()
         "start" -> StartScreen(
            onStart = { screenState = "player_selection" },
            onSettings = { screenState = "settings" },
            onRules = { screenState = "rules" }
         )
         "player_selection" -> PlayerSelectionScreen(
            onStartGame = { players ->
               showFinalRevealAnimation = false
               gameViewModel.startGame(players)
               screenState = "game"
            },
            onBack = {
               showFinalRevealAnimation = false
               screenState = "start"
            }
         )
         "game" -> {
            val currentGameState = gameState
            if (currentGameState == null) {
               println("GameScreen State: gameState is null, showing GenericLoadingScreen while waiting...")
               GenericLoadingScreen()
            } else if (!currentGameState.isGameOver || showFinalRevealAnimation) {
               println("GameScreen State: gameState is ready, showing GameScreen.")
               GameScreen(
                  gameState = currentGameState,
                  message = message,
                  isBotActing = isBotActing,
                  triggerFinalReveal = showFinalRevealAnimation,
                  isPlusSectorActionActive = isPlusSectorActionActive,
                  onPlusCellClick = { letter ->
                     gameViewModel.selectLetterOnPlusSector(letter)
                                    },
                  onSpinRequest = { gameViewModel.requestSpin() },
                  onGuess = { input -> gameViewModel.makeGuess(input) }
               )
            } else {
               println("GameScreen State: gameState indicates game over, showing GenericLoadingScreen during transition.")
               GenericLoadingScreen()
            }
         }
         "end" -> {
            val finalGameState = gameState
            if (finalGameState != null) {
               val winnerIndex = finalGameState.currentPlayerIndex
               val winningPlayer = finalGameState.players.getOrNull(winnerIndex)
               val winnerName = winningPlayer?.name ?: "Неизвестно"

               println("EndScreen: Determining winner. Final state player index: $winnerIndex, Winner name: $winnerName")

               EndScreen(winnerName, finalGameState.currentWord.text) {
                  println("EndScreen: Restart button clicked.")
                  showFinalRevealAnimation = false
                  gameViewModel.resetGame()
                  screenState = "start"
               }
            } else {
               println("EndScreen: Invalid state detected. Navigating to start.")
               showFinalRevealAnimation = false
               LaunchedEffect(Unit) { screenState = "start" }
               GenericLoadingScreen()
            }
         }

         "settings" -> SettingsScreen(
            isMusicEnabled = isMusicEnabled,
            isSoundEffectsEnabled = isSoundEffectsEnabled,
            onMusicToggle = { enabled -> gameViewModel.toggleMusic(enabled) },
            onSoundEffectsToggle = { enabled -> gameViewModel.toggleSoundEffects(enabled) },
            onBack = { screenState = "start" }
         )
         "rules" -> RulesScreen { screenState = "start" }
         else -> {
            println("Error: Unknown screen state '$screenState'")
            LaunchedEffect(Unit) { screenState = "start" }
            GenericLoadingScreen()
         }

      }
   }
}