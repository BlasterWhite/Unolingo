package fr.enssat.unolingo.mguezenn_mphongsa

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fr.enssat.unolingo.mguezenn_mphongsa.domain.progress.GradeCalculator
import fr.enssat.unolingo.mguezenn_mphongsa.ui.home.GradeCardTag
import fr.enssat.unolingo.mguezenn_mphongsa.ui.stats.ProgressChartTag
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * TP2 "Test des statistiques": seeds fake results (isFake = true) in the real database, then checks the grade
 * computed from the database, the progress chart and the rank shown on the home screen. Independent of every other
 * test: it removes the fake results before and after, and leaves real results alone.
 */
@RunWith(AndroidJUnit4::class)
class StatsSeedTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val repository get() = TestSupport.repository

    @Before
    fun cleanBefore() = runBlocking { repository.deleteFake() }

    @After
    fun cleanAfter() = runBlocking { repository.deleteFake() }

    @Test
    fun seededResults_giveExpectedGrade_chartAndHomeRank() {
        // Real results already on the phone stay in the database; fake ones are made the most recent so that the
        // 20-quiz window contains fake results only and the expected grade is known exactly.
        val realCount = TestSupport.countReal()
        val fakes = TestSupport.fakeOutcomes(firstPlayedAt = System.currentTimeMillis() + 3_600_000L)
        runBlocking { repository.insertFake(fakes) }
        assertEquals(TestSupport.FAKE_QUIZ_COUNT, TestSupport.countFake())

        // 1. Grade computed from the database == grade recomputed by hand.
        val expectedValue = TestSupport.expectedValue(fakes)
        val expectedLevel = TestSupport.expectedLevel(expectedValue)
        assertEquals("hand-computed value", 1.1425, expectedValue, 1e-9) // guards the fixture itself
        assertEquals("hand-computed rank level", 3, expectedLevel)
        val stored = runBlocking { repository.observeOutcomes().first() }
        assertEquals(realCount + TestSupport.FAKE_QUIZ_COUNT, stored.size)
        val grade = GradeCalculator.grade(stored)
        assertEquals("grade value", expectedValue, grade.value, 1e-9)
        assertEquals("rank level", expectedLevel, grade.rank.level)

        // 2. The rank is shown on the home screen (the card follows the database live).
        val rank = TestSupport.rankLabel(expectedLevel)
        compose.waitUntil(timeoutMillis = 10_000) {
            runCatching { compose.onNodeWithTag(GradeCardTag).assertTextContains(rank, substring = true) }.isSuccess
        }
        compose.onNodeWithTag(GradeCardTag).assertIsDisplayed()

        // 3. The progress page shows the chart, with one point per quiz and the right rank.
        compose.onNodeWithTag(GradeCardTag).performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithTag(ProgressChartTag).fetchSemanticsNodes().isNotEmpty()
        }
        val chart = compose.onNodeWithTag(ProgressChartTag)
        chart.performScrollTo().assertIsDisplayed()
        val points = realCount + TestSupport.FAKE_QUIZ_COUNT
        val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
        val expectedDescription = resources.getQuantityString(R.plurals.stats_chart_description, points, points, rank)
        val description = chart.fetchSemanticsNode().config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString()
        assertEquals(expectedDescription, description)
    }
}
