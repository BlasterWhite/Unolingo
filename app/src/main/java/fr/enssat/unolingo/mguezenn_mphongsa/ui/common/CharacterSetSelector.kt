package fr.enssat.unolingo.mguezenn_mphongsa.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import fr.enssat.unolingo.mguezenn_mphongsa.domain.model.CharacterSet

@Composable
fun CharacterSetSelector(
    selected: CharacterSet,
    onSelect: (CharacterSet) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        CharacterSet.entries.forEachIndexed { index, set ->
            SegmentedButton(
                selected = set == selected,
                onClick = { onSelect(set) },
                shape = SegmentedButtonDefaults.itemShape(index, CharacterSet.entries.size),
            ) { Text(stringResource(set.labelRes)) }
        }
    }
}
