package bzh.enssat.unolingo.ui.battle

import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import bzh.enssat.unolingo.R
import bzh.enssat.unolingo.domain.battle.BattleGame
import bzh.enssat.unolingo.domain.battle.BattleRound
import bzh.enssat.unolingo.domain.battle.Player
import bzh.enssat.unolingo.domain.model.CharacterSet
import bzh.enssat.unolingo.domain.model.JapaneseCharacter
import bzh.enssat.unolingo.domain.quiz.Question
import bzh.enssat.unolingo.ui.common.BackTopAppBar
import bzh.enssat.unolingo.ui.common.CharacterSetSelector
import bzh.enssat.unolingo.ui.common.ChoiceGrid
import bzh.enssat.unolingo.ui.common.QuestionPrompt
import bzh.enssat.unolingo.ui.common.choiceOptions
import bzh.enssat.unolingo.ui.common.choiceState
import bzh.enssat.unolingo.ui.theme.UnolingoTheme

@Composable
fun BattleScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel: BattleViewModel = viewModel(factory = BattleViewModel.Factory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        BattleUiState.Setup -> BattleSetup(onStart = viewModel::start, onBack = onBack, modifier = modifier)
        BattleUiState.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        BattleUiState.LoadError -> Box(
            modifier = modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.quiz_load_error), textAlign = TextAlign.Center)
            BackHandler(onBack = viewModel::quit)
        }
        is BattleUiState.Playing -> SplitScreenBattle(
            game = state.game,
            onAnswer = viewModel::submitAnswer,
            onRematch = viewModel::rematch,
            onQuit = viewModel::quit,
            modifier = modifier,
        )
    }
}

@Composable
private fun BattleSetup(
    onStart: (CharacterSet) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedSet by rememberSaveable { mutableStateOf(CharacterSet.HIRAGANA) }
    Scaffold(
        modifier = modifier,
        topBar = { BackTopAppBar(title = stringResource(R.string.battle_title), onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.battle_setup_rules), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = pluralStringResource(R.plurals.battle_round_count, BattleGame.ROUND_COUNT, BattleGame.ROUND_COUNT),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(stringResource(R.string.home_choose_set), style = MaterialTheme.typography.titleMedium)
            CharacterSetSelector(selected = selectedSet, onSelect = { selectedSet = it })
            Button(onClick = { onStart(selectedSet) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.battle_start))
            }
        }
    }
}

/** Player TWO sits across the table: their half is rotated 180°. */
@Composable
private fun SplitScreenBattle(
    game: BattleGame,
    onAnswer: (Player, String) -> Unit,
    onRematch: () -> Unit,
    onQuit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KeepScreenOnAndLockOrientation()

    var confirmQuit by rememberSaveable { mutableStateOf(false) }
    BackHandler { if (game.isFinished) onQuit() else confirmQuit = true }
    if (confirmQuit) {
        AlertDialog(
            onDismissRequest = { confirmQuit = false },
            title = { Text(stringResource(R.string.battle_quit_title)) },
            text = { Text(stringResource(R.string.battle_quit_message)) },
            confirmButton = {
                TextButton(onClick = { confirmQuit = false; onQuit() }) {
                    Text(stringResource(R.string.battle_action_quit))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmQuit = false }) { Text(stringResource(R.string.battle_action_continue)) }
            },
        )
    }

    Surface(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.safeDrawingPadding()) {
            PlayerPanel(
                game = game,
                player = Player.TWO,
                onAnswer = onAnswer,
                modifier = Modifier
                    .weight(1f)
                    .rotate(180f),
            )
            CenterBar(game = game, onRematch = onRematch, onQuit = onQuit)
            PlayerPanel(
                game = game,
                player = Player.ONE,
                onAnswer = onAnswer,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PlayerPanel(
    game: BattleGame,
    player: Player,
    onAnswer: (Player, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = if (player == Player.ONE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = stringResource(if (player == Player.ONE) R.string.battle_player_one else R.string.battle_player_two),
                style = MaterialTheme.typography.titleMedium,
                color = accent,
            )
            Text(
                text = stringResource(R.string.battle_score, game.score(player), game.score(player.opponent)),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (game.isFinished) {
                ResultMessage(game, player)
            } else {
                RoundContent(game.currentRound, player, onAnswer)
            }
        }
    }
}

@Composable
private fun RoundContent(round: BattleRound, player: Player, onAnswer: (Player, String) -> Unit) {
    val question = round.question
    val chosen = round.answers[player]
    QuestionPrompt(question, characterFontSize = 56.sp)
    ChoiceGrid(
        options = question.choiceOptions,
        stateOf = { choiceState(question, it, chosen = chosen, revealed = round.isOver) },
        enabled = !round.isOver && chosen == null,
        onChoose = { onAnswer(player, it) },
        buttonHeight = 56.dp,
        japaneseFontSize = 28.sp,
        spacing = 8.dp,
    )
    val status = when {
        round.winner == player -> R.string.battle_status_point_you
        round.winner == player.opponent -> R.string.battle_status_point_opponent
        round.isOver -> R.string.battle_status_no_point
        chosen != null -> R.string.battle_status_wrong
        else -> null
    }
    // Always laid out so the grid does not move when the status appears.
    Text(
        text = status?.let { stringResource(it) } ?: "",
        style = MaterialTheme.typography.titleSmall,
        minLines = 1,
    )
}

@Composable
private fun ResultMessage(game: BattleGame, player: Player) {
    val message = when (game.winner) {
        player -> R.string.battle_result_win
        null -> R.string.battle_result_draw
        else -> R.string.battle_result_lose
    }
    Text(text = stringResource(message), style = MaterialTheme.typography.displaySmall)
    Text(
        text = stringResource(R.string.battle_score, game.score(player), game.score(player.opponent)),
        style = MaterialTheme.typography.headlineMedium,
    )
}

@Composable
private fun CenterBar(game: BattleGame, onRematch: () -> Unit, onQuit: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
        if (game.isFinished) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(onClick = onRematch, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.battle_action_rematch))
                }
                OutlinedButton(onClick = onQuit, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.battle_action_quit))
                }
            }
        } else {
            // Shown twice so that each player reads it the right way up.
            val roundText = stringResource(R.string.battle_round, game.roundNumber, game.questions.size)
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(roundText, style = MaterialTheme.typography.labelLarge, modifier = Modifier.rotate(180f))
                Text(roundText, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** The phone lies flat between the players: keep it awake and stop the sensor from rotating the screen. */
@Composable
private fun KeepScreenOnAndLockOrientation() {
    val view = LocalView.current
    val activity = LocalActivity.current
    DisposableEffect(view, activity) {
        view.keepScreenOn = true
        val previousOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        onDispose {
            view.keepScreenOn = false
            if (activity != null && previousOrientation != null) activity.requestedOrientation = previousOrientation
        }
    }
}

private val previewCharacters = listOf(
    JapaneseCharacter("ぬ", listOf("nu")),
    JapaneseCharacter("め", listOf("me")),
    JapaneseCharacter("ね", listOf("ne")),
    JapaneseCharacter("れ", listOf("re")),
)

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun SplitScreenBattlePreview() {
    val questions = listOf(
        Question.RomajiToCharacter(previewCharacters[0], "nu", previewCharacters),
        Question.CharacterToRomajiChoice(previewCharacters[1], listOf("nu", "me", "ne", "re")),
    )
    val game = BattleGame(questions).submit(Player.TWO, "め")
    UnolingoTheme {
        SplitScreenBattle(game = game, onAnswer = { _, _ -> }, onRematch = {}, onQuit = {})
    }
}
