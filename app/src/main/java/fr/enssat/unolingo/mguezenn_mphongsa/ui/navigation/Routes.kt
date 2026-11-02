package fr.enssat.unolingo.mguezenn_mphongsa.ui.navigation

import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.CharacterSet
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.QuizMode
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data class QuizRoute(val characterSet: CharacterSet, val mode: QuizMode)

@Serializable
data object StatsRoute

@Serializable
data object BattleRoute
