package bzh.enssat.unolingo.ui.quiz

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import bzh.enssat.unolingo.R
import bzh.enssat.unolingo.domain.model.CharacterSet
import bzh.enssat.unolingo.domain.model.JapaneseCharacter
import bzh.enssat.unolingo.domain.model.QuizMode
import bzh.enssat.unolingo.domain.quiz.Question
import bzh.enssat.unolingo.domain.quiz.QuestionResult
import bzh.enssat.unolingo.ui.common.BackTopAppBar
import bzh.enssat.unolingo.ui.common.ChoiceGrid
import bzh.enssat.unolingo.ui.common.QuestionPrompt
import bzh.enssat.unolingo.ui.common.choiceOptions
import bzh.enssat.unolingo.ui.common.choiceState
import bzh.enssat.unolingo.ui.common.instructionRes
import bzh.enssat.unolingo.ui.common.JapaneseText
import bzh.enssat.unolingo.ui.common.labelRes
import bzh.enssat.unolingo.ui.theme.UnolingoTheme

@Composable
fun QuizScreen(
    characterSet: CharacterSet,
    mode: QuizMode,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: QuizViewModel = viewModel(factory = QuizViewModel.factory(characterSet, mode))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Coming back from the voice installation screen: check the voice again.
    LifecycleResumeEffect(viewModel) {
        viewModel.retryVoice()
        onPauseOrDispose {}
    }
    QuizContent(
        title = stringResource(characterSet.labelRes),
        uiState = uiState,
        actions = QuizActions(
            onSubmit = viewModel::submitAnswer,
            onNext = viewModel::next,
            onRestart = viewModel::restart,
            onReplay = viewModel::replay,
            onPlayReading = viewModel::playReading,
            onRetryVoice = viewModel::retryVoice,
        ),
        onBack = onBack,
        modifier = modifier,
    )
}

/** User actions of the quiz screen, grouped to keep the composables' signatures short. */
private data class QuizActions(
    val onSubmit: (String) -> Unit,
    val onNext: () -> Unit,
    val onRestart: () -> Unit,
    val onReplay: () -> Unit,
    val onPlayReading: (String) -> Unit,
    val onRetryVoice: () -> Unit,
) {
    companion object {
        val None = QuizActions({}, {}, {}, {}, {}, {})
    }
}

@Composable
private fun QuizContent(
    title: String,
    uiState: QuizUiState,
    actions: QuizActions,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { BackTopAppBar(title = title, onBack = onBack) },
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .consumeWindowInsets(innerPadding)
            .imePadding()
        when (uiState) {
            QuizUiState.Loading -> Box(contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            QuizUiState.LoadError -> Box(contentModifier.padding(16.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.quiz_load_error), textAlign = TextAlign.Center)
            }
            QuizUiState.MissingVoice -> MissingVoiceContent(actions.onRetryVoice, contentModifier)
            QuizUiState.SpeechUnavailable -> SpeechUnavailableContent(actions.onRetryVoice, contentModifier)
            is QuizUiState.Answering -> AnsweringContent(uiState, actions, contentModifier)
            is QuizUiState.Finished -> FinishedContent(uiState, actions.onRestart, onBack, contentModifier)
        }
    }
}

@Composable
private fun AnsweringContent(
    state: QuizUiState.Answering,
    actions: QuizActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = stringResource(R.string.quiz_progress, state.number, state.total),
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = stringResource(R.string.quiz_score, state.score),
                style = MaterialTheme.typography.labelLarge,
            )
        }
        LinearProgressIndicator(
            progress = { state.number.toFloat() / state.total },
            modifier = Modifier.fillMaxWidth(),
        )

        when (val question = state.question) {
            is Question.CharacterToRomaji -> CharacterToRomajiContent(question, state.feedback, actions.onSubmit)
            is Question.RomajiToCharacter, is Question.CharacterToRomajiChoice ->
                ChoiceQuestionContent(question, state.feedback, actions.onSubmit)
            is Question.SoundToCharacter ->
                SoundToCharacterContent(question, state.feedback, actions.onSubmit, actions.onReplay)
            is Question.CharacterToSound ->
                CharacterToSoundContent(question, state.feedback, actions.onSubmit, actions.onPlayReading)
        }

        state.feedback?.let { feedback ->
            FeedbackCard(feedback)
            Button(onClick = actions.onNext, modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(
                        if (state.number == state.total) R.string.quiz_action_see_result else R.string.quiz_action_next,
                    ),
                )
            }
        }
    }
}

@Composable
private fun CharacterToRomajiContent(
    question: Question.CharacterToRomaji,
    feedback: QuestionResult?,
    onSubmit: (String) -> Unit,
) {
    val answered = feedback != null
    var answer by rememberSaveable(question.character.symbol) { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val submit = { if (answer.isNotBlank() && !answered) onSubmit(answer) }

    Text(
        text = stringResource(question.instructionRes),
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
    )
    QuestionPrompt(question, characterFontSize = 96.sp)
    MeaningText(question.character)
    OutlinedTextField(
        value = answer,
        onValueChange = { answer = it },
        label = { Text(stringResource(R.string.quiz_answer_label)) },
        singleLine = true,
        enabled = !answered,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Ascii,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { submit() }),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
    )
    if (!answered) {
        Button(onClick = submit, enabled = answer.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.quiz_action_validate))
        }
    }

    LaunchedEffect(question) {
        if (!answered) focusRequester.requestFocus()
    }
}

@Composable
private fun ChoiceQuestionContent(
    question: Question,
    feedback: QuestionResult?,
    onSubmit: (String) -> Unit,
) {
    Text(
        text = stringResource(question.instructionRes),
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
    )
    QuestionPrompt(question, characterFontSize = 96.sp)
    MeaningText(question.character)
    ChoiceGrid(
        options = question.choiceOptions,
        stateOf = { choiceState(question, it, chosen = feedback?.answer, revealed = feedback != null) },
        enabled = feedback == null,
        onChoose = onSubmit,
    )
}

@Composable
internal fun MeaningText(character: JapaneseCharacter) {
    character.meaning?.let {
        Text(
            text = stringResource(R.string.quiz_meaning, it),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FeedbackCard(feedback: QuestionResult) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (feedback.isCorrect) colorScheme.primaryContainer else colorScheme.errorContainer,
            contentColor = if (feedback.isCorrect) colorScheme.onPrimaryContainer else colorScheme.onErrorContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(
                    if (feedback.isCorrect) R.string.quiz_feedback_correct else R.string.quiz_feedback_incorrect,
                ),
                style = MaterialTheme.typography.titleMedium,
            )
            val character = feedback.question.character
            JapaneseText(
                text = stringResource(R.string.quiz_answer_detail, character.symbol, character.readings.joinToString(" / ")),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun FinishedContent(
    state: QuizUiState.Finished,
    onRestart: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.quiz_result_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            text = stringResource(R.string.quiz_result_score, state.score, state.total),
            style = MaterialTheme.typography.displayMedium,
        )
        if (state.mistakes.isEmpty()) {
            Text(stringResource(R.string.quiz_result_perfect), style = MaterialTheme.typography.titleMedium)
        } else {
            Text(
                text = stringResource(R.string.quiz_result_mistakes),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
            )
            state.mistakes.forEach { MistakeRow(it) }
        }
        Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.quiz_action_restart))
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.quiz_action_home))
        }
    }
}

@Composable
private fun MistakeRow(result: QuestionResult) {
    val character = result.question.character
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            JapaneseText(text = character.symbol, fontSize = 36.sp)
            Column {
                Text(character.readings.joinToString(" / "), style = MaterialTheme.typography.titleMedium)
                JapaneseText(
                    text = stringResource(R.string.quiz_given_answer, result.answer),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

private val previewCharacters = listOf(
    JapaneseCharacter("あ", listOf("a")),
    JapaneseCharacter("お", listOf("o")),
    JapaneseCharacter("め", listOf("me")),
    JapaneseCharacter("ぬ", listOf("nu")),
)

@Preview(showBackground = true)
@Composable
private fun RomajiToCharacterPreview() {
    val question = Question.RomajiToCharacter(previewCharacters[3], "nu", previewCharacters)
    UnolingoTheme {
        QuizContent(
            title = "Hiragana",
            uiState = QuizUiState.Answering(
                number = 3,
                total = 10,
                score = 2,
                question = question,
                feedback = QuestionResult(question, "め", isCorrect = false),
            ),
            actions = QuizActions.None,
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FinishedPreview() {
    val question = Question.CharacterToRomaji(previewCharacters[2])
    UnolingoTheme {
        QuizContent(
            title = "Hiragana",
            uiState = QuizUiState.Finished(
                score = 9,
                total = 10,
                mistakes = listOf(QuestionResult(question, "nu", isCorrect = false)),
            ),
            actions = QuizActions.None,
            onBack = {},
        )
    }
}
