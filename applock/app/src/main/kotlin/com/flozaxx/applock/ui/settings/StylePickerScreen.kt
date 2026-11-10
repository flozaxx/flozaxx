package com.flozaxx.applock.ui.settings

import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flozaxx.applock.R
import com.flozaxx.applock.graph
import com.flozaxx.applock.lock.LockController
import com.flozaxx.applock.lock.LockScreenModel
import com.flozaxx.applock.lock.LockScreenStyle
import com.flozaxx.applock.lock.LockScreenStyles
import com.flozaxx.applock.lock.LockTarget
import com.flozaxx.applock.lock.LockUiState
import com.flozaxx.applock.ui.components.ScreenScaffold
import com.flozaxx.applock.ui.theme.AppTheme
import kotlinx.coroutines.launch

private val PHONE_WIDTH = 360.dp
private val PHONE_HEIGHT = 720.dp

@Composable
fun StylePickerScreen(onBack: () -> Unit) {
    val graph = LocalContext.current.graph
    val scope = rememberCoroutineScope()
    val settings by graph.store.settings.collectAsStateWithLifecycle()
    val selected = LockScreenStyles.byId(settings.lockStyleId)
    val pinLength = settings.pin?.length ?: 4
    val target = LockTarget(stringResource(R.string.style_preview_app), icon = null)

    ScreenScaffold(title = stringResource(R.string.style_setting), onBack = onBack) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.style_preview_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            ScaledPhone(scale = 0.62f, selected = false) {
                key(selected.id) { LivePreview(selected, target, pinLength) }
            }
            Spacer(Modifier.height(20.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                LockScreenStyles.all.forEach { style ->
                    Column(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(role = Role.RadioButton) {
                                scope.launch { graph.store.update { it.copy(lockStyleId = style.id) } }
                            }
                            .padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ScaledPhone(scale = 0.2f, selected = style.id == selected.id) {
                            style.Content(
                                LockScreenModel(LockUiState(pinLength, enteredCount = 2), target, {}, {}, null, null, null),
                                Modifier,
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            stringResource(style.nameRes),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (style.id == selected.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * A working copy of the lock screen that is not connected to the real PIN: the first complete
 * entry is rejected to show the error animation, the next one is accepted to show the unlock.
 */
@Composable
private fun LivePreview(style: LockScreenStyle, target: LockTarget, pinLength: Int) {
    val scope = rememberCoroutineScope()
    var round by remember { mutableIntStateOf(0) }
    key(round) {
        val controller = remember {
            var entries = 0
            LockController(
                scope = scope,
                pinLength = pinLength,
                verify = { entries++ > 0 },
                store = null,
                onUnlocked = { round++ },
            )
        }
        style.Content(
            LockScreenModel(
                state = controller.state,
                target = target,
                onDigit = controller::onDigit,
                onBackspace = controller::onBackspace,
                onBiometric = controller::unlockWithoutPin,
                onForgotPin = {},
                onCancel = {},
            ),
            Modifier,
        )
    }
}

/** Lays out [content] at phone size and shrinks it, so every style is previewed at its real proportions. */
@Composable
private fun ScaledPhone(scale: Float, selected: Boolean, content: @Composable () -> Unit) {
    val corner: Dp = 28.dp * scale
    Box(
        Modifier
            .size(PHONE_WIDTH * scale, PHONE_HEIGHT * scale)
            .clip(RoundedCornerShape(corner))
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(corner),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.requiredSize(PHONE_WIDTH, PHONE_HEIGHT).graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        ) {
            AppTheme(darkTheme = true) { content() }
        }
    }
}

