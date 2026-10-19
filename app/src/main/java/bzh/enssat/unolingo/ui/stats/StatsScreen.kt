package bzh.enssat.unolingo.ui.stats

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import bzh.enssat.unolingo.R
import bzh.enssat.unolingo.ui.common.PlaceholderScreen

@Composable
fun StatsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = stringResource(R.string.stats_title),
        onBack = onBack,
        modifier = modifier,
    )
}
