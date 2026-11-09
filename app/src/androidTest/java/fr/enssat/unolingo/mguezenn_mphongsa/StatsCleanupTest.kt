package fr.enssat.unolingo.mguezenn_mphongsa

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * TP2: the cleanup removes the fake results (isFake = true) and nothing else. A real "witness" result is inserted
 * first and must survive; it is deleted at the end. Independent of every other test.
 */
@RunWith(AndroidJUnit4::class)
class StatsCleanupTest {
    private val repository get() = TestSupport.repository

    @Before
    fun prepare() {
        TestSupport.deleteWitnesses() // witness left by a crashed run
        runBlocking { repository.deleteFake() }
    }

    @After
    fun removeWitnessAndFakes() {
        TestSupport.deleteWitnesses()
        runBlocking { repository.deleteFake() }
    }

    @Test
    fun cleanup_removesOnlyFakeResults() = runBlocking {
        val realBefore = TestSupport.countReal()
        val witnessId = TestSupport.insertWitness()
        repository.insertFake(TestSupport.fakeOutcomes(firstPlayedAt = System.currentTimeMillis()))
        assertEquals(TestSupport.FAKE_QUIZ_COUNT, TestSupport.countFake())
        assertEquals(realBefore + 1, TestSupport.countReal())

        repository.deleteFake()

        assertEquals("fake results are gone", 0, TestSupport.countFake())
        assertTrue("the real witness is still there", TestSupport.witnessExists(witnessId))
        assertEquals("real results are untouched", realBefore + 1, TestSupport.countReal())

        TestSupport.deleteWitnesses()
        assertEquals("witness removed", realBefore, TestSupport.countReal())
    }
}
