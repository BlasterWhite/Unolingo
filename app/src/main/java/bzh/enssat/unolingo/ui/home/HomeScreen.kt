package bzh.enssat.unolingo.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import bzh.enssat.unolingo.R
import bzh.enssat.unolingo.domain.model.CharacterSet
import bzh.enssat.unolingo.domain.model.QuizMode
import bzh.enssat.unolingo.ui.common.CharacterSetSelector
import bzh.enssat.unolingo.ui.common.labelRes
import bzh.enssat.unolingo.ui.theme.UnolingoTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStartQuiz: (CharacterSet, QuizMode) -> Unit,
    onOpenStats: () -> Unit,
    onOpenBattle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedSet by rememberSaveable { mutableStateOf(CharacterSet.HIRAGANA) }
    var selectedMode by rememberSaveable { mutableStateOf(QuizMode.DISPLAY) }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.home_choose_set), style = MaterialTheme.typography.titleMedium)
            CharacterSetSelector(selected = selectedSet, onSelect = { selectedSet = it })

            Text(stringResource(R.string.home_choose_mode), style = MaterialTheme.typography.titleMedium)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                QuizMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = mode == selectedMode,
                        onClick = { selectedMode = mode },
                        shape = SegmentedButtonDefaults.itemShape(index, QuizMode.entries.size),
                    ) { Text(stringResource(mode.labelRes)) }
                }
            }
            Text(
                text = pluralStringResource(
                    R.plurals.home_question_count,
                    selectedMode.questionCount,
                    selectedMode.questionCount,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )

            Button(
                onClick = { onStartQuiz(selectedSet, selectedMode) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.action_start)) }
            OutlinedButton(onClick = onOpenStats, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.stats_title))
            }
            OutlinedButton(onClick = onOpenBattle, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.battle_title))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    UnolingoTheme {
        HomeScreen(onStartQuiz = { _, _ -> }, onOpenStats = {}, onOpenBattle = {})
    }
}
