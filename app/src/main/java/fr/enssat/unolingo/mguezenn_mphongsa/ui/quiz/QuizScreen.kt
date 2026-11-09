package fr.enssat.unolingo.mguezenn_mphongsa.ui.quiz

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.enssat.unolingo.mguezenn_mphongsa.R
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.JapaneseCharacter
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.QuizConfig
import fr.enssat.unolingo.mguezenn_mphongsa.domain.quiz.Question
import fr.enssat.unolingo.mguezenn_mphongsa.domain.quiz.QuestionResult
import fr.enssat.unolingo.mguezenn_mphongsa.ui.common.AnswerMark
import fr.enssat.unolingo.mguezenn_mphongsa.ui.common.BackTopAppBar
import fr.enssat.unolingo.mguezenn_mphongsa.ui.common.ChoiceGrid
import fr.enssat.unolingo.mguezenn_mphongsa.ui.common.Hanamaru
import fr.enssat.unolingo.mguezenn_mphongsa.ui.common.JapaneseText
import fr.enssat.unolingo.mguezenn_mphongsa.ui.common.PrimaryButton
import fr.enssat.unolingo.mguezenn_mphongsa.ui.common.QuestionPrompt
import fr.enssat.unolingo.mguezenn_mphongsa.ui.common.SecondaryButton
import fr.enssat.unolingo.mguezenn_mphongsa.ui.common.appendMixed
import fr.enssat.unolingo.mguezenn_mphongsa.ui.common.choiceOptions
import fr.enssat.unolingo.mguezenn_mphongsa.ui.common.choiceState
import fr.enssat.unolingo.mguezenn_mphongsa.ui.common.instructionRes
import fr.enssat.unolingo.mguezenn_mphongsa.ui.common.labelRes
import fr.enssat.unolingo.mguezenn_mphongsa.ui.theme.KanaStyles
import fr.enssat.unolingo.mguezenn_mphongsa.ui.theme.UnolingoTheme

@Composable
fun QuizScreen(
    config: QuizConfig,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: QuizViewModel = viewModel(factory = QuizViewModel.factory(config))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    // Coming back from the voice installation screen: check the voice again.
    LifecycleResumeEffect(viewModel) {
        viewModel.retryVoice()
        onPauseOrDispose {}
    }
    val setLabel = stringResource(config.characterSet.labelRes)
    QuizContent(
        title = setLabel,
        subtitle = stringResource(R.string.quiz_result_subtitle, setLabel, stringResource(config.questionKind.labelRes)),
        uiState = uiState,
        isSpeaking = isSpeaking,
        actions = QuizActions(
            onSubmit = viewModel::submitAnswer,
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
    val onRestart: () -> Unit,
    val onReplay: () -> Unit,
    val onPlayReading: (String) -> Unit,
    val onRetryVoice: () -> Unit,
) {
    companion object {
        val None = QuizActions({}, {}, {}, {}, {})
    }
}

@Composable
internal fun QuizContent(
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
                    is Question.RomajiToCharacter, is Question.CharacterToRomajiChoice,
                    is Question.CharacterToMeaning, is Question.MeaningToCharacter ->
                        ChoiceQuestionContent(question, shown.feedback, actions.onSubmit)
                    is Question.SoundToCharacter ->
                        SoundToCharacterContent(question, shown.feedback, isSpeaking, actions.onSubmit, actions.onReplay)
                    is Question.CharacterToSound ->
                        CharacterToSoundContent(question, shown.feedback, actions.onSubmit, actions.onPlayReading)
                }
            }
        }
    }
}

/** "Question 3 / 10", the score, then one dot per question (right ◯, wrong ✕, not answered yet: empty). */
@Composable
private fun ProgressHeader(state: QuizUiState.Answering) {
    Column(modifier = Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
        ResultDots(state.outcomes, current = state.number - 1)
    }
}

@Composable
private fun ResultDots(outcomes: List<Boolean?>, current: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        outcomes.forEachIndexed { index, outcome ->
            ResultDot(index + 1, outcome, isCurrent = index == current, modifier = Modifier.weight(1f))
        }
    }
}

/** The mark is drawn by [AnswerMark] so the status never relies on color alone; the current question has a thick ring. */
@Composable
private fun ResultDot(number: Int, outcome: Boolean?, isCurrent: Boolean, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val status = stringResource(
        when (outcome) {
            true -> R.string.quiz_dot_correct
            false -> R.string.quiz_dot_incorrect
            null -> if (isCurrent) R.string.quiz_dot_current else R.string.quiz_dot_pending
        },
    )
    val description = stringResource(R.string.quiz_dot_description, number, status)
    Box(
        modifier = modifier
            .widthIn(max = 36.dp)
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(
                when (outcome) {
                    true -> colors.primaryContainer
                    false -> colors.errorContainer
                    null -> colors.surface
                },
            )
            .border(
                width = if (isCurrent) 2.dp else 1.dp,
                color = if (isCurrent) colors.primary else colors.outlineVariant,
                shape = CircleShape,
            )
            .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        if (outcome != null) {
            // Only the dot of the question just answered traces its mark.
            key(outcome) {
                AnswerMark(
                    correct = outcome,
                    color = if (outcome) colors.primary else colors.error,
                    size = 14.dp,
                    animate = isCurrent,
                )
            }
        }
    }
}

/** Instruction, then the help text when the quiz has help enabled. */
@Composable
private fun QuestionHeading(question: Question) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = stringResource(question.instructionRes), style = MaterialTheme.typography.headlineSmall)
        question.help?.let { help ->
            Text(
                text = buildAnnotatedString { appendMixed(stringResource(R.string.quiz_help, help)) },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
            Text(
                text = buildAnnotatedString { appendMixed(character.kanaReadings.all.ifEmpty { character.readings }.joinToString(" / ")) },
                style = MaterialTheme.typography.titleMedium,
            )
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
    val question = Question.CharacterToRomajiChoice(previewCharacters[1], listOf("nu", "ne", "no", "me"))
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
