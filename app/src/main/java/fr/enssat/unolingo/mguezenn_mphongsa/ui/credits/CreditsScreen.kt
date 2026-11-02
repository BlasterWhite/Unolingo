package fr.enssat.unolingo.mguezenn_mphongsa.ui.credits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.enssat.unolingo.mguezenn_mphongsa.R
import fr.enssat.unolingo.mguezenn_mphongsa.ui.common.BackTopAppBar
import fr.enssat.unolingo.mguezenn_mphongsa.ui.theme.UnolingoTheme

private const val KANJI_DATA_URL = "https://github.com/davidluzgouveia/kanji-data"

@Composable
fun CreditsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = { BackTopAppBar(title = stringResource(R.string.credits_title), onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.credits_intro),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CreditEntry(
                name = stringResource(R.string.credits_kanji_data_name),
                description = stringResource(R.string.credits_kanji_data_description),
                license = stringResource(R.string.credits_kanji_data_license),
                linkUrl = KANJI_DATA_URL,
            )
            CreditEntry(
                name = stringResource(R.string.credits_klee_name),
                description = stringResource(R.string.credits_klee_description),
                license = stringResource(R.string.credits_ofl_license),
            )
            CreditEntry(
                name = stringResource(R.string.credits_lexend_name),
                description = stringResource(R.string.credits_lexend_description),
                license = stringResource(R.string.credits_ofl_license),
            )
            CreditEntry(
                name = stringResource(R.string.credits_symbols_name),
                description = stringResource(R.string.credits_symbols_description),
                license = stringResource(R.string.credits_symbols_license),
            )
        }
    }
}

@Composable
private fun CreditEntry(
    name: String,
    description: String,
    license: String,
    modifier: Modifier = Modifier,
    linkUrl: String? = null,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(name, style = MaterialTheme.typography.titleMedium)
        Text(description, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = stringResource(R.string.credits_license_label, license),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (linkUrl != null) {
            val uriHandler = LocalUriHandler.current
            Text(linkUrl, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = { uriHandler.openUri(linkUrl) }) {
                Text(stringResource(R.string.credits_open_link))
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 860)
@Composable
private fun CreditsPreview() {
    UnolingoTheme { CreditsScreen(onBack = {}) }
}
