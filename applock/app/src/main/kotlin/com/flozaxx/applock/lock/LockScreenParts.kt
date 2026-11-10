package com.flozaxx.applock.lock

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flozaxx.applock.R

/** Horizontal offset in the range -1..1 that plays a short shake whenever [wrongCount] grows. */
@Composable
fun rememberShake(wrongCount: Int): Animatable<Float, *> {
    val shake = remember { Animatable(0f) }
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(wrongCount) {
        if (wrongCount == 0) return@LaunchedEffect
        haptics.performHapticFeedback(HapticFeedbackType.Reject)
        shake.snapTo(0f)
        shake.animateTo(
            0f,
            keyframes {
                durationMillis = 420
                -1f at 50
                1f at 120
                -0.7f at 190
                0.7f at 260
                -0.3f at 330
            },
        )
    }
    return shake
}

fun Modifier.shake(shake: Animatable<Float, *>, distance: Dp = 14.dp): Modifier =
    graphicsLayer { translationX = shake.value * distance.toPx() }

/** Fades and slightly enlarges the content once the PIN is accepted, right before the screen closes. */
@Composable
fun Modifier.unlockFade(status: LockStatus): Modifier {
    val progress by animateFloatAsState(
        targetValue = if (status == LockStatus.UNLOCKED) 1f else 0f,
        animationSpec = tween(LockController.UNLOCK_ANIMATION_MILLIS.toInt()),
        label = "unlock",
    )
    return graphicsLayer {
        alpha = 1f - progress
        scaleX = 1f + 0.04f * progress
        scaleY = 1f + 0.04f * progress
    }
}

@Composable
fun lockMessage(state: LockUiState): String = when (state.status) {
    LockStatus.WRONG -> stringResource(R.string.lock_wrong_pin)
    LockStatus.LOCKED_OUT -> stringResource(R.string.lock_locked_out, state.lockoutSecondsLeft)
    else -> stringResource(R.string.lock_enter_pin)
}

val LockUiState.isError: Boolean
    get() = status == LockStatus.WRONG || status == LockStatus.LOCKED_OUT

@Composable
fun TargetIcon(target: LockTarget, size: Dp, fallbackTint: Color, modifier: Modifier = Modifier) {
    if (target.icon != null) {
        Image(target.icon, contentDescription = null, modifier = modifier.size(size).clip(RoundedCornerShape(28)))
    } else {
        Icon(painterResource(R.drawable.ic_lock), contentDescription = null, tint = fallbackTint, modifier = modifier.size(size))
    }
}

@Composable
fun PinDots(
    total: Int,
    filled: Int,
    color: Color,
    modifier: Modifier = Modifier,
    dotSize: Dp = 14.dp,
    gap: Dp = 18.dp,
    emptyColor: Color = Color.Transparent,
    outlineColor: Color = color,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(gap)) {
        repeat(total) { index ->
            val isFilled = index < filled
            Box(
                Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(if (isFilled) color else emptyColor)
                    .border(1.5.dp, if (isFilled) color else outlineColor, CircleShape),
            )
        }
    }
}

/**
 * The 3×4 key grid every PIN style shares. [key] draws one digit key; the two corner slots next
 * to 0 are free for the style (biometric, backspace, cancel).
 */
@Composable
fun PinPad(
    keySize: Dp,
    columnGap: Dp,
    rowGap: Dp,
    modifier: Modifier = Modifier,
    bottomStart: @Composable () -> Unit = {},
    bottomEnd: @Composable () -> Unit = {},
    key: @Composable (digit: Int) -> Unit,
) {
    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(rowGap),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        for (row in listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9))) {
            Row(horizontalArrangement = Arrangement.spacedBy(columnGap)) {
                row.forEach { digit -> key(digit) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(columnGap), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(keySize), contentAlignment = Alignment.Center) { bottomStart() }
            key(0)
            Box(Modifier.size(keySize), contentAlignment = Alignment.Center) { bottomEnd() }
        }
    }
}

/** A text-only action for the bottom edge. Keeps its slot when [onClick] is null so the row stays balanced. */
@Composable
fun LockTextAction(label: String, color: Color, onClick: (() -> Unit)?, fontSize: TextUnit = 15.sp) {
    if (onClick == null) {
        Box(Modifier.size(1.dp))
        return
    }
    Box(
        Modifier.clip(CircleShape).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = color, fontSize = fontSize)
    }
}
