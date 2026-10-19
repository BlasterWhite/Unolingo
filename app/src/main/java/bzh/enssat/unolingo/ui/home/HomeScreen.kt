package bzh.enssat.unolingo.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import bzh.enssat.unolingo.R
import bzh.enssat.unolingo.domain.battle.Player
import bzh.enssat.unolingo.domain.model.CharacterSet
import bzh.enssat.unolingo.domain.model.QuizMode
import bzh.enssat.unolingo.ui.common.ChoiceButton
import bzh.enssat.unolingo.ui.common.ChoiceState
import bzh.enssat.unolingo.ui.common.JapaneseText
import bzh.enssat.unolingo.ui.common.PlayerSeal
import bzh.enssat.unolingo.ui.common.PrimaryButton
import bzh.enssat.unolingo.ui.common.labelRes
import bzh.enssat.unolingo.ui.common.seigaiha
import bzh.enssat.unolingo.ui.theme.KanaStyles
import bzh.enssat.unolingo.ui.theme.UnolingoTheme

@Composable
fun HomeScreen(
    onStartQuiz: (CharacterSet, QuizMode) -> Unit,
    onOpenStats: () -> Unit,
    onOpenBattle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
    val counts by viewModel.characterCounts.collectAsStateWithLifecycle()
    HomeContent(counts, onStartQuiz, onOpenStats, onOpenBattle, modifier)
}

@Composable
private fun HomeContent(
    counts: Map<CharacterSet, Int>,
    onStartQuiz: (CharacterSet, QuizMode) -> Unit,
    onOpenStats: () -> Unit,
    onOpenBattle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedSet by rememberSaveable { mutableStateOf(CharacterSet.HIRAGANA) }
    var selectedMode by rememberSaveable { mutableStateOf(QuizMode.DISPLAY) }

    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AppHeader()
            HeroBanner()

            Text(stringResource(R.string.home_choose_set), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CharacterSet.entries.forEach { set ->
                    CharacterSetCard(
                        set = set,
                        count = counts[set],
                        selected = set == selectedSet,
                        onClick = { selectedSet = set },
                        modifier = Modifier
                            .weight(1f)
                            .height(124.dp),
                    )
                }
            }

            Text(stringResource(R.string.home_choose_mode), style = MaterialTheme.typography.titleMedium)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    QuizMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = mode == selectedMode,
                            onClick = { selectedMode = mode },
                            shape = SegmentedButtonDefaults.itemShape(index, QuizMode.entries.size),
                            modifier = Modifier.height(52.dp),
                        ) { Text(stringResource(mode.labelRes), style = MaterialTheme.typography.titleMedium) }
                    }
                }
                Text(
                    text = pluralStringResource(
                        R.plurals.home_question_count,
                        selectedMode.questionCount,
                        selectedMode.questionCount,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            PrimaryButton(stringResource(R.string.action_start), onClick = { onStartQuiz(selectedSet, selectedMode) })

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BattleCard(
                    onClick = onOpenBattle,
                    modifier = Modifier
                        .weight(1.8f)
                        .height(88.dp),
                )
                StatsCard(
                    onClick = onOpenStats,
                    modifier = Modifier
                        .weight(1f)
                        .height(88.dp),
                )
            }
        }
    }
}

@Composable
private fun AppHeader() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            JapaneseText("う", style = KanaStyles.Compact, color = MaterialTheme.colorScheme.onPrimary)
        }
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
    }
}

/** Welcome banner: text on plain primary, seigaiha waves only on the right side, behind the あ seal. */
@Composable
private fun HeroBanner() {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(136.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(colors.primary),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 20.dp, end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        ) {
            Text(
                text = stringResource(R.string.home_hero_eyebrow),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onPrimary.copy(alpha = 0.8f),
            )
            Text(
                text = stringResource(R.string.home_hero_title),
                style = MaterialTheme.typography.titleLarge,
                color = colors.onPrimary,
            )
        }
        Box(
            modifier = Modifier
                .width(128.dp)
                .fillMaxHeight()
                .seigaiha(lines = colors.onPrimary.copy(alpha = 0.18f), background = colors.primary),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(colors.surface),
                contentAlignment = Alignment.Center,
            ) {
                JapaneseText("あ", style = KanaStyles.Choice, color = colors.onSurface)
            }
        }
    }
}

@Composable
private fun CharacterSetCard(
    set: CharacterSet,
    count: Int?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ChoiceButton(
        state = if (selected) ChoiceState.SELECTED else ChoiceState.NEUTRAL,
        enabled = true,
        onClick = onClick,
        modifier = modifier,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            JapaneseText(
                text = when (set) {
                    CharacterSet.HIRAGANA -> "あ"
                    CharacterSet.KATAKANA -> "ア"
                    CharacterSet.KANJI -> "日"
                },
                style = KanaStyles.Choice,
            )
            Text(stringResource(set.labelRes), style = MaterialTheme.typography.titleSmall, maxLines = 1)
            Text(
                text = count?.let { pluralStringResource(R.plurals.home_set_count, it, it) } ?: "",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun BattleCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    HomeTile(onClick = onClick, modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
            Box(modifier = Modifier.width(64.dp)) {
                PlayerSeal(Player.ONE, ink = colors.onPrimary, fill = colors.primary, border = colors.primary, size = 40.dp)
                PlayerSeal(
                    Player.TWO,
                    ink = colors.tertiary,
                    fill = colors.tertiaryContainer,
                    size = 40.dp,
                    modifier = Modifier.offset(x = 24.dp),
                )
            }
            Column(modifier = Modifier.padding(start = 8.dp)) {
                Text(stringResource(R.string.battle_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.home_battle_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatsCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    HomeTile(onClick = onClick, modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(painterResource(R.drawable.ic_bar_chart), contentDescription = null)
            Text(stringResource(R.string.stats_title), style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun HomeTile(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

@Preview(showBackground = true, heightDp = 860)
@Composable
private fun HomePreview() {
    UnolingoTheme {
        HomeContent(
            counts = mapOf(CharacterSet.HIRAGANA to 104, CharacterSet.KATAKANA to 104, CharacterSet.KANJI to 49),
            onStartQuiz = { _, _ -> },
            onOpenStats = {},
            onOpenBattle = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 860)
@Composable
private fun HomeDarkPreview() {
    UnolingoTheme(darkTheme = true) {
        HomeContent(emptyMap(), onStartQuiz = { _, _ -> }, onOpenStats = {}, onOpenBattle = {})
    }
}
