package com.flozaxx.applock.lock.styles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Box
import com.flozaxx.applock.R
import com.flozaxx.applock.lock.LockScreenModel
import com.flozaxx.applock.lock.LockScreenStyle
import com.flozaxx.applock.lock.PinDots
import com.flozaxx.applock.lock.PinPad
import com.flozaxx.applock.lock.TargetIcon
import com.flozaxx.applock.lock.isError
import com.flozaxx.applock.lock.lockMessage
import com.flozaxx.applock.lock.rememberShake
import com.flozaxx.applock.lock.shake
import com.flozaxx.applock.lock.unlockFade

/** Material You look: filled round keys in the device's dynamic colors. */
object PixelStyle : LockScreenStyle {
    override val id = "pixel"
    override val nameRes = R.string.style_pixel

    private val keySize = 80.dp

    @Composable
    override fun Content(model: LockScreenModel, modifier: Modifier) {
        val state = model.state
        val colors = MaterialTheme.colorScheme
        val shake = rememberShake(state.wrongCount)
        Column(
            modifier
                .fillMaxSize()
                .background(colors.surface)
                .unlockFade(state.status)
                .systemBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            TargetIcon(model.target, 44.dp, fallbackTint = colors.primary)
            Spacer(Modifier.height(16.dp))
            Text(model.target.label, color = colors.onSurface, fontSize = 22.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                lockMessage(state),
                color = if (state.isError) colors.error else colors.onSurfaceVariant,
                fontSize = 15.sp,
            )
            Spacer(Modifier.height(28.dp))
            PinDots(
                total = state.pinLength,
                filled = state.enteredCount,
                color = colors.primary,
                emptyColor = colors.surfaceContainerHighest,
                outlineColor = colors.surfaceContainerHighest,
                dotSize = 16.dp,
                gap = 16.dp,
                modifier = Modifier.shake(shake),
            )
            Spacer(Modifier.weight(1f))
            PinPad(
                keySize = keySize,
                columnGap = 20.dp,
                rowGap = 14.dp,
                bottomStart = {
                    model.onBiometric?.let { onBiometric ->
                        RoundKey(colors.tertiaryContainer, onBiometric) {
                            Icon(
                                painterResource(R.drawable.ic_fingerprint),
                                stringResource(R.string.lock_use_biometric),
                                tint = colors.onTertiaryContainer,
                                modifier = Modifier.size(30.dp),
                            )
                        }
                    }
                },
                bottomEnd = {
                    RoundKey(colors.secondaryContainer, model.onBackspace) {
                        Icon(
                            painterResource(R.drawable.ic_backspace),
                            stringResource(R.string.lock_delete),
                            tint = colors.onSecondaryContainer,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                },
            ) { digit ->
                val haptics = LocalHapticFeedback.current
                RoundKey(
                    colors.surfaceContainerHigh,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                        model.onDigit(digit)
                    },
                ) {
                    Text(digit.toString(), color = colors.onSurface, fontSize = 28.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            Box(Modifier.height(48.dp), contentAlignment = Alignment.Center) {
                model.onForgotPin?.let { onForgotPin ->
                    TextButton(onClick = onForgotPin) { Text(stringResource(R.string.lock_forgot_pin)) }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    @Composable
    private fun RoundKey(color: Color, onClick: () -> Unit, content: @Composable () -> Unit) {
        Surface(onClick = onClick, shape = CircleShape, color = color, modifier = Modifier.size(keySize)) {
            Box(contentAlignment = Alignment.Center) { content() }
        }
    }
}
