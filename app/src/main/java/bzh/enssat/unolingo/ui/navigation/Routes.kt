package bzh.enssat.unolingo.ui.navigation

import bzh.enssat.unolingo.domain.model.CharacterSet
import bzh.enssat.unolingo.domain.model.QuizMode
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data class QuizRoute(val characterSet: CharacterSet, val mode: QuizMode)

@Serializable
data object StatsRoute

@Serializable
data object BattleRoute
