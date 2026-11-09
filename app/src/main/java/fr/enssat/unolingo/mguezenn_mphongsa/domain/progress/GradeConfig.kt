package fr.enssat.unolingo.mguezenn_mphongsa.domain.progress

/** All the tunable constants of the learner grade formula, in one place. */
object GradeConfig {
    /** Number of most recent quizzes averaged to get the player's value (sliding window). */
    const val WINDOW_SIZE = 20

    /** Multiplier applied to a quiz played with help enabled. */
    const val HELP_FACTOR = 0.8

    /** Value (average points) that maps to the top of the rank scale. */
    const val MAX_VALUE = 5.0

    /** Number of rank levels (0 = 10 kyū … 14 = 5 dan). */
    const val LEVEL_COUNT = 15

    /** Number of kyū levels: levels 0 until this are kyū, the following ones are dan. */
    const val KYU_COUNT = 10

    /** Difficulty of a kana quiz. */
    const val KANA_DIFFICULTY = 1.0

    /** Difficulty of a kanji quiz of grade [grade]: 1 + g/2 (1.5 at grade 1, 4 at grade 6, 5 at grade 8). */
    fun kanjiDifficulty(grade: Int): Double = 1.0 + grade / 2.0
}
