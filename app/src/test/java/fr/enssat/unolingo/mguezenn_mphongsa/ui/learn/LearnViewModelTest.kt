package fr.enssat.unolingo.mguezenn_mphongsa.ui.learn

import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.enssat.unolingo.mguezenn_mphongsa.data.CharacterRepository
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.CharacterSet
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.JapaneseCharacter
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.KanaReadings
import fr.enssat.unolingo.mguezenn_mphongsa.speech.JapaneseSpeaker
import fr.enssat.unolingo.mguezenn_mphongsa.speech.SpeakerStatus
import fr.enssat.unolingo.mguezenn_mphongsa.speech.SpeechRates
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class LearnViewModelTest {
    private class FakeSpeaker(initial: SpeakerStatus) : JapaneseSpeaker {
        override val status = MutableStateFlow(initial)
        override val isSpeaking: StateFlow<Boolean> = MutableStateFlow(false)
        val spoken = mutableListOf<Pair<String, Float>>()
        var released = false

        override fun speak(text: String, rate: Float) {
            spoken += text to rate
        }

        override fun release() {
            released = true
        }
    }

    private val hiragana = listOf(
        JapaneseCharacter("あ", listOf("a")),
        JapaneseCharacter("い", listOf("i")),
        JapaneseCharacter("か", listOf("ka")),
    )
    private val kanji = listOf(
        JapaneseCharacter("日", listOf("nichi", "hi", "ka"), meaning = "jour"),
        JapaneseCharacter(
            "山", listOf("san", "yama"), meaning = "mountain", grade = 1, meaningsEn = listOf("mountain"),
            kanaReadings = KanaReadings(on = listOf("さん"), kun = listOf("やま")),
        ),
        JapaneseCharacter(
            "校", listOf("kou"), meaning = "school", grade = 2, meaningsEn = listOf("school"),
            kanaReadings = KanaReadings(on = listOf("こう")),
        ),
    )

    private val repository = object : CharacterRepository {
        override suspend fun getCharacters(set: CharacterSet) = if (set == CharacterSet.KANJI) kanji else hiragana
    }

    private fun viewModel(set: CharacterSet, speaker: FakeSpeaker) =
        LearnViewModel(set, repository, speakerFactory = { speaker }).also { idle() }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun LearnViewModel.showing() = uiState.value as LearnUiState.Showing

    @Test
    fun navigation_stopsAtBothEnds() {
        val vm = viewModel(CharacterSet.HIRAGANA, FakeSpeaker(SpeakerStatus.READY))
        assertEquals(0, vm.showing().index)
        assertFalse(vm.showing().hasPrevious)
        vm.previous()
        assertEquals(0, vm.showing().index)
        vm.next()
        vm.next()
        assertEquals(2, vm.showing().index)
        assertFalse(vm.showing().hasNext)
        vm.next()
        assertEquals(2, vm.showing().index)
        vm.previous()
        assertEquals("い", vm.showing().current.symbol)
        assertTrue(vm.showing().hasNext)
    }

    @Test
    fun speak_alternatesNormalAndSlowForTheSameCard_andRestartsOnAnotherCard() {
        val speaker = FakeSpeaker(SpeakerStatus.READY)
        val vm = viewModel(CharacterSet.HIRAGANA, speaker)
        vm.speak()
        vm.speak()
        vm.next()
        vm.speak()
        assertEquals(
            listOf("あ" to SpeechRates.NORMAL, "あ" to SpeechRates.SLOW, "い" to SpeechRates.NORMAL),
            speaker.spoken,
        )
    }

    @Test
    fun speak_pronouncesKanjiAsHiragana() {
        val speaker = FakeSpeaker(SpeakerStatus.READY)
        val vm = viewModel(CharacterSet.KANJI, speaker)
        vm.speak()
        // The kana converter is built from the repository's hiragana, which only holds あ/い/か here: no match for
        // "nichi", so the symbol itself is the fallback.
        assertEquals("日", speaker.spoken.single().first)
    }

    @Test
    fun missingVoice_isReported_andRetryChecksAgain() {
        val missing = FakeSpeaker(SpeakerStatus.MISSING_VOICE)
        val retried = FakeSpeaker(SpeakerStatus.READY)
        val speakers = ArrayDeque(listOf(missing, retried))
        val vm = LearnViewModel(CharacterSet.HIRAGANA, repository, speakerFactory = { speakers.removeFirst() })
        idle()
        assertEquals(LearnUiState.MissingVoice, vm.uiState.value)
        vm.retryVoice()
        idle()
        assertTrue(missing.released)
        assertTrue(vm.uiState.value is LearnUiState.Showing)
    }

    @Test
    fun speechUnavailable_isReported() {
        val vm = viewModel(CharacterSet.HIRAGANA, FakeSpeaker(SpeakerStatus.UNAVAILABLE))
        assertEquals(LearnUiState.SpeechUnavailable, vm.uiState.value)
    }

    @Test
    fun kanjiSpeak_readsTheSelectedHiraganaReading() {
        val speaker = FakeSpeaker(SpeakerStatus.READY)
        val vm = viewModel(CharacterSet.KANJI, speaker)
        vm.selectGrade(1)
        vm.speak()
        vm.selectReading(1)
        assertEquals(listOf("さん", "やま"), speaker.spoken.map { it.first })
        assertEquals(1, vm.showing().readingIndex)
        vm.next()
        vm.selectGrade(null)
        assertEquals(0, vm.showing().readingIndex)
    }

    @Test
    fun selectGrade_filtersCardsAndRestartsAtTheFirst() {
        val vm = viewModel(CharacterSet.KANJI, FakeSpeaker(SpeakerStatus.READY))
        assertEquals(listOf(1, 2), vm.showing().grades)
        vm.next()
        vm.selectGrade(2)
        assertEquals(listOf("校"), vm.showing().characters.map { it.symbol })
        assertEquals(0, vm.showing().index)
        assertEquals(2, vm.showing().selectedGrade)
        vm.selectGrade(null)
        assertEquals(3, vm.showing().characters.size)
    }

    @Test
    fun kana_haveNoGradeFilter() {
        assertTrue(viewModel(CharacterSet.HIRAGANA, FakeSpeaker(SpeakerStatus.READY)).showing().grades.isEmpty())
    }
}
