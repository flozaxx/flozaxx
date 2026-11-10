package com.flozaxx.applock.lock.styles

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flozaxx.applock.R
import com.flozaxx.applock.lock.LockScreenModel
import com.flozaxx.applock.lock.LockScreenStyle
import com.flozaxx.applock.lock.LockTextAction
import com.flozaxx.applock.lock.PinDots
import com.flozaxx.applock.lock.PinPad
import com.flozaxx.applock.lock.TargetIcon
import com.flozaxx.applock.lock.isError
import com.flozaxx.applock.lock.lockMessage
import com.flozaxx.applock.lock.rememberShake
import com.flozaxx.applock.lock.shake
import com.flozaxx.applock.lock.unlockFade

/** Dark screen with large borderless-looking digits, dots on top and two text actions at the bottom edge. */
object SamsungStyle : LockScreenStyle {
    override val id = "samsung"
    override val nameRes = R.string.style_samsung

    private val keySize = 78.dp
    private val background = Brush.verticalGradient(listOf(Color(0xFF1A2130), Color(0xFF07090E)))
    private val keyFill = Color.White.copy(alpha = 0.10f)
    private val dimText = Color.White.copy(alpha = 0.72f)
    private val errorText = Color(0xFFFF9A8F)

    @Composable
    override fun Content(model: LockScreenModel, modifier: Modifier) {
        val state = model.state
        val shake = rememberShake(state.wrongCount)
        Column(
            modifier
                .fillMaxSize()
                .background(background)
                .unlockFade(state.status)
                .systemBarsPadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            TargetIcon(model.target, 52.dp, fallbackTint = Color.White)
            Spacer(Modifier.height(14.dp))
            Text(model.target.label, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Text(lockMessage(state), color = if (state.isError) errorText else dimText, fontSize = 15.sp)
            Spacer(Modifier.height(28.dp))
            PinDots(
                total = state.pinLength,
                filled = state.enteredCount,
                color = Color.White,
                outlineColor = Color.White.copy(alpha = 0.55f),
                modifier = Modifier.shake(shake),
            )
            Spacer(Modifier.weight(1f))
            PinPad(
                keySize = keySize,
                columnGap = 26.dp,
                rowGap = 16.dp,
                bottomStart = {
                    model.onBiometric?.let { onBiometric ->
                        IconKey(R.drawable.ic_fingerprint, stringResource(R.string.lock_use_biometric), onBiometric)
                    }
                },
                bottomEnd = { IconKey(R.drawable.ic_backspace, stringResource(R.string.lock_delete), model.onBackspace) },
            ) { digit -> DigitKey(digit, model.onDigit) }
            Spacer(Modifier.height(22.dp))
            Row(Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                LockTextAction(stringResource(R.string.lock_forgot_pin), dimText, model.onForgotPin, 15.sp)
                LockTextAction(stringResource(R.string.lock_cancel), dimText, model.onCancel, 15.sp)
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    @Composable
    private fun DigitKey(digit: Int, onDigit: (Int) -> Unit) {
        val haptics = LocalHapticFeedback.current
        Box(
            Modifier
                .size(keySize)
                .clip(CircleShape)
                .background(keyFill)
                .clickable(role = Role.Button) {
                    haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                    onDigit(digit)
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(digit.toString(), color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Light)
        }
    }

    @Composable
    private fun IconKey(iconRes: Int, description: String, onClick: () -> Unit) {
        Box(
            Modifier.size(keySize).clip(CircleShape).clickable(role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(iconRes), description, tint = Color.White, modifier = Modifier.size(30.dp))
        }
    }
}
