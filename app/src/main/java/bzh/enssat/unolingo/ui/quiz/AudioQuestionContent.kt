package bzh.enssat.unolingo.ui.quiz

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bzh.enssat.unolingo.R
import bzh.enssat.unolingo.domain.quiz.Question
import bzh.enssat.unolingo.domain.quiz.QuestionResult
import bzh.enssat.unolingo.ui.common.ChoiceGrid
import bzh.enssat.unolingo.ui.common.ChoiceState
import bzh.enssat.unolingo.ui.common.QuestionPrompt
import bzh.enssat.unolingo.ui.common.choiceOptions
import bzh.enssat.unolingo.ui.common.choiceState
import bzh.enssat.unolingo.ui.common.instructionRes

/** "Sound → character": the reading is pronounced when the question appears; the speaker button replays it. */
@Composable
internal fun SoundToCharacterContent(
    question: Question.SoundToCharacter,
    feedback: QuestionResult?,
    onSubmit: (String) -> Unit,
    onReplay: () -> Unit,
) {
    Text(
        text = stringResource(question.instructionRes),
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
    )
    FilledTonalIconButton(onClick = onReplay, modifier = Modifier.size(96.dp)) {
        Icon(
            painter = painterResource(R.drawable.ic_volume_up),
            contentDescription = stringResource(R.string.quiz_action_replay),
            modifier = Modifier.size(48.dp),
        )
    }
    ChoiceGrid(
        options = question.choiceOptions,
        stateOf = { choiceState(question, it, chosen = feedback?.answer, revealed = feedback != null) },
        enabled = feedback == null,
        onChoose = onSubmit,
    )
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

    Text(
        text = stringResource(question.instructionRes),
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
    )
    QuestionPrompt(question, characterFontSize = 96.sp)
    MeaningText(question.character)
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
        Icon(
            painter = painterResource(R.drawable.ic_volume_up),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
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
    if (feedback == null) {
        Button(
            onClick = { selected?.let(onSubmit) },
            enabled = selected != null,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.quiz_action_validate)) }
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
    ) {
        Button(onClick = { openVoiceInstallation(context) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.quiz_action_install_voice))
        }
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

@Composable
private fun AudioProblemContent(
    title: String,
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    primaryAction: @Composable () -> Unit = {},
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        primaryAction()
        OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.quiz_action_retry))
        }
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
