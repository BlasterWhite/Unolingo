package bzh.enssat.unolingo.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bzh.enssat.unolingo.domain.battle.Player
import bzh.enssat.unolingo.ui.theme.KanaStyles
import kotlinx.coroutines.launch

/**
 * Hanko seal of a player: a round seal marked 一 for player 1, a square one marked 二 for player 2 (the shapes tell
 * the players apart without relying on color). Each time [stampKey] changes after the first composition, the
 * seal "stamps" (scale 1.3 → 1, rotation −8° → 0, 260 ms): pass the player's score.
 */
@Composable
fun PlayerSeal(
    player: Player,
    ink: Color,
    modifier: Modifier = Modifier,
    fill: Color = Color.Transparent,
    /** Ring around the seal; defaults to [ink]. Pass [fill] for a plain filled seal. */
    border: Color = ink,
    size: Dp = 40.dp,
    stampKey: Int? = null,
) {
    val scale = remember { Animatable(1f) }
    val rotation = remember { Animatable(0f) }
    val initialKey = remember { stampKey }
    LaunchedEffect(stampKey) {
        if (stampKey == null || stampKey == initialKey) return@LaunchedEffect
        scale.snapTo(1.3f)
        rotation.snapTo(-8f)
        launch { scale.animateTo(1f, tween(260)) }
        rotation.animateTo(0f, tween(260))
    }
    val shape = if (player == Player.ONE) CircleShape else RoundedCornerShape(size * 0.22f)
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                rotationZ = rotation.value
            }
            .background(fill, shape)
            .border(size * 0.07f, border, shape),
        contentAlignment = Alignment.Center,
    ) {
        JapaneseText(
            text = if (player == Player.ONE) "一" else "二",
            style = KanaStyles.Compact.copy(fontSize = (size.value * 0.5f).sp, lineHeight = (size.value * 0.6f).sp),
            color = ink,
        )
    }
}
