package com.flozaxx.applock.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import com.flozaxx.applock.protection.GuardService
import com.flozaxx.applock.ui.theme.AppTheme

class MainActivity : FragmentActivity() {

    private var unlocked by mutableStateOf(false)
    private var recoveryRequested by mutableStateOf(false)
    private var stoppedAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        recoveryRequested = intent.getBooleanExtra(EXTRA_RECOVERY, false)
        setContent {
            AppTheme {
                AppRoot(
                    unlocked = unlocked,
                    onUnlocked = { unlocked = true },
                    recoveryRequested = recoveryRequested,
                    onRecoveryClosed = { recoveryRequested = false },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(EXTRA_RECOVERY, false)) recoveryRequested = true
    }

    override fun onStart() {
        super.onStart()
        GuardService.start(this)
        // Short trips to a system settings page should not ask for the PIN again; a longer absence does.
        if (stoppedAt != 0L && SystemClock.elapsedRealtime() - stoppedAt > RELOCK_AFTER_MILLIS) unlocked = false
    }

    override fun onStop() {
        super.onStop()
        stoppedAt = SystemClock.elapsedRealtime()
    }

    companion object {
        private const val EXTRA_RECOVERY = "recovery"
        private const val RELOCK_AFTER_MILLIS = 60_000L

        fun recoveryIntent(context: Context): Intent =
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(EXTRA_RECOVERY, true)
    }
}
