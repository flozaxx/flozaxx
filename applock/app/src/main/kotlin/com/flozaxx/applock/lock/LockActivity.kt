package com.flozaxx.applock.lock

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flozaxx.applock.graph
import com.flozaxx.applock.ui.MainActivity
import com.flozaxx.applock.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The screen that covers a locked app until the user proves who they are.
 *
 * Lives in its own task so it can sit on top of any app, and removes itself as soon as the user
 * leaves, so a stale lock screen never resurfaces over an unrelated app.
 */
class LockActivity : FragmentActivity() {

    private data class Request(val packageName: String, val openAfterUnlock: Boolean)

    private var request by mutableStateOf<Request?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        setRecentsScreenshotEnabled(false)
        overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
        overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, android.R.anim.fade_out)
        onBackPressedDispatcher.addCallback(this) { leaveToHomeScreen() }

        accept(intent)
        setContent {
            AppTheme(darkTheme = true) {
                request?.let { current -> key(current) { LockContent(current) } }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        accept(intent)
    }

    override fun onResume() {
        super.onResume()
        window.decorView.post { graph.coordinator.onLockScreenVisible?.invoke() }
    }

    override fun onStop() {
        super.onStop()
        // While the display is off the lock screen has to stay, so it is already there on wake-up.
        val screenOn = getSystemService(PowerManager::class.java).isInteractive
        if (screenOn && !isChangingConfigurations) finish()
    }

    private fun accept(intent: Intent) {
        val packageName = intent.getStringExtra(EXTRA_PACKAGE)
        if (packageName == null) {
            finish()
            return
        }
        val incoming = Request(packageName, intent.getBooleanExtra(EXTRA_OPEN_AFTER_UNLOCK, false))
        if (graph.coordinator.shouldLock(packageName)) request = incoming else close(incoming, authenticated = false)
    }

    @Composable
    private fun LockContent(current: Request) {
        val settings by graph.store.settings.collectAsStateWithLifecycle()
        val pin = settings.pin ?: return
        val scope = rememberCoroutineScope()
        val target = remember { loadLockTarget(current.packageName) }
        val controller = remember {
            LockController(
                scope = scope,
                pinLength = pin.length,
                verify = { entry -> withContext(Dispatchers.Default) { graph.vault.matches(entry, pin) } },
                store = graph.store,
                onUnlocked = { close(current, authenticated = true) },
            )
        }

        val biometricAllowed = settings.biometricAllowedFor(current.packageName) && remember { hasEnrolledBiometric() }
        val askBiometric = { showBiometricPrompt(target.label, controller::unlockWithoutPin) }
        var promptedOnOpen by remember { mutableStateOf(false) }
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
            if (biometricAllowed && !promptedOnOpen) {
                promptedOnOpen = true
                askBiometric()
            }
        }

        LockScreenStyles.byId(settings.lockStyleId).Content(
            LockScreenModel(
                state = controller.state,
                target = target,
                onDigit = controller::onDigit,
                onBackspace = controller::onBackspace,
                onBiometric = askBiometric.takeIf { biometricAllowed },
                onForgotPin = {
                    startActivity(MainActivity.recoveryIntent(this))
                    finish()
                },
                onCancel = ::leaveToHomeScreen,
            ),
            Modifier,
        )
    }

    private fun close(finished: Request, authenticated: Boolean) {
        if (authenticated) graph.coordinator.onAuthenticated(finished.packageName)
        if (finished.openAfterUnlock) openTarget(finished.packageName)
        finish()
    }

    private fun openTarget(packageName: String) {
        val hiddenTap = graph.hiddenContentIntents.remove(packageName)
        if (hiddenTap != null) {
            val options = ActivityOptions.makeBasic().setPendingIntentBackgroundActivityStartMode(startMode()).toBundle()
            try {
                hiddenTap.send(this, 0, null, null, null, null, options)
                return
            } catch (_: PendingIntent.CanceledException) {
                // The app withdrew that notification's target; opening the app itself is the next best thing.
            }
        }
        packageManager.getLaunchIntentForPackage(packageName)?.let(::startActivity)
    }

    @Suppress("DEPRECATION")
    private fun startMode(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS
        else ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED

    private fun leaveToHomeScreen() {
        startActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        finish()
    }

    companion object {
        private const val EXTRA_PACKAGE = "package"
        private const val EXTRA_OPEN_AFTER_UNLOCK = "open_after_unlock"

        /**
         * @param openAfterUnlock true when the lock screen is not shown over the app (a tapped
         * notification), so the app still has to be opened once the PIN is accepted.
         */
        fun intent(context: Context, packageName: String, openAfterUnlock: Boolean = false): Intent =
            Intent(context, LockActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                .putExtra(EXTRA_PACKAGE, packageName)
                .putExtra(EXTRA_OPEN_AFTER_UNLOCK, openAfterUnlock)
    }
}

fun Context.loadLockTarget(packageName: String): LockTarget = try {
    val info = packageManager.getApplicationInfo(packageName, 0)
    LockTarget(
        label = packageManager.getApplicationLabel(info).toString(),
        icon = packageManager.getApplicationIcon(info).toBitmap(192, 192).asImageBitmap(),
    )
} catch (_: PackageManager.NameNotFoundException) {
    LockTarget(packageName, null)
}
