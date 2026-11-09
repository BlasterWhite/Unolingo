package fr.enssat.unolingo.mguezenn_mphongsa.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.enssat.unolingo.mguezenn_mphongsa.R
import fr.enssat.unolingo.mguezenn_mphongsa.domain.progress.Rank
import fr.enssat.unolingo.mguezenn_mphongsa.domain.progress.RankType
import fr.enssat.unolingo.mguezenn_mphongsa.ui.common.JapaneseText
import fr.enssat.unolingo.mguezenn_mphongsa.ui.theme.KanaStyles

/** "4 kyū" / "2 dan", localized. */
@Composable
fun Rank.label(): String =
    stringResource(if (type == RankType.KYU) R.string.rank_kyu else R.string.rank_dan, number)

/**
 * Seal of a rank, in the spirit of `PlayerSeal`: a round primary seal for a kyū rank, a square gold one for a dan
 * rank (the shape tells them apart without relying on color), marked with the number and 級 (kyū) or 段 (dan).
 * Decorative: the rank is always written next to it.
 */
@Composable
fun RankSeal(rank: Rank, modifier: Modifier = Modifier, size: Dp = 56.dp) {
    val colors = MaterialTheme.colorScheme
    val kyu = rank.type == RankType.KYU
    val shape = if (kyu) CircleShape else RoundedCornerShape(size * 0.22f)
    val ink = if (kyu) colors.onPrimary else colors.tertiary
    Column(
        modifier = modifier
            .size(size)
            .background(if (kyu) colors.primary else colors.tertiaryContainer, shape)
            .border(size * 0.06f, if (kyu) colors.primary else colors.tertiary, shape)
            .clearAndSetSemantics {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Text(
            text = rank.number.toString(),
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = (size.value * 0.38f).sp,
                lineHeight = (size.value * 0.42f).sp,
                fontWeight = FontWeight.Bold,
            ),
            color = ink,
        )
        JapaneseText(
            text = if (kyu) "級" else "段",
            style = KanaStyles.Compact.copy(fontSize = (size.value * 0.22f).sp, lineHeight = (size.value * 0.26f).sp),
            color = ink,
        )
    }
}
