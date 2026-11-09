package fr.enssat.unolingo.mguezenn_mphongsa.ui.quizsetup

import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.CharacterSet
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.QuestionKind
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.QuizConfig
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class QuizSetupSettingsTest {
    @Test
    fun kanaConfig_ignoresKanjiGrade() {
        val config = QuizSetupSettings(CharacterSet.KATAKANA, kanjiGrade = 5).toConfig()
        assertEquals(QuizConfig.KANA_GRADE, config.grade)
    }

    @Test
    fun kanjiConfig_carriesGradeAndOptions() {
        val config = QuizSetupSettings(CharacterSet.KANJI, QuestionKind.CHARACTER_TO_MEANING, help = true, tts = true, kanjiGrade = 8).toConfig()
        assertEquals(QuizConfig(CharacterSet.KANJI, QuestionKind.CHARACTER_TO_MEANING, help = true, tts = true, grade = 8), config)
    }

    @Test
    fun switchingToKana_dropsMeaningKindButKeepsGradeForLater() {
        val kanji = QuizSetupSettings(CharacterSet.KANJI, QuestionKind.MEANING_TO_CHARACTER, kanjiGrade = 4)
        val kana = kanji.withSet(CharacterSet.HIRAGANA)
        assertEquals(QuestionKind.MIX, kana.questionKind)
        assertEquals(4, kana.withSet(CharacterSet.KANJI).kanjiGrade)
    }

    @Test
    fun switchingSet_keepsKindOfferedByBoth() {
        val settings = QuizSetupSettings(CharacterSet.HIRAGANA, QuestionKind.READING_TO_CHARACTER)
        assertEquals(QuestionKind.READING_TO_CHARACTER, settings.withSet(CharacterSet.KANJI).questionKind)
    }

    @Test
    fun everySetOffersKindsThatBuildAValidConfig() {
        for (set in CharacterSet.entries) for (kind in QuestionKind.availableFor(set)) {
            QuizSetupSettings(set, kind).toConfig()
        }
    }

    @Test
    fun json_roundTrips() {
        val settings = QuizSetupSettings(CharacterSet.KANJI, QuestionKind.MIX, help = true, kanjiGrade = 6)
        assertEquals(settings, Json.decodeFromString<QuizSetupSettings>(Json.encodeToString(settings)))
    }
}
