package com.flozaxx.applock

import android.app.Application
import android.app.PendingIntent
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.flozaxx.applock.context.WifiMonitor
import com.flozaxx.applock.lock.LockCoordinator
import com.flozaxx.applock.notifications.Channels
import com.flozaxx.applock.storage.PinVault
import com.flozaxx.applock.storage.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.util.concurrent.ConcurrentHashMap

private val Context.settingsDataStore by preferencesDataStore("settings")

class App : Application() {

    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(this)
        Channels.create(this)
        graph.wifi.start()
    }
}

/** Process-wide objects shared by the services, the lock screen and the settings UI. */
class AppGraph(app: Application) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val store = SettingsStore(app.settingsDataStore, scope)
    val vault = PinVault()
    val wifi = WifiMonitor(app)
    val coordinator = LockCoordinator(store, wifi, scope)

    /** Tap targets of notifications we replaced, so the hidden notification still opens the right chat. */
    val hiddenContentIntents = ConcurrentHashMap<String, PendingIntent>()
}

val Context.graph: AppGraph
    get() = (applicationContext as App).graph
