package bzh.enssat.unolingo.ui.common

import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import bzh.enssat.unolingo.R
import bzh.enssat.unolingo.domain.quiz.AnswerChecker
import bzh.enssat.unolingo.domain.quiz.Question
import bzh.enssat.unolingo.ui.theme.KanaStyles

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

/**
 * The prompt of a question: the reading to find, the character to read, or a speaker for a sound. No pattern or
 * decoration may be drawn behind it.
 */
@Composable
fun QuestionPrompt(
    question: Question,
    characterStyle: TextStyle,
    modifier: Modifier = Modifier,
    romajiStyle: TextStyle = MaterialTheme.typography.displayLarge,
) {
    when (question) {
        is Question.RomajiToCharacter -> Text(text = question.prompt, style = romajiStyle, modifier = modifier)
        is Question.CharacterToRomaji, is Question.CharacterToRomajiChoice, is Question.CharacterToSound ->
            JapaneseText(text = question.character.symbol, style = characterStyle, modifier = modifier)
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
    japaneseStyle: TextStyle = KanaStyles.Choice,
    spacing: Dp = 12.dp,
    /** Content of the button at 0-based [index]; shows the option's value by default. */
    optionContent: @Composable BoxScope.(index: Int, option: ChoiceOption) -> Unit = { _, option ->
        ChoiceLabel(option, japaneseStyle)
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
private fun ChoiceLabel(option: ChoiceOption, japaneseStyle: TextStyle) {
    if (option.isJapanese) {
        JapaneseText(text = option.value, style = japaneseStyle)
    } else {
        Text(text = option.value, style = MaterialTheme.typography.headlineSmall)
    }
}

private data class ChoiceColors(
    val container: Color,
    val content: Color,
    val border: Color,
    val borderWidth: Dp,
    /** Null for a flat button (dimmed choices). */
    val lip: Color?,
)

@Composable
private fun choiceColors(state: ChoiceState): ChoiceColors {
    val colors = MaterialTheme.colorScheme
    return when (state) {
        ChoiceState.NEUTRAL -> ChoiceColors(colors.surface, colors.onSurface, colors.outline, 1.5.dp, colors.outline)
        ChoiceState.SELECTED -> ChoiceColors(
            colors.secondaryContainer, colors.onSecondaryContainer, colors.secondary, 2.dp, colors.secondary,
        )
        ChoiceState.CORRECT -> ChoiceColors(
            colors.primaryContainer, colors.onPrimaryContainer, colors.primary, 2.dp, colors.primary,
        )
        ChoiceState.WRONG -> ChoiceColors(colors.errorContainer, colors.onErrorContainer, colors.error, 2.dp, colors.error)
        ChoiceState.DIMMED -> ChoiceColors(
            colors.surfaceContainer, colors.onSurface.copy(alpha = 0.38f), colors.outlineVariant, 1.dp, null,
        )
    }
}

private val LipHeight = 4.dp

/** Test tag of every [ChoiceButton], for UI and screenshot tests. */
const val ChoiceTestTag = "choice"

/**
 * A choice button of the art direction: a raised face on a 4 dp "lip" that flattens when pressed, a ◯ or ✕ mark
 * once corrected, and a short horizontal shake (240 ms) when it turns [ChoiceState.WRONG]. Its size comes from
 * [modifier].
 */
@Composable
fun ChoiceButton(
    state: ChoiceState,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = choiceColors(state)
    val shape = MaterialTheme.shapes.large
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val lip = if (colors.lip != null) LipHeight else 0.dp
    val pressOffset by animateDpAsState(if (pressed && enabled) lip else 0.dp, tween(80), label = "press")

    val shake = remember { Animatable(0f) }
    LaunchedEffect(state) {
        if (state == ChoiceState.WRONG) {
            shake.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 240
                    6f at 40
                    -6f at 90
                    4f at 140
                    -3f at 190
                },
            )
        }
    }

    Box(modifier.offset { IntOffset(shake.value.dp.roundToPx(), 0) }) {
        if (colors.lip != null) {
            Box(
                Modifier
                    .matchParentSize()
                    .padding(top = lip)
                    .background(colors.lip, shape),
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(top = pressOffset, bottom = lip - pressOffset)
                .testTag(ChoiceTestTag)
                .clip(shape)
                .background(colors.container)
                .border(colors.borderWidth, colors.border, shape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(),
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                )
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            CompositionLocalProvider(LocalContentColor provides colors.content) { content() }
            if (state == ChoiceState.CORRECT || state == ChoiceState.WRONG) {
                AnswerMark(
                    correct = state == ChoiceState.CORRECT,
                    color = colors.border,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp),
                    size = 18.dp,
                )
            }
        }
    }
}
