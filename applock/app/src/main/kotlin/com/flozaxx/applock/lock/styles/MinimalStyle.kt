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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.flozaxx.applock.lock.PinPad
import com.flozaxx.applock.lock.isError
import com.flozaxx.applock.lock.lockMessage
import com.flozaxx.applock.lock.rememberShake
import com.flozaxx.applock.lock.shake
import com.flozaxx.applock.lock.unlockFade

/** Black screen, one colour, no key outlines; progress is a row of short bars. */
object MinimalStyle : LockScreenStyle {
    override val id = "minimal"
    override val nameRes = R.string.style_minimal

    private val keySize = 72.dp
    private val ink = Color(0xFFEDEDED)
    private val faint = ink.copy(alpha = 0.28f)

    @Composable
    override fun Content(model: LockScreenModel, modifier: Modifier) {
        val state = model.state
        val shake = rememberShake(state.wrongCount)
        Column(
            modifier
                .fillMaxSize()
                .background(Color.Black)
                .unlockFade(state.status)
                .systemBarsPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            Text(model.target.label, color = ink, fontSize = 17.sp, letterSpacing = 1.sp)
            Spacer(Modifier.height(6.dp))
            Text(lockMessage(state), color = if (state.isError) ink else faint, fontSize = 13.sp)
            Spacer(Modifier.height(30.dp))
            Row(Modifier.shake(shake), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(state.pinLength) { index ->
                    Box(Modifier.width(22.dp).height(3.dp).background(if (index < state.enteredCount) ink else faint))
                }
            }
            Spacer(Modifier.weight(1f))
            PinPad(
                keySize = keySize,
                columnGap = 30.dp,
                rowGap = 10.dp,
                bottomStart = {
                    model.onBiometric?.let { onBiometric ->
                        PlainKey(onBiometric) {
                            Icon(
                                painterResource(R.drawable.ic_fingerprint),
                                stringResource(R.string.lock_use_biometric),
                                tint = ink,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    }
                },
                bottomEnd = {
                    PlainKey(model.onBackspace) {
                        Icon(
                            painterResource(R.drawable.ic_backspace),
                            stringResource(R.string.lock_delete),
                            tint = ink,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                },
            ) { digit ->
                PlainKey({ model.onDigit(digit) }) {
                    Text(digit.toString(), color = ink, fontSize = 30.sp, fontWeight = FontWeight.Thin)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                LockTextAction(stringResource(R.string.lock_forgot_pin), faint, model.onForgotPin, 13.sp)
                LockTextAction(stringResource(R.string.lock_cancel), faint, model.onCancel, 13.sp)
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    @Composable
    private fun PlainKey(onClick: () -> Unit, content: @Composable () -> Unit) {
        Box(
            Modifier.size(keySize).clip(CircleShape).clickable(role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) { content() }
    }
}
