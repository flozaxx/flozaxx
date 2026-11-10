package com.flozaxx.applock.notifications

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.flozaxx.applock.R
import com.flozaxx.applock.graph
import com.flozaxx.applock.lock.LockActivity
import com.flozaxx.applock.storage.NotificationMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Removes notifications of locked apps while the lock is active and, depending on the app's mode,
 * posts a content-free stand-in.
 *
 * The system shows a notification before listeners hear about it, so the original can flash for
 * a moment (longer for heads-up pop-ups). Ongoing, foreground-service and media notifications
 * cannot be removed by a listener at all.
 */
class NotificationGuardService : NotificationListenerService() {

    private var scope: CoroutineScope? = null

    override fun onListenerConnected() {
        scope?.cancel()
        val scope = CoroutineScope(Job() + Dispatchers.Main.immediate).also { scope = it }
        scope.launch {
            graph.coordinator.unlocked.collect { packageName -> notificationManager().cancel(packageName, Channels.HIDDEN_NOTIFICATION_ID) }
        }
        // Covers both a fresh connection and the lock switching on (coming home) with notifications already showing.
        scope.launch {
            graph.coordinator.lockActive.collect { active -> if (active) activeNotifications.forEach(::guard) }
        }
    }

    override fun onListenerDisconnected() {
        scope?.cancel()
        scope = null
        // Compat: some vendor builds drop the listener binding after memory pressure and never restore it.
        requestRebind(ComponentName(this, NotificationGuardService::class.java))
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) = guard(sbn)

    private fun guard(sbn: StatusBarNotification) {
        val source = sbn.packageName
        if (source == packageName) return
        val mode = graph.store.settings.value.lockedApps[source]?.notificationMode ?: return
        if (mode == NotificationMode.UNTOUCHED || !graph.coordinator.isLockActive()) return
        if (!sbn.isClearable || sbn.notification.isMediaPlayback()) return

        cancelNotification(sbn.key)
        if (mode != NotificationMode.HIDE_CONTENT) return
        // The group summary only repeats what its children say; one stand-in per app is enough.
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        sbn.notification.contentIntent?.let { graph.hiddenContentIntents[source] = it }
        postStandIn(source)
    }

    private fun postStandIn(source: String) {
        val openBehindLock = PendingIntent.getActivity(
            this,
            source.hashCode(),
            LockActivity.intent(this, source, openAfterUnlock = true),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val standIn = Notification.Builder(this, Channels.HIDDEN)
            .setSmallIcon(R.drawable.ic_lock)
            .setContentTitle(appLabel(source))
            .setContentText(getString(R.string.notification_hidden_text))
            .setVisibility(Notification.VISIBILITY_SECRET)
            .setContentIntent(openBehindLock)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()
        notificationManager().notify(source, Channels.HIDDEN_NOTIFICATION_ID, standIn)
    }

    private fun Notification.isMediaPlayback(): Boolean =
        extras.containsKey(Notification.EXTRA_MEDIA_SESSION) || category == Notification.CATEGORY_TRANSPORT

    private fun appLabel(source: String): String = try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(source, 0)).toString()
    } catch (_: PackageManager.NameNotFoundException) {
        source
    }

    private fun notificationManager() = getSystemService(NotificationManager::class.java)
}
