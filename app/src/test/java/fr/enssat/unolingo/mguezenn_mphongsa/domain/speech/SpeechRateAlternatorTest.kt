package fr.enssat.unolingo.mguezenn_mphongsa.domain.speech

import org.junit.Test
import org.junit.Assert.assertEquals

class SpeechRateAlternatorTest {
    private val normal = SpeechPace.NORMAL
    private val slow = SpeechPace.SLOW

    @Test
    fun sameText_alternatesNormalThenSlow() {
        val a = SpeechRateAlternator()
        assertEquals(listOf(normal, slow, normal, slow), List(4) { a.next("あ") })
    }

    @Test
    fun textChange_restartsAtNormal() {
        val a = SpeechRateAlternator()
        a.next("あ")
        assertEquals(slow, a.next("あ"))
        assertEquals(normal, a.next("い"))
        assertEquals(slow, a.next("い"))
    }

    @Test
    fun returningToPreviousText_restartsAtNormal() {
        val a = SpeechRateAlternator()
        a.next("あ")
        a.next("い")
        assertEquals(normal, a.next("あ"))
    }

    @Test
    fun reset_restartsAtNormal() {
        val a = SpeechRateAlternator()
        a.next("あ")
        a.reset()
        assertEquals(normal, a.next("あ"))
    }
}
