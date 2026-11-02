package fr.enssat.unolingo.mguezenn_mphongsa.ui.stats

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import fr.enssat.unolingo.mguezenn_mphongsa.R
import fr.enssat.unolingo.mguezenn_mphongsa.ui.common.PlaceholderScreen

@Composable
fun StatsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = stringResource(R.string.stats_title),
        onBack = onBack,
        modifier = modifier,
    )
}
