package com.example.safesquare.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.safesquare.SafeSquareApplication
import com.example.safesquare.data.AppSettings
import com.example.safesquare.data.GameStats
import com.example.safesquare.ui.game.GameScreen
import com.example.safesquare.ui.game.GameViewModel
import com.example.safesquare.ui.screens.AboutScreen
import com.example.safesquare.ui.screens.HomeScreen
import com.example.safesquare.ui.screens.HowToPlayScreen
import com.example.safesquare.ui.screens.ResultScreen
import com.example.safesquare.ui.screens.SettingsActions
import com.example.safesquare.ui.screens.SettingsScreen
import com.example.safesquare.ui.screens.SplashScreen
import kotlinx.coroutines.launch

object Routes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val GAME = "game"
    const val RESULT = "result"
    const val HOW_TO_PLAY = "how_to_play"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
}

@Composable
fun SafeSquareNavHost(app: SafeSquareApplication, settings: AppSettings) {
    val navController = rememberNavController()
    val repository = app.settingsRepository
    val stats by repository.stats.collectAsStateWithLifecycle(initialValue = GameStats())
    val scope = rememberCoroutineScope()

    // Activity-scoped so the Result screen can read the finished match.
    val gameViewModel: GameViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                GameViewModel(onGameFinished = { winner, captures -> repository.recordGame(winner, captures) })
            }
        }
    )

    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            SplashScreen(onFinished = {
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                }
            })
        }
        composable(Routes.HOME) {
            HomeScreen(
                stats = stats,
                onPlay = {
                    gameViewModel.newGame()
                    navController.navigateOnce(Routes.GAME)
                },
                onHowToPlay = { navController.navigateOnce(Routes.HOW_TO_PLAY) },
                onSettings = { navController.navigateOnce(Routes.SETTINGS) },
                onAbout = { navController.navigateOnce(Routes.ABOUT) }
            )
        }
        composable(Routes.GAME) {
            GameScreen(
                viewModel = gameViewModel,
                settings = settings,
                soundEngine = app.soundEngine,
                haptics = app.haptics,
                onExitToHome = { navController.popBackStack(Routes.HOME, inclusive = false) },
                onGameFinished = {
                    if (navController.currentDestination?.route == Routes.GAME) {
                        navController.navigate(Routes.RESULT) {
                            popUpTo(Routes.GAME) { inclusive = true }
                        }
                    }
                }
            )
        }
        composable(Routes.RESULT) {
            val ui by gameViewModel.ui.collectAsStateWithLifecycle()
            ResultScreen(
                game = ui.game,
                durationMillis = ui.durationMillis,
                onPlayAgain = {
                    gameViewModel.newGame()
                    navController.navigate(Routes.GAME) {
                        popUpTo(Routes.RESULT) { inclusive = true }
                    }
                },
                onHome = { navController.popBackStack(Routes.HOME, inclusive = false) }
            )
        }
        composable(Routes.HOW_TO_PLAY) {
            HowToPlayScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                settings = settings,
                stats = stats,
                actions = SettingsActions(
                    onThemeMode = { scope.launch { repository.setThemeMode(it) } },
                    onBoardTheme = { scope.launch { repository.setBoardTheme(it) } },
                    onSound = { scope.launch { repository.setSound(it) } },
                    onHaptics = { scope.launch { repository.setHaptics(it) } },
                    onShowLegalMoves = { scope.launch { repository.setShowLegalMoves(it) } },
                    onHighlightSafe = { scope.launch { repository.setHighlightSafeSquares(it) } },
                    onFastMovement = { scope.launch { repository.setFastMovement(it) } },
                    onResetStats = { scope.launch { repository.resetStats() } }
                ),
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.ABOUT) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
    }
}

/** Ignores rapid double taps that would otherwise stack duplicate destinations. */
private fun NavHostController.navigateOnce(route: String) {
    if (currentDestination?.route != route) navigate(route) { launchSingleTop = true }
}
