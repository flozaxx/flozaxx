package com.flozaxx.applock.protection

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.flozaxx.applock.R
import com.flozaxx.applock.notifications.Channels
import com.flozaxx.applock.notifications.NewAppNotifier

/**
 * Keeps the process in the foreground class so the vendor's background limits leave the lock
 * alone, and listens for newly installed apps (that broadcast only reaches receivers registered
 * at runtime).
 */
class GuardService : Service() {

    private val packageAdded = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) return
            val installedPackage = intent.data?.schemeSpecificPart ?: return
            NewAppNotifier.offerToLock(context, installedPackage)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter(Intent.ACTION_PACKAGE_ADDED).apply { addDataScheme("package") }
        ContextCompat.registerReceiver(this, packageAdded, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = Notification.Builder(this, Channels.GUARD)
            .setSmallIcon(R.drawable.ic_shield)
            .setVisibility(Notification.VISIBILITY_SECRET)
            .setOngoing(true)
            .build()
        startForeground(Channels.GUARD_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        return START_STICKY
    }

    override fun onDestroy() {
        unregisterReceiver(packageAdded)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        fun start(context: Context) {
            try {
                context.startForegroundService(Intent(context, GuardService::class.java))
            } catch (_: ForegroundServiceStartNotAllowedException) {
                // Raised when the process was woken in the background without the battery exemption.
                // The next start from the UI or the accessibility service succeeds.
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            GuardService.start(context)
        }
    }
}
