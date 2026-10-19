package bzh.enssat.unolingo.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bzh.enssat.unolingo.R
import bzh.enssat.unolingo.domain.quiz.AnswerChecker
import bzh.enssat.unolingo.domain.quiz.Question

/** One option of a multiple-choice question: [value] is what gets submitted as the answer. */
data class ChoiceOption(val value: String, val isJapanese: Boolean)

val Question.choiceOptions: List<ChoiceOption>
    get() = when (this) {
        is Question.RomajiToCharacter -> choices.map { ChoiceOption(it.symbol, isJapanese = true) }
        is Question.SoundToCharacter -> choices.map { ChoiceOption(it.symbol, isJapanese = true) }
        is Question.CharacterToRomajiChoice -> choices.map { ChoiceOption(it, isJapanese = false) }
        is Question.CharacterToSound -> choices.map { ChoiceOption(it, isJapanese = false) }
        is Question.CharacterToRomaji -> emptyList()
    }

/** Instruction shown above the prompt. */
@get:StringRes
val Question.instructionRes: Int
    get() = when (this) {
        is Question.RomajiToCharacter -> R.string.quiz_prompt_character
        is Question.CharacterToRomaji, is Question.CharacterToRomajiChoice -> R.string.quiz_prompt_reading
        is Question.SoundToCharacter -> R.string.quiz_prompt_sound
        is Question.CharacterToSound -> R.string.quiz_prompt_pronunciation
    }

/** [SELECTED] marks a choice picked but not validated yet. */
enum class ChoiceState { NEUTRAL, SELECTED, CORRECT, WRONG, DIMMED }

/**
 * [chosen] is the answer the player gave (null if none yet); [revealed] shows the right answer. The right
 * answer stays hidden while only [chosen] is set, so a player locked out by a mistake cannot help the other.
 */
fun choiceState(question: Question, option: ChoiceOption, chosen: String?, revealed: Boolean): ChoiceState {
    val isRight = AnswerChecker.isCorrect(question, option.value)
    return when {
        isRight && (revealed || option.value == chosen) -> ChoiceState.CORRECT
        option.value == chosen -> ChoiceState.WRONG
        revealed || chosen != null -> ChoiceState.DIMMED
        else -> ChoiceState.NEUTRAL
    }
}

/** The prompt of a question: the reading to find, the character to read, or a speaker for a sound. */
@Composable
fun QuestionPrompt(question: Question, characterFontSize: TextUnit, modifier: Modifier = Modifier) {
    when (question) {
        is Question.RomajiToCharacter ->
            Text(text = question.prompt, style = MaterialTheme.typography.displayMedium, modifier = modifier)
        is Question.CharacterToRomaji, is Question.CharacterToRomajiChoice, is Question.CharacterToSound ->
            JapaneseText(text = question.character.symbol, fontSize = characterFontSize, modifier = modifier)
        is Question.SoundToCharacter -> Icon(
            painter = painterResource(R.drawable.ic_volume_up),
            contentDescription = null,
            modifier = modifier.size(64.dp),
        )
    }
}

/** Choices laid out two per row. */
@Composable
fun ChoiceGrid(
    options: List<ChoiceOption>,
    stateOf: (ChoiceOption) -> ChoiceState,
    enabled: Boolean,
    onChoose: (String) -> Unit,
    modifier: Modifier = Modifier,
    buttonHeight: Dp = 88.dp,
    japaneseFontSize: TextUnit = 40.sp,
    spacing: Dp = 12.dp,
    /** Content of the button at 0-based [index]; shows the option's value by default. */
    optionContent: @Composable RowScope.(index: Int, option: ChoiceOption) -> Unit = { _, option ->
        ChoiceLabel(option, japaneseFontSize)
    },
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing)) {
        options.chunked(2).forEachIndexed { rowIndex, row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                row.forEachIndexed { columnIndex, option ->
                    ChoiceButton(
                        state = stateOf(option),
                        enabled = enabled,
                        onClick = { onChoose(option.value) },
                        modifier = Modifier
                            .weight(1f)
                            .height(buttonHeight),
                    ) { optionContent(rowIndex * 2 + columnIndex, option) }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ChoiceLabel(option: ChoiceOption, japaneseFontSize: TextUnit) {
    if (option.isJapanese) {
        JapaneseText(text = option.value, fontSize = japaneseFontSize)
    } else {
        Text(text = option.value, style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
fun ChoiceButton(
    state: ChoiceState,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val (containerColor, contentColor) = when (state) {
        ChoiceState.NEUTRAL -> colorScheme.surface to colorScheme.onSurface
        ChoiceState.SELECTED -> colorScheme.secondaryContainer to colorScheme.onSecondaryContainer
        ChoiceState.CORRECT -> colorScheme.primaryContainer to colorScheme.onPrimaryContainer
        ChoiceState.WRONG -> colorScheme.errorContainer to colorScheme.onErrorContainer
        ChoiceState.DIMMED -> colorScheme.surface to colorScheme.onSurface.copy(alpha = 0.38f)
    }
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        // Same colors whether enabled or not: the state alone drives the look.
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor,
            disabledContentColor = contentColor,
        ),
        modifier = modifier,
        content = content,
    )
}
