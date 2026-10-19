package bzh.enssat.unolingo.ui.quiz

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import bzh.enssat.unolingo.R
import bzh.enssat.unolingo.domain.quiz.Question
import bzh.enssat.unolingo.domain.quiz.QuestionResult
import bzh.enssat.unolingo.ui.common.ChoiceGrid
import bzh.enssat.unolingo.ui.common.ChoiceState
import bzh.enssat.unolingo.ui.common.JapaneseText
import bzh.enssat.unolingo.ui.common.PrimaryButton
import bzh.enssat.unolingo.ui.common.QuestionPrompt
import bzh.enssat.unolingo.ui.common.SecondaryButton
import bzh.enssat.unolingo.ui.common.choiceOptions
import bzh.enssat.unolingo.ui.common.choiceState
import bzh.enssat.unolingo.ui.theme.KanaStyles

/** "Sound → character": the reading is pronounced when the question appears; the speaker button replays it. */
@Composable
internal fun SoundToCharacterContent(
    question: Question.SoundToCharacter,
    feedback: QuestionResult?,
    isSpeaking: Boolean,
    onSubmit: (String) -> Unit,
    onReplay: () -> Unit,
) {
    PromptPanel { SpeakerButton(isSpeaking = isSpeaking, onClick = onReplay) }
    ChoiceGrid(
        options = question.choiceOptions,
        stateOf = { choiceState(question, it, chosen = feedback?.answer, revealed = feedback != null) },
        enabled = feedback == null,
        onChoose = onSubmit,
    )
}

/** Big round speaker. While a reading is pronounced, its icon is filled and two rings pulse around it (900 ms). */
@Composable
private fun SpeakerButton(isSpeaking: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.size(168.dp), contentAlignment = Alignment.Center) {
            if (isSpeaking) PulseRings(colors.primary)
            FilledIconButton(
                onClick = onClick,
                modifier = Modifier.size(96.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = colors.primary),
            ) {
                Icon(
                    painter = painterResource(if (isSpeaking) R.drawable.ic_volume_up_filled else R.drawable.ic_volume_up),
                    contentDescription = stringResource(R.string.quiz_action_replay),
                    modifier = Modifier.size(44.dp),
                )
            }
        }
        Text(
            text = stringResource(if (isSpeaking) R.string.quiz_speaking else R.string.quiz_action_replay),
            style = MaterialTheme.typography.titleSmall,
            color = colors.primary,
        )
    }
}

@Composable
private fun PulseRings(color: Color) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    Canvas(Modifier.fillMaxSize()) {
        val inner = 48.dp.toPx()
        val outer = size.minDimension / 2
        listOf(phase, (phase + 0.5f) % 1f).forEach { ringPhase ->
            drawCircle(
                color = color.copy(alpha = 0.45f * (1 - ringPhase)),
                radius = inner + (outer - inner) * ringPhase,
                style = Stroke(width = 3.dp.toPx()),
            )
        }
    }
}

/**
 * "Character → sound": tapping a proposal plays it and selects it, "Valider" submits the selection. Once answered,
 * the proposals can still be played and show their reading.
 */
@Composable
internal fun CharacterToSoundContent(
    question: Question.CharacterToSound,
    feedback: QuestionResult?,
    onSubmit: (String) -> Unit,
    onPlayReading: (String) -> Unit,
) {
    var selected by rememberSaveable(question.character.symbol) { mutableStateOf<String?>(null) }

    PromptPanel { QuestionPrompt(question, characterStyle = KanaStyles.Hero) }
    ChoiceGrid(
        options = question.choiceOptions,
        stateOf = { option ->
            when {
                feedback != null -> choiceState(question, option, chosen = feedback.answer, revealed = true)
                option.value == selected -> ChoiceState.SELECTED
                else -> ChoiceState.NEUTRAL
            }
        },
        enabled = true,
        onChoose = { reading ->
            onPlayReading(reading)
            if (feedback == null) selected = reading
        },
        buttonHeight = 72.dp,
    ) { index, option ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_volume_up),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (feedback == null) {
                    stringResource(R.string.quiz_sound_option, index + 1)
                } else {
                    stringResource(R.string.quiz_sound_option_revealed, index + 1, option.value)
                },
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
    if (feedback == null) {
        PrimaryButton(
            text = stringResource(R.string.quiz_action_validate),
            onClick = { selected?.let(onSubmit) },
            enabled = selected != null,
        )
    }
}

@Composable
internal fun MissingVoiceContent(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    AudioProblemContent(
        title = stringResource(R.string.quiz_missing_voice_title),
        message = stringResource(R.string.quiz_missing_voice_message),
        onRetry = onRetry,
        modifier = modifier,
        showKanaBadge = true,
    ) {
        PrimaryButton(
            text = stringResource(R.string.quiz_action_install_voice),
            onClick = { openVoiceInstallation(context) },
            icon = R.drawable.ic_download,
        )
    }
}

@Composable
internal fun SpeechUnavailableContent(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    AudioProblemContent(
        title = stringResource(R.string.quiz_tts_unavailable_title),
        message = stringResource(R.string.quiz_tts_unavailable_message),
        onRetry = onRetry,
        modifier = modifier,
    )
}

/** Muted-speaker illustration, explanation, then the actions pinned at the bottom. */
@Composable
private fun AudioProblemContent(
    title: String,
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    showKanaBadge: Boolean = false,
    @DrawableRes illustration: Int = R.drawable.ic_volume_off,
    primaryAction: @Composable () -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = modifier.padding(16.dp)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(modifier = Modifier.size(136.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(colors.surfaceContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(illustration),
                        contentDescription = null,
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(56.dp),
                    )
                }
                if (showKanaBadge) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 8.dp)
                            .size(48.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(colors.secondaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        JapaneseText("あ", style = KanaStyles.Compact, color = colors.onSecondaryContainer)
                    }
                }
            }
            Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        primaryAction()
        Spacer(Modifier.height(12.dp))
        SecondaryButton(stringResource(R.string.quiz_action_retry), onClick = onRetry)
    }
}

/** Opens the engine's voice download screen, or the system TTS settings if the engine has none. */
private fun openVoiceInstallation(context: Context) {
    val intents = listOf(
        Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA),
        Intent("com.android.settings.TTS_SETTINGS"),
    )
    for (intent in intents) {
        try {
            context.startActivity(intent)
            return
        } catch (_: ActivityNotFoundException) {
            // Try the next one.
        }
    }
}
