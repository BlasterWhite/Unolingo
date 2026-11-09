package fr.enssat.unolingo.mguezenn_mphongsa.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import fr.enssat.unolingo.mguezenn_mphongsa.ui.battle.BattleScreen
import fr.enssat.unolingo.mguezenn_mphongsa.ui.credits.CreditsScreen
import fr.enssat.unolingo.mguezenn_mphongsa.ui.home.HomeScreen
import fr.enssat.unolingo.mguezenn_mphongsa.ui.learn.LearnScreen
import fr.enssat.unolingo.mguezenn_mphongsa.ui.quiz.QuizScreen
import fr.enssat.unolingo.mguezenn_mphongsa.ui.quizsetup.QuizSetupScreen
import fr.enssat.unolingo.mguezenn_mphongsa.ui.quizsetup.QuizSetupSettings
import fr.enssat.unolingo.mguezenn_mphongsa.ui.stats.StatsScreen

@Composable
fun UnolingoNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    // Last quiz settings, kept while the app lives (and across rotation / process death).
    var setupSettings by rememberSaveable(stateSaver = QuizSetupSettings.Saver) { mutableStateOf(QuizSetupSettings()) }
    NavHost(navController = navController, startDestination = HomeRoute, modifier = modifier) {
        composable<HomeRoute> {
            HomeScreen(
                onOpenQuizSetup = { set ->
                    // The home's set is applied once, on arrival; the setup screen can then change it freely.
                    setupSettings = setupSettings.withSet(set)
                    navController.navigate(QuizSetupRoute(set))
                },
                onLearn = { set -> navController.navigate(LearnRoute(set)) },
                onOpenStats = { navController.navigate(StatsRoute) },
                onOpenBattle = { navController.navigate(BattleRoute) },
                onOpenCredits = { navController.navigate(CreditsRoute) },
            )
        }
        composable<QuizSetupRoute> {
            QuizSetupScreen(
                settings = setupSettings,
                onSettingsChange = { setupSettings = it },
                onStart = { config -> navController.navigate(QuizRoute(config)) },
                onBack = navController::popBackStack,
            )
        }
        composable<QuizRoute>(typeMap = QuizRouteTypeMap) { entry ->
            QuizScreen(
                config = entry.toRoute<QuizRoute>().config,
                onBack = navController::popBackStack,
            )
        }
        composable<LearnRoute> { entry ->
            LearnScreen(
                characterSet = entry.toRoute<LearnRoute>().characterSet,
                onBack = navController::popBackStack,
            )
        }
        composable<StatsRoute> {
            StatsScreen(onBack = navController::popBackStack)
        }
        composable<CreditsRoute> {
            CreditsScreen(onBack = navController::popBackStack)
        }
        composable<BattleRoute> {
            BattleScreen(onBack = navController::popBackStack)
        }
    }
}
