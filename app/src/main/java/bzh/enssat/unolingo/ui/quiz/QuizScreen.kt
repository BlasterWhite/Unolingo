package bzh.enssat.unolingo.ui.quiz

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
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
import bzh.enssat.unolingo.ui.common.AnswerMark
import bzh.enssat.unolingo.ui.common.BackTopAppBar
import bzh.enssat.unolingo.ui.common.ChoiceGrid
import bzh.enssat.unolingo.ui.common.Hanamaru
import bzh.enssat.unolingo.ui.common.JapaneseText
import bzh.enssat.unolingo.ui.common.PrimaryButton
import bzh.enssat.unolingo.ui.common.QuestionPrompt
import bzh.enssat.unolingo.ui.common.SecondaryButton
import bzh.enssat.unolingo.ui.common.appendMixed
import bzh.enssat.unolingo.ui.common.choiceOptions
import bzh.enssat.unolingo.ui.common.choiceState
import bzh.enssat.unolingo.ui.common.instructionRes
import bzh.enssat.unolingo.ui.common.labelRes
import bzh.enssat.unolingo.ui.theme.KanaStyles
import bzh.enssat.unolingo.ui.theme.UnolingoTheme

/** Material 3 "emphasized decelerate": used for elements entering the screen. */
internal val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

@Composable
fun QuizScreen(
    characterSet: CharacterSet,
    mode: QuizMode,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: QuizViewModel = viewModel(factory = QuizViewModel.factory(characterSet, mode))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    // Coming back from the voice installation screen: check the voice again.
    LifecycleResumeEffect(viewModel) {
        viewModel.retryVoice()
        onPauseOrDispose {}
    }
    val setLabel = stringResource(characterSet.labelRes)
    QuizContent(
        title = setLabel,
        subtitle = stringResource(R.string.quiz_result_subtitle, setLabel, stringResource(mode.labelRes)),
        uiState = uiState,
        isSpeaking = isSpeaking,
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
internal data class QuizActions(
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
    subtitle: String,
    uiState: QuizUiState,
    isSpeaking: Boolean,
    actions: QuizActions,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        // The result screen has no top bar (docs/ui "Résultat").
        topBar = { if (uiState !is QuizUiState.Finished) BackTopAppBar(title = title, onBack = onBack) },
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .consumeWindowInsets(innerPadding)
            .imePadding()
        when (uiState) {
            QuizUiState.Loading -> LoadingContent(contentModifier)
            QuizUiState.LoadError -> LoadErrorContent(onBack, contentModifier)
            QuizUiState.MissingVoice -> MissingVoiceContent(actions.onRetryVoice, contentModifier)
            QuizUiState.SpeechUnavailable -> SpeechUnavailableContent(actions.onRetryVoice, contentModifier)
            is QuizUiState.Answering -> AnsweringContent(uiState, isSpeaking, actions, contentModifier)
            is QuizUiState.Finished -> FinishedContent(uiState, subtitle, actions.onRestart, onBack, contentModifier)
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Text(stringResource(R.string.quiz_loading), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun LoadErrorContent(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(16.dp)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.quiz_load_error),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
        }
        SecondaryButton(stringResource(R.string.quiz_action_home), onBack)
    }
}

@Composable
private fun AnsweringContent(
    state: QuizUiState.Answering,
    isSpeaking: Boolean,
    actions: QuizActions,
    modifier: Modifier = Modifier,
) {
    val distance = with(LocalDensity.current) { 24.dp.roundToPx() }
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        ProgressHeader(state)
        AnimatedContent(
            targetState = state,
            contentKey = { it.number },
            transitionSpec = {
                // Question suivante (280 ms): the current question leaves to the left, the next one comes in.
                val spec = tween<Float>(280)
                (slideInHorizontally(tween(280)) { distance } + fadeIn(spec))
                    .togetherWith(slideOutHorizontally(tween(280)) { -distance } + fadeOut(spec))
            },
            modifier = Modifier.weight(1f),
            label = "question",
        ) { shown ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                QuestionHeading(shown.question)
                when (val question = shown.question) {
                    is Question.CharacterToRomaji -> CharacterToRomajiContent(question, shown.feedback, actions.onSubmit)
                    is Question.RomajiToCharacter, is Question.CharacterToRomajiChoice ->
                        ChoiceQuestionContent(question, shown.feedback, actions.onSubmit)
                    is Question.SoundToCharacter ->
                        SoundToCharacterContent(question, shown.feedback, isSpeaking, actions.onSubmit, actions.onReplay)
                    is Question.CharacterToSound ->
                        CharacterToSoundContent(question, shown.feedback, actions.onSubmit, actions.onPlayReading)
                }
            }
        }
        state.feedback?.let { feedback ->
            Column(
                modifier = Modifier.padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                FeedbackCard(feedback)
                PrimaryButton(
                    text = stringResource(
                        if (state.number == state.total) R.string.quiz_action_see_result else R.string.quiz_action_next,
                    ),
                    onClick = actions.onNext,
                )
            }
        }
    }
}

@Composable
private fun ProgressHeader(state: QuizUiState.Answering) {
    val progress by animateFloatAsState(state.number.toFloat() / state.total, tween(280), label = "progress")
    Column(modifier = Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = stringResource(R.string.quiz_progress, state.number, state.total),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(R.string.quiz_score, state.score),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
    }
}

/** Instruction, then the meaning of a kanji. */
@Composable
private fun QuestionHeading(question: Question) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = stringResource(question.instructionRes), style = MaterialTheme.typography.headlineSmall)
        // Hearing a sound and seeing its meaning would give the answer away.
        if (question !is Question.SoundToCharacter) MeaningText(question.character)
    }
}

/** Washi panel holding the prompt of a question. Nothing decorative is drawn in it. */
@Composable
internal fun PromptPanel(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(vertical = 24.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center,
        content = content,
    )
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

    PromptPanel { QuestionPrompt(question, characterStyle = KanaStyles.Hero) }
    OutlinedTextField(
        value = answer,
        onValueChange = { answer = it },
        label = { Text(stringResource(R.string.quiz_answer_label)) },
        singleLine = true,
        enabled = !answered,
        textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Normal),
        shape = MaterialTheme.shapes.small,
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
        PrimaryButton(stringResource(R.string.quiz_action_validate), onClick = submit, enabled = answer.isNotBlank())
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
    PromptPanel { QuestionPrompt(question, characterStyle = KanaStyles.Hero) }
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

/** Correction card: rises 16 dp and fades in (220 ms) when the answer is checked. */
@Composable
private fun FeedbackCard(feedback: QuestionResult) {
    val colors = MaterialTheme.colorScheme
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(durationMillis = 220, easing = EmphasizedDecelerate)) }
    val rise = with(LocalDensity.current) { 16.dp.toPx() }
    val character = feedback.question.character
    Surface(
        color = if (feedback.isCorrect) colors.primaryContainer else colors.errorContainer,
        contentColor = if (feedback.isCorrect) colors.onPrimaryContainer else colors.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = appear.value
                translationY = (1 - appear.value) * rise
            },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnswerMark(
                correct = feedback.isCorrect,
                color = if (feedback.isCorrect) colors.primary else colors.error,
                size = 32.dp,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(
                        if (feedback.isCorrect) R.string.quiz_feedback_correct else R.string.quiz_feedback_incorrect,
                    ),
                    style = MaterialTheme.typography.titleLarge,
                )
                val readsAs = stringResource(R.string.quiz_reads_as, character.readings.joinToString(" / "))
                Text(
                    text = buildAnnotatedString {
                        appendMixed(character.symbol)
                        append(" ")
                        append(readsAs)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
private fun FinishedContent(
    state: QuizUiState.Finished,
    subtitle: String,
    onRestart: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(16.dp)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                text = stringResource(R.string.quiz_result_title),
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.padding(top = 16.dp),
            )
            val scoreText = stringResource(R.string.quiz_result_score, state.score, state.total)
            if (state.mistakes.isEmpty()) {
                Hanamaru(
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier
                        .padding(vertical = 24.dp)
                        .size(260.dp),
                ) {
                    Text(scoreText, style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold))
                }
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    shape = MaterialTheme.shapes.extraLarge,
                ) {
                    Text(
                        text = stringResource(R.string.quiz_result_perfect),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    )
                }
            } else {
                Text(
                    text = scoreText,
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp, lineHeight = 80.sp),
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = pluralStringResource(R.plurals.quiz_result_encouragement, state.mistakes.size, state.mistakes.size),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.quiz_result_mistakes),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                )
                state.mistakes.forEach { MistakeRow(it) }
            }
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton(stringResource(R.string.quiz_action_restart), onClick = onRestart)
        Spacer(Modifier.height(12.dp))
        SecondaryButton(stringResource(R.string.quiz_action_home), onClick = onBack)
    }
}

@Composable
private fun MistakeRow(result: QuestionResult) {
    val character = result.question.character
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(colors.surfaceContainer)
            .border(1.dp, colors.outlineVariant, MaterialTheme.shapes.medium)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .heightIn(min = 64.dp)
                .widthIn(min = 64.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(colors.surface)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Yōon (きゃ…) are two characters: a smaller size keeps the tile compact.
            JapaneseText(text = character.symbol, style = if (character.symbol.length > 1) KanaStyles.Compact else KanaStyles.List)
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(character.readings.joinToString(" / "), style = MaterialTheme.typography.titleMedium)
            val label = stringResource(R.string.quiz_given_answer)
            Text(
                text = buildAnnotatedString {
                    append(label)
                    append(" ")
                    withStyle(SpanStyle(color = colors.error, fontWeight = FontWeight.SemiBold)) { appendMixed(result.answer) }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

private val previewCharacters = listOf(
    JapaneseCharacter("め", listOf("me")),
    JapaneseCharacter("ぬ", listOf("nu")),
    JapaneseCharacter("ね", listOf("ne")),
    JapaneseCharacter("の", listOf("no")),
)

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun RomajiToCharacterPreview() {
    val question = Question.RomajiToCharacter(previewCharacters[1], "nu", previewCharacters)
    UnolingoTheme {
        QuizContent(
            title = "Hiragana",
            subtitle = "Hiragana · Affichage",
            uiState = QuizUiState.Answering(
                number = 4,
                total = 10,
                score = 2,
                question = question,
                feedback = QuestionResult(question, "め", isCorrect = false),
            ),
            isSpeaking = false,
            actions = QuizActions.None,
            onBack = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun FinishedPreview() {
    val question = Question.CharacterToRomaji(previewCharacters[1])
    UnolingoTheme {
        QuizContent(
            title = "Hiragana",
            subtitle = "Hiragana · Affichage",
            uiState = QuizUiState.Finished(
                score = 9,
                total = 10,
                mistakes = listOf(QuestionResult(question, "mu", isCorrect = false)),
            ),
            isSpeaking = false,
            actions = QuizActions.None,
            onBack = {},
        )
    }
}
