package com.example.fieldofwonders

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect // Оставляем для звука
import androidx.compose.runtime.LaunchedEffect // Оставляем для навигации и звука
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember // Оставляем для screenState
import androidx.compose.runtime.rememberCoroutineScope // Оставляем для навигации
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel // Для получения ViewModel
import com.example.fieldofwonders.logic.GameViewModel // Импорт ViewModel
import com.example.fieldofwonders.screens.* // Импорты экранов
import com.example.fieldofwonders.screens.gamescreen.GameScreen
import com.example.fieldofwonders.ui.theme.FieldOfWondersTheme
import androidx.lifecycle.compose.LocalLifecycleOwner // Оставляем для звука
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {
   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      setContent {
         FieldOfWondersTheme {
            Surface(
               modifier = Modifier.fillMaxSize(),
               color = MaterialTheme.colorScheme.background
            ) {
               // Передаем applicationContext в ViewModel factory (если нужно)
               // val context = LocalContext.current.applicationContext
               PoleChudesApp(/* viewModel = viewModel(factory = ...) */) // Можно и без factory, если конструктор ViewModel простой
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


   var screenState by remember { mutableStateOf("loading") } // Или "start", если загрузка быстрая
   val scope = rememberCoroutineScope()
   val lifecycleOwner = LocalLifecycleOwner.current

   // Состояние для запуска финальной анимации
   var showFinalRevealAnimation by remember { mutableStateOf(false) }

   // --- Эффекты ---

   // 1. Эффект для перехода на стартовый экран ПОСЛЕ готовности ViewModel
   LaunchedEffect(Unit) { // Запускаем один раз
      // Ждем пока мы на экране загрузки
      if (screenState == "loading") {
         println("PoleChudesApp: Waiting for ViewModel to be ready...")
         // Собираем значение isReady и ждем, пока оно не станет true
         gameViewModel.isReady.first { isReadyValue -> isReadyValue }
         // Как только isReady стало true:
         println("PoleChudesApp: ViewModel is ready, navigating from loading to start.")
         // Музыка уже должна была запуститься из init ViewModel
         screenState = "start" // Переходим на стартовый экран
      }
   }

   // 2. Эффект для жизненного цикла звука
   DisposableEffect(lifecycleOwner) {
      val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
         // Вызываем методы ViewModel для управления музыкой
         when (event) {
            // Используем ON_STOP и ON_START для более надежной паузы/возобновления
            Lifecycle.Event.ON_STOP -> gameViewModel.pauseBackgroundMusic()
            Lifecycle.Event.ON_START -> gameViewModel.playBackgroundMusic()
            // ON_DESTROY обрабатывается в ViewModel.onCleared()
            else -> {}
         }
      }
      lifecycleOwner.lifecycle.addObserver(observer)
      onDispose {
         lifecycleOwner.lifecycle.removeObserver(observer)
         // Не нужно вызывать release здесь
      }
   }

   // 3. Эффект для навигации на EndScreen и ЗАПУСКА АНИМАЦИИ
   LaunchedEffect(gameState?.isGameOver) {
      val currentGameState = gameState // Захватываем состояние
      if (currentGameState?.isGameOver == true && screenState == "game") {
         println("PoleChudesApp: Game over detected. Starting final reveal animation.")
         // 1. Запускаем анимацию
         showFinalRevealAnimation = true
         // 2. Рассчитываем общую длительность анимации
         val animationBaseDuration = 600L // Длительность одной ячейки
         val staggerDelay = 60L // Задержка между ячейками
         val wordLength = currentGameState.currentWord.text.length
         val totalAnimationTime = animationBaseDuration + (wordLength * staggerDelay) + 300L // Добавим еще немного запаса

         println("PoleChudesApp: Waiting ${totalAnimationTime}ms for final animation...")
         // 3. Ждем завершения анимации
         kotlinx.coroutines.delay(totalAnimationTime)

         // 4. Переходим на EndScreen
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
            // Передаем колбэки для настроек/правил
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
            } else if (!currentGameState.isGameOver/* || showFinalRevealAnimation*/) { // Показываем игру, пока идет анимация
               println("GameScreen State: gameState is ready, showing GameScreen.")
               GameScreen(
                  gameState = currentGameState,
                  message = message,
                  isBotActing = isBotActing,
                  triggerFinalReveal = showFinalRevealAnimation,
                  onSpinRequest = { gameViewModel.requestSpin() },
                  onGuess = { input -> gameViewModel.makeGuess(input) }
               )
            } else {
               println("GameScreen State: gameState indicates game over, showing GenericLoadingScreen during transition.")
               GenericLoadingScreen()
            }
         }
         "end" -> {
            val finalGameState = gameState // Получаем финальное состояние из ViewModel
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

         // Экран настроек теперь должен работать с ViewModel
         "settings" -> SettingsScreen(
            // Передаем текущие значения и лямбды для их изменения
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