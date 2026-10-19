package bzh.enssat.unolingo.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

// Pill buttons of the art direction: 56 dp high, titleMedium label, optional leading icon. Full width unless
// [fullWidth] is false (side-by-side buttons).

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fullWidth: Boolean = true,
    @DrawableRes icon: Int? = null,
) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier.buttonSize(fullWidth)) {
        ButtonLabel(text, icon)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fullWidth: Boolean = true,
    @DrawableRes icon: Int? = null,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.buttonSize(fullWidth),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        ButtonLabel(text, icon)
    }
}

private fun Modifier.buttonSize(fullWidth: Boolean): Modifier =
    (if (fullWidth) fillMaxWidth() else this).height(56.dp)

@Composable
private fun ButtonLabel(text: String, @DrawableRes icon: Int?) {
    if (icon != null) {
        Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
    }
    Text(text, style = MaterialTheme.typography.titleMedium)
}
