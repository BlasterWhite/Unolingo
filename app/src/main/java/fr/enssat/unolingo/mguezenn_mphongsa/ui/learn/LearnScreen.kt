package fr.enssat.unolingo.mguezenn_mphongsa.ui.learn

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.enssat.unolingo.mguezenn_mphongsa.R
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.CharacterSet
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.JapaneseCharacter
import fr.enssat.unolingo.mguezenn_mphongsa.ui.common.BackTopAppBar
import fr.enssat.unolingo.mguezenn_mphongsa.ui.common.JapaneseText
import fr.enssat.unolingo.mguezenn_mphongsa.ui.common.SecondaryButton
import fr.enssat.unolingo.mguezenn_mphongsa.ui.common.labelRes
import fr.enssat.unolingo.mguezenn_mphongsa.ui.quiz.MissingVoiceContent
import fr.enssat.unolingo.mguezenn_mphongsa.ui.quiz.SpeechUnavailableContent
import fr.enssat.unolingo.mguezenn_mphongsa.ui.theme.KanaStyles
import fr.enssat.unolingo.mguezenn_mphongsa.ui.theme.UnolingoTheme

@Composable
fun LearnScreen(
    characterSet: CharacterSet,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: LearnViewModel = viewModel(factory = LearnViewModel.factory(characterSet))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    // Coming back from the voice installation screen: check the voice again.
    LifecycleResumeEffect(viewModel) {
        viewModel.retryVoice()
        onPauseOrDispose {}
    }
    LearnContent(
        title = stringResource(R.string.learn_title, stringResource(characterSet.labelRes)),
        uiState = uiState,
        isSpeaking = isSpeaking,
        onPrevious = viewModel::previous,
        onNext = viewModel::next,
        onSpeak = viewModel::speak,
        onRetryVoice = viewModel::retryVoice,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
private fun LearnContent(
    title: String,
    uiState: LearnUiState,
    isSpeaking: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSpeak: () -> Unit,
    onRetryVoice: () -> Unit,
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
        when (uiState) {
            LearnUiState.Loading -> CenteredColumn(contentModifier) {
                CircularProgressIndicator()
                Text(stringResource(R.string.quiz_loading), style = MaterialTheme.typography.bodyLarge)
            }
            LearnUiState.LoadError -> CenteredColumn(contentModifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.quiz_load_error),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
                SecondaryButton(stringResource(R.string.quiz_action_home), onBack)
            }
            LearnUiState.MissingVoice -> MissingVoiceContent(onRetryVoice, contentModifier)
            LearnUiState.SpeechUnavailable -> SpeechUnavailableContent(onRetryVoice, contentModifier)
            is LearnUiState.Showing -> ShowingContent(uiState, isSpeaking, onPrevious, onNext, onSpeak, contentModifier)
        }
    }
}

@Composable
private fun CenteredColumn(modifier: Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { content() }
}

@Composable
private fun ShowingContent(
    state: LearnUiState.Showing,
    isSpeaking: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSpeak: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val distance = with(LocalDensity.current) { 24.dp.roundToPx() }
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.learn_progress, state.index + 1, state.characters.size),
            style = MaterialTheme.typography.titleMedium,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .swipeToChange(onPrevious = onPrevious, onNext = onNext),
            shape = MaterialTheme.shapes.extraLarge,
            color = colors.surfaceContainer,
            border = BorderStroke(1.dp, colors.outlineVariant),
        ) {
            AnimatedContent(
                targetState = state.index,
                transitionSpec = {
                    // Same 280 ms slide as the quiz questions; the direction follows the navigation.
                    val sign = if (targetState > initialState) 1 else -1
                    val spec = tween<Float>(280)
                    (slideInHorizontally(tween(280)) { sign * distance } + fadeIn(spec))
                        .togetherWith(slideOutHorizontally(tween(280)) { -sign * distance } + fadeOut(spec))
                },
                label = "learn-card",
            ) { index ->
                CharacterCard(state.characters[index])
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalIconButton(onClick = onPrevious, enabled = state.hasPrevious, modifier = Modifier.size(56.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_left),
                    contentDescription = stringResource(R.string.learn_action_previous),
                    modifier = Modifier.size(28.dp),
                )
            }
            FilledIconButton(
                onClick = onSpeak,
                modifier = Modifier.size(72.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = colors.primary),
            ) {
                Icon(
                    painter = painterResource(if (isSpeaking) R.drawable.ic_volume_up_filled else R.drawable.ic_volume_up),
                    contentDescription = stringResource(R.string.learn_action_listen),
                    modifier = Modifier.size(36.dp),
                )
            }
            FilledTonalIconButton(onClick = onNext, enabled = state.hasNext, modifier = Modifier.size(56.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = stringResource(R.string.learn_action_next),
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}

/** The character, then its romaji (kana) or its meaning and readings (kanji). */
@Composable
private fun CharacterCard(character: JapaneseCharacter, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        JapaneseText(character.symbol, style = KanaStyles.Hero, textAlign = TextAlign.Center)
        if (character.meaning == null) {
            Text(
                text = character.readings.first(),
                style = MaterialTheme.typography.displayMedium,
                textAlign = TextAlign.Center,
            )
        } else {
            Text(
                text = stringResource(R.string.quiz_meaning, character.meaning),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.learn_readings, character.readings.joinToString(" / ")),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Swipe left for the next card, right for the previous one (the buttons stay the main way to navigate). */
@Composable
private fun Modifier.swipeToChange(onPrevious: () -> Unit, onNext: () -> Unit): Modifier {
    val previous by rememberUpdatedState(onPrevious)
    val next by rememberUpdatedState(onNext)
    val threshold = with(LocalDensity.current) { 64.dp.toPx() }
    return pointerInput(threshold) {
        var dragged = 0f
        detectHorizontalDragGestures(
            onDragStart = { dragged = 0f },
            onDragEnd = {
                if (dragged > threshold) previous() else if (dragged < -threshold) next()
            },
            onHorizontalDrag = { _, amount -> dragged += amount },
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, heightDp = 780)
@Composable
private fun LearnPreview() {
    UnolingoTheme {
        LearnContent(
            title = "Apprendre · Hiragana",
            uiState = LearnUiState.Showing(listOf(JapaneseCharacter("あ", listOf("a"))), 0),
            isSpeaking = false,
            onPrevious = {},
            onNext = {},
            onSpeak = {},
            onRetryVoice = {},
            onBack = {},
        )
    }
}
