package bzh.enssat.unolingo.domain.quiz

/** Immutable state of a series: each [submit] returns a new session with one more result. */
data class QuizSession(
    val questions: List<Question>,
    val results: List<QuestionResult> = emptyList(),
) {
    init {
        require(questions.isNotEmpty()) { "A quiz needs at least one question" }
        require(results.size <= questions.size) { "More results than questions" }
    }

    val currentQuestion: Question?
        get() = questions.getOrNull(results.size)

    val isFinished: Boolean
        get() = results.size == questions.size

    val score: Int
        get() = results.count { it.isCorrect }

    fun submit(answer: String): QuizSession {
        val question = checkNotNull(currentQuestion) { "The quiz is already finished" }
        return copy(results = results + QuestionResult(question, answer, AnswerChecker.isCorrect(question, answer)))
    }
}
