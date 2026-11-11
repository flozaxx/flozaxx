package com.flozaxx.applock.ui

import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

private enum class Destination { RECOVERY, CREDENTIALS, WELCOME, SETUP, GATE, MAIN }

@Composable
fun AppRoot(
    darkTheme: Boolean,
    unlocked: Boolean,
    onUnlocked: () -> Unit,
    recoveryRequested: Boolean,
    onRecoveryClosed: () -> Unit,
) {
    val settings by LocalContext.current.graph.store.settings.collectAsStateWithLifecycle()
    // Stays true from the first PIN screen until the recovery code has been shown, even though the
    // PIN is already stored halfway through.
    var creatingCredentials by rememberSaveable { mutableStateOf(false) }

    val destination = when {
        recoveryRequested && settings.pin != null -> Destination.RECOVERY
        creatingCredentials -> Destination.CREDENTIALS
        settings.pin == null -> Destination.WELCOME
        !settings.setupComplete -> Destination.SETUP
        !unlocked -> Destination.GATE
        else -> Destination.MAIN
    }
    // The PIN gate is dark in every theme, so it needs light status bar icons of its own.
    SystemBars(dark = darkTheme || destination == Destination.GATE)

    when (destination) {
        Destination.RECOVERY -> ForgotPinScreen(
            onRecovered = {
                onUnlocked()
                onRecoveryClosed()
            },
            onBack = onRecoveryClosed,
        )
        Destination.CREDENTIALS -> CredentialSetup(
            mode = CredentialSetupMode.FIRST_TIME,
            onFinished = {
                creatingCredentials = false
                onUnlocked()
            },
            onBack = null,
        )
        Destination.WELCOME -> WelcomeScreen(onStart = { creatingCredentials = true })
        Destination.SETUP -> ProtectionScreen(setupMode = true, onDone = {}, onBack = null)
        Destination.GATE -> AppGate(settings, onUnlocked)
        Destination.MAIN -> MainScreens()
    }
}

@Composable
private fun SystemBars(dark: Boolean) {
    val activity = LocalActivity.current as ComponentActivity
    LaunchedEffect(dark) {
        val style =
            if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
            else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        activity.enableEdgeToEdge(style, style)
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
