package com.flozaxx.applock.detection

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.os.PowerManager
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import com.flozaxx.applock.graph
import com.flozaxx.applock.lock.Curtain
import com.flozaxx.applock.lock.LockActivity
import com.flozaxx.applock.protection.GuardService

/**
 * Watches which app comes to the front and asks the coordinator whether it has to be covered.
 */
class AppLockAccessibilityService : AccessibilityService() {

    private lateinit var curtain: Curtain
    private var ignoredPackages = emptySet<String>()

    private val deviceEvents = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> graph.coordinator.onScreenOff()
                // The app in front does not always announce itself again after the keyguard goes away.
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> reportActiveWindow()
                LocationManager.MODE_CHANGED_ACTION -> graph.wifi.start()
            }
        }
    }

    override fun onServiceConnected() {
        // Security: fail closed. If the lock screen could not be shown, the locked app is sent to the
        // background instead of being left open.
        curtain = Curtain(this, onLockScreenMissing = { performGlobalAction(GLOBAL_ACTION_HOME) })
        ignoredPackages = SYSTEM_SURFACES + keyboardPackages()

        val power = getSystemService(PowerManager::class.java)
        graph.coordinator.presentLock = { lockedPackage ->
            // No cover over a lock screen that is already showing (apps announce several windows while
            // starting), and none while the display is off, where nothing can be read anyway.
            if (power.isInteractive && !graph.coordinator.lockScreenResumed) curtain.show()
            startActivity(LockActivity.intent(this, lockedPackage))
        }
        graph.coordinator.onLockScreenVisible = curtain::hide

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(LocationManager.MODE_CHANGED_ACTION)
        }
        ContextCompat.registerReceiver(this, deviceEvents, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        GuardService.start(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        if (packageName in ignoredPackages) return
        // The lock screen sits on top of the locked app; it must not count as leaving that app.
        if (event.className?.toString() == LockActivity::class.java.name) return
        graph.coordinator.onForegroundChanged(packageName)
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        unregisterReceiver(deviceEvents)
        curtain.hide()
        graph.coordinator.presentLock = null
        graph.coordinator.onLockScreenVisible = null
        return super.onUnbind(intent)
    }

    private fun reportActiveWindow() {
        val packageName = rootInActiveWindow?.packageName?.toString() ?: return
        if (packageName !in ignoredPackages && packageName != this.packageName) {
            graph.coordinator.onForegroundChanged(packageName)
        }
    }

    private fun keyboardPackages(): Set<String> =
        getSystemService(InputMethodManager::class.java).enabledInputMethodList.map { it.packageName }.toSet()

    private companion object {
        /**
         * Windows that appear on top of an app without the user leaving it: notification shade,
         * share sheet, permission dialogs. Treating them as "another app" would relock needlessly.
         */
        val SYSTEM_SURFACES = setOf(
            "android",
            "com.android.systemui",
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller",
            "com.samsung.android.permissioncontroller",
        )
    }
}
