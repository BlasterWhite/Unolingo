package fr.enssat.unolingo.mguezenn_mphongsa.ui.stats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.enssat.unolingo.mguezenn_mphongsa.R
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.CharacterSet
import fr.enssat.unolingo.mguezenn_mphongsa.domain.progress.Grade
import fr.enssat.unolingo.mguezenn_mphongsa.domain.progress.GradeConfig
import fr.enssat.unolingo.mguezenn_mphongsa.domain.progress.Rank
import fr.enssat.unolingo.mguezenn_mphongsa.ui.common.BackTopAppBar
import fr.enssat.unolingo.mguezenn_mphongsa.ui.common.PrimaryButton
import fr.enssat.unolingo.mguezenn_mphongsa.ui.common.labelRes
import fr.enssat.unolingo.mguezenn_mphongsa.ui.theme.UnolingoTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun StatsScreen(onBack: () -> Unit, onStartQuiz: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel: StatsViewModel = viewModel(factory = StatsViewModel.Factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StatsContent(state, onBack, onStartQuiz, modifier)
}

@Composable
internal fun StatsContent(state: StatsUiState, onBack: () -> Unit, onStartQuiz: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier, topBar = { BackTopAppBar(stringResource(R.string.stats_title), onBack) }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (state) {
                StatsUiState.Loading -> Unit
                StatsUiState.Empty -> EmptyProgress(onStartQuiz)
                is StatsUiState.Content -> ProgressContent(state)
            }
            if (state != StatsUiState.Loading) HowItWorks()
        }
    }
}

@Composable
private fun ProgressContent(state: StatsUiState.Content) {
    GradeHeader(state.summary.grade)
    SectionCard(title = stringResource(R.string.stats_chart_title)) {
        ProgressChart(
            points = state.points,
            currentRank = state.summary.grade.rank,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
        )
    }
    SectionCard(title = null) {
        Fact(stringResource(R.string.stats_best_rank), state.bestRank.label())
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Fact(
            stringResource(R.string.stats_window),
            stringResource(R.string.stats_window_value, state.summary.windowCount, GradeConfig.WINDOW_SIZE),
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Fact(
            stringResource(R.string.stats_before_exit),
            pluralStringResource(R.plurals.stats_quiz_count, state.quizzesBeforeExit, state.quizzesBeforeExit),
        )
    }
    SectionCard(title = stringResource(R.string.stats_history_title)) {
        val formatter = remember { DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(Locale.getDefault()) }
        val zone = remember { ZoneId.systemDefault() }
        state.recent.forEachIndexed { index, row ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            HistoryLine(row, formatter.format(Instant.ofEpochMilli(row.playedAt).atZone(zone)))
        }
    }
}

@Composable
private fun GradeHeader(grade: Grade) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            RankSeal(grade.rank, size = 72.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.grade_current), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(grade.rank.label(), style = MaterialTheme.typography.headlineMedium)
                RankProgress(grade)
            }
        }
    }
}

/** Progress bar to the next rank and its caption; shared with the home card. */
@Composable
fun RankProgress(grade: Grade, modifier: Modifier = Modifier) {
    val atTop = grade.rank.level >= GradeConfig.LEVEL_COUNT - 1
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LinearProgressIndicator(
            progress = { grade.progress.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
        Text(
            text = if (atTop) {
                stringResource(R.string.grade_top_rank)
            } else {
                stringResource(R.string.grade_next_rank, Rank(grade.rank.level + 1).label(), (grade.progress * 100).roundToInt())
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionCard(title: String?, content: @Composable () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (title != null) Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun Fact(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun HistoryLine(row: HistoryRow, date: String) {
    val game = if (row.characterSet == CharacterSet.KANJI) {
        stringResource(R.string.stats_game_kanji, row.kanjiGrade)
    } else {
        stringResource(row.characterSet.labelRes)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(game, style = MaterialTheme.typography.titleSmall)
            Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(row.rank.label(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            stringResource(R.string.stats_score, row.score, row.total),
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
private fun EmptyProgress(onStartQuiz: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RankSeal(Rank(0), size = 96.dp)
        Text(stringResource(R.string.stats_empty_title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(
            stringResource(R.string.stats_empty_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        PrimaryButton(stringResource(R.string.stats_empty_action), onClick = onStartQuiz)
    }
}

@Composable
private fun HowItWorks() {
    SectionCard(title = stringResource(R.string.stats_how_title)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(painterResource(R.drawable.ic_bar_chart), contentDescription = null, modifier = Modifier.size(24.dp))
            Text(
                text = stringResource(
                    R.string.stats_how_body,
                    GradeConfig.WINDOW_SIZE,
                    ((1 - GradeConfig.HELP_FACTOR) * 100).roundToInt(),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun StatsEmptyPreview() {
    UnolingoTheme { StatsContent(StatsUiState.Empty, onBack = {}, onStartQuiz = {}) }
}
