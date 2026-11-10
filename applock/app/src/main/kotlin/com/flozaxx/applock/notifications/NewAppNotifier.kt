package com.flozaxx.applock.notifications

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import com.flozaxx.applock.R
import com.flozaxx.applock.graph
import com.flozaxx.applock.storage.LockedApp
import kotlinx.coroutines.launch

/** "X was installed, lock it?" with a Lock and a Don't lock action. */
object NewAppNotifier {

    fun offerToLock(context: Context, installedPackage: String) {
        val packageManager = context.packageManager
        // Apps without a launcher entry (services, plug-ins) cannot be opened, so there is nothing to lock.
        if (packageManager.getLaunchIntentForPackage(installedPackage) == null) return
        if (installedPackage in context.graph.store.settings.value.lockedApps) return
        val label = try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(installedPackage, 0))
        } catch (_: PackageManager.NameNotFoundException) {
            return
        }

        val notification = Notification.Builder(context, Channels.NEW_APP)
            .setSmallIcon(R.drawable.ic_lock)
            .setContentTitle(context.getString(R.string.new_app_title, label))
            .setContentText(context.getString(R.string.new_app_text))
            .setVisibility(Notification.VISIBILITY_SECRET)
            .setAutoCancel(true)
            .addAction(action(context, R.string.new_app_lock, NewAppActionReceiver.ACTION_LOCK, installedPackage))
            .addAction(action(context, R.string.new_app_skip, NewAppActionReceiver.ACTION_SKIP, installedPackage))
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(installedPackage, Channels.NEW_APP_NOTIFICATION_ID, notification)
    }

    private fun action(context: Context, labelRes: Int, action: String, installedPackage: String): Notification.Action {
        val intent = Intent(context, NewAppActionReceiver::class.java)
            .setAction(action)
            .putExtra(NewAppActionReceiver.EXTRA_PACKAGE, installedPackage)
        val pending = PendingIntent.getBroadcast(
            context,
            installedPackage.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Action.Builder(null as Icon?, context.getString(labelRes), pending).build()
    }
}

class NewAppActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val installedPackage = intent.getStringExtra(EXTRA_PACKAGE) ?: return
        context.getSystemService(NotificationManager::class.java).cancel(installedPackage, Channels.NEW_APP_NOTIFICATION_ID)
        if (intent.action != ACTION_LOCK) return

        val store = context.graph.store
        val pending = goAsync()
        context.graph.scope.launch {
            store.update { settings ->
                settings.copy(lockedApps = settings.lockedApps + (installedPackage to LockedApp(settings.defaultNotificationMode)))
            }
            pending.finish()
        }
    }

    companion object {
        const val ACTION_LOCK = "com.flozaxx.applock.action.LOCK_NEW_APP"
        const val ACTION_SKIP = "com.flozaxx.applock.action.SKIP_NEW_APP"
        const val EXTRA_PACKAGE = "package"
    }
}
