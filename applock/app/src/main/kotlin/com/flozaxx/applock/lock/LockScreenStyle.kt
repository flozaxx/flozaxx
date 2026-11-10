package com.flozaxx.applock.lock

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import com.flozaxx.applock.lock.styles.IosStyle
import com.flozaxx.applock.lock.styles.MinimalStyle
import com.flozaxx.applock.lock.styles.PixelStyle
import com.flozaxx.applock.lock.styles.SamsungStyle

/** The app being unlocked, shown at the top of the lock screen. */
data class LockTarget(val label: String, val icon: ImageBitmap?)

/** Everything a style may show or trigger. Optional actions are null when they do not apply. */
class LockScreenModel(
    val state: LockUiState,
    val target: LockTarget,
    val onDigit: (Int) -> Unit,
    val onBackspace: () -> Unit,
    val onBiometric: (() -> Unit)?,
    val onForgotPin: (() -> Unit)?,
    val onCancel: (() -> Unit)?,
)

/**
 * How the lock screen looks and animates. Styles never verify anything themselves; they draw
 * [LockScreenModel.state] and forward key presses. Adding a style means one new file plus one
 * entry in [LockScreenStyles.all].
 */
interface LockScreenStyle {
    val id: String

    @get:StringRes
    val nameRes: Int

    @Composable
    fun Content(model: LockScreenModel, modifier: Modifier)
}

object LockScreenStyles {
    val all: List<LockScreenStyle> = listOf(SamsungStyle, PixelStyle, IosStyle, MinimalStyle)

    fun byId(id: String): LockScreenStyle = all.firstOrNull { it.id == id } ?: SamsungStyle
}
