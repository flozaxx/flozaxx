package com.flozaxx.applock.ui

import android.graphics.Color
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import com.flozaxx.applock.R
import com.flozaxx.applock.graph
import com.flozaxx.applock.lock.LockController
import com.flozaxx.applock.lock.LockScreenModel
import com.flozaxx.applock.lock.LockScreenStyles
import com.flozaxx.applock.lock.LockTarget
import com.flozaxx.applock.lock.hasEnrolledBiometric
import com.flozaxx.applock.lock.showBiometricPrompt
import com.flozaxx.applock.storage.AppSettings
import com.flozaxx.applock.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The PIN screen in front of this app's own settings, drawn in the style the user picked. */
@Composable
fun AppGate(settings: AppSettings, onUnlocked: () -> Unit) {
    val pin = settings.pin ?: return
    val activity = LocalActivity.current as FragmentActivity
    val graph = LocalContext.current.graph
    val scope = rememberCoroutineScope()
    val appName = stringResource(R.string.app_name)

    // The lock styles are dark regardless of the system theme, so the status bar icons must be light.
    DisposableEffect(Unit) {
        activity.enableEdgeToEdge(SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT))
        onDispose { activity.enableEdgeToEdge() }
    }

    val controller = remember {
        LockController(
            scope = scope,
            pinLength = pin.length,
            verify = { entry -> withContext(Dispatchers.Default) { graph.vault.matches(entry, pin) } },
            store = graph.store,
            onUnlocked = {
                graph.coordinator.onOwnerVerified()
                onUnlocked()
            },
        )
    }
    val biometricAllowed = settings.biometricEnabled && remember { activity.hasEnrolledBiometric() }

    AppTheme(darkTheme = true) {
        LockScreenStyles.byId(settings.lockStyleId).Content(
            LockScreenModel(
                state = controller.state,
                target = LockTarget(appName, icon = null),
                onDigit = controller::onDigit,
                onBackspace = controller::onBackspace,
                onBiometric = { activity.showBiometricPrompt(appName, controller::unlockWithoutPin) }
                    .takeIf { biometricAllowed },
                onForgotPin = { activity.startActivity(MainActivity.recoveryIntent(activity)) },
                onCancel = { activity.moveTaskToBack(true) },
            ),
            Modifier,
        )
    }
}
