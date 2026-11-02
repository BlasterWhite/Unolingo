package fr.enssat.unolingo.mguezenn_mphongsa.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import fr.enssat.unolingo.mguezenn_mphongsa.ui.battle.BattleScreen
import fr.enssat.unolingo.mguezenn_mphongsa.ui.home.HomeScreen
import fr.enssat.unolingo.mguezenn_mphongsa.ui.learn.LearnScreen
import fr.enssat.unolingo.mguezenn_mphongsa.ui.quiz.QuizScreen
import fr.enssat.unolingo.mguezenn_mphongsa.ui.stats.StatsScreen

@Composable
fun UnolingoNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = HomeRoute, modifier = modifier) {
        composable<HomeRoute> {
            HomeScreen(
                onStartQuiz = { set, mode -> navController.navigate(QuizRoute(set, mode)) },
                onLearn = { set -> navController.navigate(LearnRoute(set)) },
                onOpenStats = { navController.navigate(StatsRoute) },
                onOpenBattle = { navController.navigate(BattleRoute) },
            )
        }
        composable<QuizRoute> { entry ->
            val route = entry.toRoute<QuizRoute>()
            QuizScreen(
                characterSet = route.characterSet,
                mode = route.mode,
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
        composable<BattleRoute> {
            BattleScreen(onBack = navController::popBackStack)
        }
    }
}
