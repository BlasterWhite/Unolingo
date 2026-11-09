package fr.enssat.unolingo.mguezenn_mphongsa.ui.navigation

import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.CharacterSet
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.QuizConfig
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data class QuizRoute(val config: QuizConfig)

@Serializable
data object StatsRoute

@Serializable
data object BattleRoute

@Serializable
data class LearnRoute(val characterSet: CharacterSet)

@Serializable
data object CreditsRoute
