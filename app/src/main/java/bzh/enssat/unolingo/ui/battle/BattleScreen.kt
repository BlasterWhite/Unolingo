package bzh.enssat.unolingo.ui.battle

import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import bzh.enssat.unolingo.R
import bzh.enssat.unolingo.domain.battle.BattleGame
import bzh.enssat.unolingo.domain.battle.BattleRound
import bzh.enssat.unolingo.domain.battle.Player
import bzh.enssat.unolingo.domain.model.CharacterSet
import bzh.enssat.unolingo.domain.model.JapaneseCharacter
import bzh.enssat.unolingo.domain.quiz.Question
import bzh.enssat.unolingo.ui.common.AnswerMark
import bzh.enssat.unolingo.ui.common.BackTopAppBar
import bzh.enssat.unolingo.ui.common.CharacterSetSelector
import bzh.enssat.unolingo.ui.common.ChoiceGrid
import bzh.enssat.unolingo.ui.common.PlayerSeal
import bzh.enssat.unolingo.ui.common.PrimaryButton
import bzh.enssat.unolingo.ui.common.QuestionPrompt
import bzh.enssat.unolingo.ui.common.SecondaryButton
import bzh.enssat.unolingo.ui.common.choiceOptions
import bzh.enssat.unolingo.ui.common.choiceState
import bzh.enssat.unolingo.ui.common.kanoko
import bzh.enssat.unolingo.ui.common.seigaiha
import bzh.enssat.unolingo.ui.theme.KanaStyles
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
            Text(
                text = stringResource(R.string.quiz_load_error),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
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

/** Colors of a player's band, seal and score: teal (primary) for player 1, gold (tertiary) for player 2. */
private data class PlayerColors(
    val band: Color,
    val onBand: Color,
    val seal: Color,
    val pattern: Color,
    val scorePill: Color,
    val onScorePill: Color,
    /** Accent for this player's messages on the plain surface. */
    val accent: Color,
)

@Composable
private fun playerColors(player: Player): PlayerColors {
    val colors = MaterialTheme.colorScheme
    return when (player) {
        Player.ONE -> PlayerColors(
            band = colors.primary,
            onBand = colors.onPrimary,
            seal = colors.onPrimary,
            pattern = colors.onPrimary.copy(alpha = 0.16f),
            scorePill = colors.onPrimary,
            onScorePill = colors.primary,
            accent = colors.primary,
        )
        Player.TWO -> PlayerColors(
            band = colors.tertiaryContainer,
            onBand = colors.onTertiaryContainer,
            seal = colors.tertiary,
            pattern = colors.tertiary.copy(alpha = 0.35f),
            scorePill = colors.tertiary,
            onScorePill = colors.onTertiary,
            accent = colors.tertiary,
        )
    }
}

@Composable
private fun playerName(player: Player): String =
    stringResource(if (player == Player.ONE) R.string.battle_player_one else R.string.battle_player_two)

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
                .padding(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                VersusCard()
                Text(stringResource(R.string.battle_setup_rules), style = MaterialTheme.typography.bodyLarge)
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.battle_round_count,
                            BattleGame.ROUND_COUNT,
                            BattleGame.ROUND_COUNT,
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
                Text(stringResource(R.string.home_choose_set), style = MaterialTheme.typography.titleMedium)
                CharacterSetSelector(selected = selectedSet, onSelect = { selectedSet = it })
            }
            Spacer(Modifier.height(16.dp))
            PrimaryButton(stringResource(R.string.battle_start), onClick = { onStart(selectedSet) })
        }
    }
}

/** The phone seen from above, between the two players: each band in its player's colors, player 2 upside down. */
@Composable
private fun VersusCard() {
    val shape = MaterialTheme.shapes.extraLarge
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
    ) {
        VersusBand(Player.TWO, Modifier.rotate(180f))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.battle_versus), style = MaterialTheme.typography.titleSmall)
        }
        VersusBand(Player.ONE)
    }
}

@Composable
private fun VersusBand(player: Player, modifier: Modifier = Modifier) {
    val colors = playerColors(player)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp)
            .background(colors.band),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayerSeal(player, ink = colors.seal, size = 48.dp)
        Text(playerName(player), style = MaterialTheme.typography.titleLarge, color = colors.onBand)
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
            PlayerHalf(
                game = game,
                player = Player.TWO,
                onAnswer = onAnswer,
                modifier = Modifier
                    .weight(1f)
                    .rotate(180f),
            )
            CenterBar(game = game, onRematch = onRematch, onQuit = onQuit)
            PlayerHalf(
                game = game,
                player = Player.ONE,
                onAnswer = onAnswer,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** One player's half, laid out for that player: the band sits against their edge of the screen. */
@Composable
private fun PlayerHalf(
    game: BattleGame,
    player: Player,
    onAnswer: (Player, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (game.isFinished) {
                ResultMessage(game, player)
            } else {
                RoundContent(game.currentRound, player, onAnswer)
            }
        }
        PlayerBand(player, ownScore = game.score(player), opponentScore = game.score(player.opponent))
    }
}

/** Seal, name and score seen from this player's side. The pattern stays in the band, never behind the choices. */
@Composable
private fun PlayerBand(player: Player, ownScore: Int, opponentScore: Int) {
    val colors = playerColors(player)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(colors.band),
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayerSeal(player, ink = colors.seal, size = 40.dp, stampKey = ownScore)
            Text(playerName(player), style = MaterialTheme.typography.titleLarge, color = colors.onBand)
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .then(
                    if (player == Player.ONE) {
                        Modifier.seigaiha(lines = colors.pattern, background = colors.band, radius = 12.dp)
                    } else {
                        Modifier.kanoko(dots = colors.pattern, background = colors.band)
                    },
                )
                .padding(end = 16.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Surface(color = colors.scorePill, contentColor = colors.onScorePill, shape = CircleShape) {
                Text(
                    text = stringResource(R.string.battle_score, ownScore, opponentScore),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun RoundContent(round: BattleRound, player: Player, onAnswer: (Player, String) -> Unit) {
    val question = round.question
    val chosen = round.answers[player]
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        QuestionPrompt(
            question = question,
            characterStyle = KanaStyles.Duel,
            romajiStyle = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
        )
        Spacer(Modifier.weight(1f))
        RoundStatus(round, player)
    }
    ChoiceGrid(
        options = question.choiceOptions,
        stateOf = { choiceState(question, it, chosen = chosen, revealed = round.isOver) },
        enabled = !round.isOver && chosen == null,
        onChoose = { onAnswer(player, it) },
        buttonHeight = 60.dp,
        japaneseStyle = KanaStyles.Compact,
        spacing = 8.dp,
    )
}

@Composable
private fun RoundStatus(round: BattleRound, player: Player) {
    val colors = MaterialTheme.colorScheme
    val accent = playerColors(player).accent
    val (text, mark) = when {
        round.winner == player -> R.string.battle_status_point_you to (true to accent)
        round.winner == player.opponent -> R.string.battle_status_point_opponent to null
        round.isOver -> R.string.battle_status_no_point to null
        round.answers[player] != null -> R.string.battle_status_wrong to (false to colors.error)
        else -> return
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (mark != null) AnswerMark(correct = mark.first, color = mark.second, size = 18.dp)
        Text(
            text = stringResource(text),
            style = MaterialTheme.typography.titleSmall,
            color = mark?.second ?: colors.onSurfaceVariant,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun ResultMessage(game: BattleGame, player: Player) {
    val (message, color) = when (game.winner) {
        player -> R.string.battle_result_win to playerColors(player).accent
        null -> R.string.battle_result_draw to MaterialTheme.colorScheme.onSurface
        else -> R.string.battle_result_lose to MaterialTheme.colorScheme.onSurface
    }
    Text(text = stringResource(message), style = MaterialTheme.typography.displaySmall, color = color)
    Text(
        text = stringResource(R.string.battle_score, game.score(player), game.score(player.opponent)),
        style = MaterialTheme.typography.displayLarge,
    )
}

@Composable
private fun CenterBar(game: BattleGame, onRematch: () -> Unit, onQuit: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (game.isFinished) {
                SecondaryButton(
                    text = stringResource(R.string.battle_action_quit),
                    onClick = onQuit,
                    fullWidth = false,
                    icon = R.drawable.ic_close,
                )
                PrimaryButton(
                    text = stringResource(R.string.battle_action_rematch),
                    onClick = onRematch,
                    fullWidth = false,
                    icon = R.drawable.ic_replay,
                )
            } else {
                // Shown twice so that each player reads it the right way up.
                val roundText = stringResource(R.string.battle_round, game.roundNumber, game.questions.size)
                Text(roundText, style = MaterialTheme.typography.labelLarge, modifier = Modifier.rotate(180f))
                RoundTrack(game)
                Text(roundText, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** One dot per round: the winner's color for played rounds, ink for the current one, light for the rest. */
@Composable
private fun RoundTrack(game: BattleGame) {
    val colors = MaterialTheme.colorScheme
    val one = playerColors(Player.ONE).accent
    val two = playerColors(Player.TWO).accent
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(game.questions.size) { index ->
            val round = game.rounds.getOrNull(index)
            val color = when {
                round == null -> colors.outlineVariant
                !round.isOver -> colors.onSurface
                round.winner == Player.ONE -> one
                round.winner == Player.TWO -> two
                else -> colors.onSurfaceVariant.copy(alpha = 0.5f)
            }
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color),
            )
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
    JapaneseCharacter("ク", listOf("ku")),
    JapaneseCharacter("ケ", listOf("ke")),
    JapaneseCharacter("タ", listOf("ta")),
    JapaneseCharacter("ナ", listOf("na")),
)

private val previewGame = BattleGame(
    List(10) { Question.RomajiToCharacter(previewCharacters[1], "ke", previewCharacters) },
)
    .submit(Player.ONE, "ケ").nextRound()
    .submit(Player.TWO, "ケ").nextRound()
    .submit(Player.ONE, "ケ")

@Preview(showBackground = true, heightDp = 840)
@Composable
private fun SplitScreenBattlePreview() {
    UnolingoTheme {
        SplitScreenBattle(game = previewGame, onAnswer = { _, _ -> }, onRematch = {}, onQuit = {})
    }
}

@Preview(showBackground = true, heightDp = 840)
@Composable
private fun SplitScreenBattleDarkPreview() {
    UnolingoTheme(darkTheme = true) {
        SplitScreenBattle(game = previewGame, onAnswer = { _, _ -> }, onRematch = {}, onQuit = {})
    }
}

@Preview(showBackground = true, heightDp = 840)
@Composable
private fun BattleSetupPreview() {
    UnolingoTheme { BattleSetup(onStart = {}, onBack = {}) }
}
