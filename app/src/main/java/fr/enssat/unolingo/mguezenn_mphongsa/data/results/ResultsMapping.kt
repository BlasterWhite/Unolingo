package fr.enssat.unolingo.mguezenn_mphongsa.data.results

import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.QuestionKind
import fr.enssat.unolingo.mguezenn_mphongsa.domain.progress.QuizOutcome

fun QuizResultEntity.toOutcome(): QuizOutcome = QuizOutcome(
    score = score,
    total = total,
    characterSet = characterSet,
    grade = grade,
    help = help,
    playedAt = playedAt,
)

/**
 * A [QuizOutcome] does not know the question kind nor the tts flag, so a fake result is a mixed, silent quiz.
 * Always flagged [QuizResultEntity.isFake].
 */
fun QuizOutcome.toFakeEntity(): QuizResultEntity = QuizResultEntity(
    playedAt = playedAt,
    characterSet = characterSet,
    questionKind = QuestionKind.MIX,
    help = help,
    tts = false,
    grade = grade,
    score = score,
    total = total,
    isFake = true,
)
