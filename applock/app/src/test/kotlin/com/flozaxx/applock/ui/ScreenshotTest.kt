package com.flozaxx.applock.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.flozaxx.applock.lock.LockScreenModel
import com.flozaxx.applock.lock.LockScreenStyles
import com.flozaxx.applock.lock.LockStatus
import com.flozaxx.applock.lock.LockTarget
import com.flozaxx.applock.lock.LockUiState
import com.flozaxx.applock.ui.apps.AppsScreen
import com.flozaxx.applock.ui.settings.LimitsScreen
import com.flozaxx.applock.ui.settings.SettingsScreen
import com.flozaxx.applock.ui.settings.StylePickerScreen
import com.flozaxx.applock.ui.setup.CredentialSetup
import com.flozaxx.applock.ui.setup.CredentialSetupMode
import com.flozaxx.applock.ui.setup.ProtectionScreen
import com.flozaxx.applock.ui.setup.WelcomeScreen
import com.flozaxx.applock.ui.theme.AppTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the screens off-device. The images land in build/outputs/roborazzi and are meant for a
 * human eye; the test itself only fails when a screen cannot be composed.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h780dp-xxhdpi")
class ScreenshotTest {

    private val target = LockTarget("Mesajlar", icon = null)

    private fun model(state: LockUiState, withBiometric: Boolean = true) =
        LockScreenModel(state, target, {}, {}, ({}).takeIf { withBiometric }, {}, {})

    @Test
    fun lockStyles() {
        LockScreenStyles.all.forEach { style ->
            captureRoboImage("build/outputs/roborazzi/lock_${style.id}.png") {
                AppTheme(darkTheme = true) {
                    style.Content(model(LockUiState(pinLength = 4, enteredCount = 2)), Modifier)
                }
            }
        }
    }

    @Test
    fun lockStylesLockedOut() {
        LockScreenStyles.all.forEach { style ->
            captureRoboImage("build/outputs/roborazzi/lock_${style.id}_lockout.png") {
                AppTheme(darkTheme = true) {
                    val state = LockUiState(pinLength = 6, status = LockStatus.LOCKED_OUT, lockoutSecondsLeft = 28)
                    style.Content(model(state, withBiometric = false), Modifier)
                }
            }
        }
    }

    @Test
    fun appScreens() {
        val screens = mapOf<String, @Composable () -> Unit>(
            "welcome" to { WelcomeScreen(onStart = {}) },
            "pin_create" to { CredentialSetup(CredentialSetupMode.FIRST_TIME, onFinished = {}, onBack = null) },
            "setup" to { ProtectionScreen(setupMode = true, onDone = {}, onBack = null) },
            "apps" to { AppsScreen(onOpenSettings = {}, onOpenProtection = {}) },
            "settings" to { SettingsScreen({}, {}, {}, {}, {}, {}) },
            "style_picker" to { StylePickerScreen(onBack = {}) },
            "limits" to { LimitsScreen(onBack = {}) },
        )
        screens.forEach { (name, screen) ->
            captureRoboImage("build/outputs/roborazzi/screen_$name.png") {
                AppTheme(darkTheme = false) { screen() }
            }
        }
    }
}
