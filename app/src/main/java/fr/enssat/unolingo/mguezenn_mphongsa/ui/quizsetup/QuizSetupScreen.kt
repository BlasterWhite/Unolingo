package fr.enssat.unolingo.mguezenn_mphongsa.ui.quizsetup

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.enssat.unolingo.mguezenn_mphongsa.R
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.CharacterSet
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.QuizConfig
import fr.enssat.unolingo.mguezenn_mphongsa.speech.SpeakerStatus
import fr.enssat.unolingo.mguezenn_mphongsa.ui.common.BackTopAppBar
import fr.enssat.unolingo.mguezenn_mphongsa.ui.common.CharacterSetSelector
import fr.enssat.unolingo.mguezenn_mphongsa.ui.common.ChoiceButton
import fr.enssat.unolingo.mguezenn_mphongsa.ui.common.ChoiceState
import fr.enssat.unolingo.mguezenn_mphongsa.ui.common.PrimaryButton
import fr.enssat.unolingo.mguezenn_mphongsa.ui.common.SecondaryButton
import fr.enssat.unolingo.mguezenn_mphongsa.ui.common.labelRes
import fr.enssat.unolingo.mguezenn_mphongsa.ui.theme.UnolingoTheme

/** Quiz settings (TP2): builds a [QuizConfig] from [settings], which the caller keeps so the next visit starts from them. */
@Composable
fun QuizSetupScreen(
    settings: QuizSetupSettings,
    onSettingsChange: (QuizSetupSettings) -> Unit,
    onStart: (QuizConfig) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: QuizSetupViewModel = viewModel(factory = QuizSetupViewModel.Factory)
    val kanjiCounts by viewModel.kanjiCounts.collectAsStateWithLifecycle()
    val voiceStatus by viewModel.voiceStatus.collectAsStateWithLifecycle()
    // Re-checked on every resume: the user may come back from the voice installation.
    LifecycleResumeEffect(settings.tts) {
        if (settings.tts) viewModel.checkVoice() else viewModel.stopVoiceCheck()
        onPauseOrDispose {}
    }
    val context = LocalContext.current
    QuizSetupContent(
        settings = settings,
        onSettingsChange = onSettingsChange,
        kanjiCounts = kanjiCounts,
        voiceStatus = voiceStatus,
        onInstallVoice = { openVoiceInstallation(context) },
        onStart = { onStart(settings.toConfig()) },
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
internal fun QuizSetupContent(
    settings: QuizSetupSettings,
    onSettingsChange: (QuizSetupSettings) -> Unit,
    kanjiCounts: Map<Int, Int>,
    voiceStatus: SpeakerStatus?,
    onInstallVoice: () -> Unit,
    onStart: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { BackTopAppBar(stringResource(R.string.setup_title), onBack) },
        // Always reachable, even when the kanji options make the page taller than the screen.
        bottomBar = {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = pluralStringResource(
                        R.plurals.home_question_count,
                        QuizConfig.QUESTION_COUNT,
                        QuizConfig.QUESTION_COUNT,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PrimaryButton(stringResource(R.string.action_start), onClick = onStart)
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionTitle(R.string.setup_section_set)
            CharacterSetSelector(settings.characterSet, { onSettingsChange(settings.withSet(it)) })

            SectionTitle(R.string.setup_section_kind)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                settings.availableKinds.forEach { kind ->
                    ChoiceButton(
                        state = if (kind == settings.questionKind) ChoiceState.SELECTED else ChoiceState.NEUTRAL,
                        enabled = true,
                        onClick = { onSettingsChange(settings.copy(questionKind = kind)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),
                    ) {
                        Text(stringResource(kind.labelRes), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            if (settings.characterSet == CharacterSet.KANJI) {
                SectionTitle(R.string.setup_section_grade)
                GradePicker(settings.kanjiGrade, kanjiCounts) { onSettingsChange(settings.copy(kanjiGrade = it)) }
            }

            SectionTitle(R.string.setup_section_options)
            SwitchRow(
                title = stringResource(R.string.setup_help_title),
                description = stringResource(R.string.setup_help_description),
                checked = settings.help,
                onCheckedChange = { onSettingsChange(settings.copy(help = it)) },
            )
            SwitchRow(
                title = stringResource(R.string.setup_tts_title),
                description = stringResource(R.string.setup_tts_description),
                checked = settings.tts,
                onCheckedChange = { onSettingsChange(settings.copy(tts = it)) },
            )
            if (settings.tts) VoiceNotice(voiceStatus, onInstallVoice)
        }
    }
}

@Composable
private fun SectionTitle(titleRes: Int) {
    Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium)
}

/** Grades 1 to 6 and 8, then "kanji up to grade g" with how many kanji that makes. */
@Composable
private fun GradePicker(selected: Int, counts: Map<Int, Int>, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QuizConfig.GRADES.forEach { grade ->
                FilterChip(
                    selected = grade == selected,
                    onClick = { onSelect(grade) },
                    label = { Text(grade.toString()) },
                )
            }
        }
        val count = counts[selected]
        Text(
            text = if (count == null) {
                stringResource(R.string.setup_grade_summary, selected)
            } else {
                stringResource(R.string.setup_grade_summary_count, selected, pluralStringResource(R.plurals.setup_kanji_count, count, count))
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        // The row handles the click; the switch itself is only the visual.
        Switch(checked = checked, onCheckedChange = null)
    }
}

/** Reminder under the text-to-speech switch when the phone cannot pronounce Japanese; the quiz also handles it. */
@Composable
private fun VoiceNotice(status: SpeakerStatus?, onInstallVoice: () -> Unit) {
    val message = when (status) {
        SpeakerStatus.MISSING_VOICE -> R.string.setup_voice_missing
        SpeakerStatus.UNAVAILABLE -> R.string.setup_voice_unavailable
        SpeakerStatus.READY, SpeakerStatus.INITIALIZING, null -> return
    }
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.errorContainer) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            if (status == SpeakerStatus.MISSING_VOICE) {
                SecondaryButton(stringResource(R.string.quiz_action_install_voice), onClick = onInstallVoice)
            }
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

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun QuizSetupKanjiPreview() {
    UnolingoTheme {
        QuizSetupContent(
            settings = QuizSetupSettings(CharacterSet.KANJI, tts = true),
            onSettingsChange = {},
            kanjiCounts = mapOf(1 to 80, 2 to 240),
            voiceStatus = SpeakerStatus.MISSING_VOICE,
            onInstallVoice = {},
            onStart = {},
            onBack = {},
        )
    }
}
