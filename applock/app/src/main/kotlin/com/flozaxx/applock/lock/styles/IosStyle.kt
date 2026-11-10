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
import com.flozaxx.applock.lock.isError
import com.flozaxx.applock.lock.lockMessage
import com.flozaxx.applock.lock.rememberShake
import com.flozaxx.applock.lock.shake
import com.flozaxx.applock.lock.unlockFade

/** Translucent round keys with letter rows under the digits; the delete/cancel action is plain text. */
object IosStyle : LockScreenStyle {
    override val id = "ios"
    override val nameRes = R.string.style_ios

    private val keySize = 78.dp
    private val background = Brush.verticalGradient(listOf(Color(0xFF3A4A66), Color(0xFF1C2333), Color(0xFF10131C)))
    private val keyFill = Color.White.copy(alpha = 0.18f)
    private val letters = listOf("", "", "ABC", "DEF", "GHI", "JKL", "MNO", "PQRS", "TUV", "WXYZ")

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
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            Icon(painterResource(R.drawable.ic_lock), null, tint = Color.White, modifier = Modifier.size(26.dp))
            Spacer(Modifier.height(14.dp))
            Text(
                if (state.isError) lockMessage(state) else stringResource(R.string.lock_enter_pin_for, model.target.label),
                color = Color.White,
                fontSize = 19.sp,
            )
            Spacer(Modifier.height(22.dp))
            PinDots(
                total = state.pinLength,
                filled = state.enteredCount,
                color = Color.White,
                dotSize = 13.dp,
                gap = 22.dp,
                modifier = Modifier.shake(shake),
            )
            Spacer(Modifier.weight(1f))
            PinPad(
                keySize = keySize,
                columnGap = 24.dp,
                rowGap = 16.dp,
                bottomStart = {
                    model.onBiometric?.let { onBiometric ->
                        Box(
                            Modifier.size(keySize).clip(CircleShape).clickable(role = Role.Button, onClick = onBiometric),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_fingerprint),
                                stringResource(R.string.lock_use_biometric),
                                tint = Color.White,
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    }
                },
            ) { digit -> DigitKey(digit, model.onDigit) }
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                LockTextAction(stringResource(R.string.lock_forgot_pin), Color.White, model.onForgotPin, 16.sp)
                if (state.enteredCount > 0) {
                    LockTextAction(stringResource(R.string.lock_delete), Color.White, model.onBackspace, 16.sp)
                } else {
                    LockTextAction(stringResource(R.string.lock_cancel), Color.White, model.onCancel, 16.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    @Composable
    private fun DigitKey(digit: Int, onDigit: (Int) -> Unit) {
        val haptics = LocalHapticFeedback.current
        Column(
            Modifier
                .size(keySize)
                .clip(CircleShape)
                .background(keyFill)
                .clickable(role = Role.Button) {
                    haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                    onDigit(digit)
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(digit.toString(), color = Color.White, fontSize = 32.sp, lineHeight = 34.sp)
            if (letters[digit].isNotEmpty()) {
                Text(
                    letters[digit],
                    color = Color.White,
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                )
            }
        }
    }
}
