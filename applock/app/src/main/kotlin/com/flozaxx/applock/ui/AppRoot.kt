package com.flozaxx.applock.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flozaxx.applock.graph
import com.flozaxx.applock.ui.apps.AppsScreen
import com.flozaxx.applock.ui.settings.LimitsScreen
import com.flozaxx.applock.ui.settings.SettingsScreen
import com.flozaxx.applock.ui.settings.StylePickerScreen
import com.flozaxx.applock.ui.setup.CredentialSetup
import com.flozaxx.applock.ui.setup.CredentialSetupMode
import com.flozaxx.applock.ui.setup.ForgotPinScreen
import com.flozaxx.applock.ui.setup.ProtectionScreen
import com.flozaxx.applock.ui.setup.WelcomeScreen

private enum class Screen { APPS, SETTINGS, STYLE, PROTECTION, LIMITS, CHANGE_PIN, NEW_RECOVERY_CODE }

@Composable
fun AppRoot(
    unlocked: Boolean,
    onUnlocked: () -> Unit,
    recoveryRequested: Boolean,
    onRecoveryClosed: () -> Unit,
) {
    val settings by LocalContext.current.graph.store.settings.collectAsStateWithLifecycle()
    // Stays true from the first PIN screen until the recovery code has been shown, even though the
    // PIN is already stored halfway through.
    var creatingCredentials by rememberSaveable { mutableStateOf(false) }

    when {
        recoveryRequested && settings.pin != null -> ForgotPinScreen(
            onRecovered = {
                onUnlocked()
                onRecoveryClosed()
            },
            onBack = onRecoveryClosed,
        )

        creatingCredentials -> CredentialSetup(
            mode = CredentialSetupMode.FIRST_TIME,
            onFinished = {
                creatingCredentials = false
                onUnlocked()
            },
            onBack = null,
        )

        settings.pin == null -> WelcomeScreen(onStart = { creatingCredentials = true })

        !settings.setupComplete -> ProtectionScreen(setupMode = true, onDone = {}, onBack = null)

        !unlocked -> AppGate(settings, onUnlocked)

        else -> MainScreens()
    }
}

@Composable
private fun MainScreens() {
    var stack by rememberSaveable { mutableStateOf(listOf(Screen.APPS)) }
    val open = { screen: Screen -> stack = stack + screen }
    val back = { stack = stack.dropLast(1) }
    BackHandler(enabled = stack.size > 1, onBack = back)

    AnimatedContent(
        targetState = stack.last(),
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "screen",
    ) { screen ->
        when (screen) {
            Screen.APPS -> AppsScreen(
                onOpenSettings = { open(Screen.SETTINGS) },
                onOpenProtection = { open(Screen.PROTECTION) },
            )
            Screen.SETTINGS -> SettingsScreen(
                onBack = back,
                onOpenStyle = { open(Screen.STYLE) },
                onOpenProtection = { open(Screen.PROTECTION) },
                onOpenLimits = { open(Screen.LIMITS) },
                onChangePin = { open(Screen.CHANGE_PIN) },
                onNewRecoveryCode = { open(Screen.NEW_RECOVERY_CODE) },
            )
            Screen.STYLE -> StylePickerScreen(onBack = back)
            Screen.PROTECTION -> ProtectionScreen(setupMode = false, onDone = back, onBack = back)
            Screen.LIMITS -> LimitsScreen(onBack = back)
            Screen.CHANGE_PIN -> CredentialSetup(CredentialSetupMode.CHANGE_PIN, onFinished = back, onBack = back)
            Screen.NEW_RECOVERY_CODE ->
                CredentialSetup(CredentialSetupMode.NEW_RECOVERY_CODE, onFinished = back, onBack = back)
        }
    }
}
