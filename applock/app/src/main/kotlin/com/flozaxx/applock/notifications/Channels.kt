package com.flozaxx.applock.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.flozaxx.applock.R

object Channels {
    const val GUARD = "guard"
    const val HIDDEN = "hidden"
    const val NEW_APP = "new_app"

    const val GUARD_NOTIFICATION_ID = 1
    const val HIDDEN_NOTIFICATION_ID = 2
    const val NEW_APP_NOTIFICATION_ID = 3

    fun create(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                channel(context, GUARD, R.string.channel_guard, NotificationManager.IMPORTANCE_MIN),
                channel(context, HIDDEN, R.string.channel_hidden, NotificationManager.IMPORTANCE_LOW),
                channel(context, NEW_APP, R.string.channel_new_app, NotificationManager.IMPORTANCE_DEFAULT),
            ),
        )
    }

    private fun channel(context: Context, id: String, nameRes: Int, importance: Int) =
        NotificationChannel(id, context.getString(nameRes), importance).apply {
            lockscreenVisibility = Notification.VISIBILITY_SECRET
            setShowBadge(false)
        }
}
