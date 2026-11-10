package com.flozaxx.applock.lock

import android.os.SystemClock
import com.flozaxx.applock.context.WifiMonitor
import com.flozaxx.applock.context.WifiSnapshot
import com.flozaxx.applock.rules.LockRules
import com.flozaxx.applock.rules.LockSession
import com.flozaxx.applock.rules.LockZone
import com.flozaxx.applock.rules.NetworkState
import com.flozaxx.applock.storage.AppSettings
import com.flozaxx.applock.storage.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Connects what the device reports (foreground app, network, screen) to the rule engine and
 * keeps the unlock session. Everything here runs on the main thread.
 */
class LockCoordinator(
    private val store: SettingsStore,
    private val wifi: WifiMonitor,
    private val scope: CoroutineScope,
    private val now: () -> Long = SystemClock::elapsedRealtime,
) {
    private var session = LockSession()

    /** Set by the accessibility service while it is connected; shows the lock screen over an app. */
    var presentLock: ((packageName: String) -> Unit)? = null

    /** Called once the lock screen is on screen, so the service can drop its cover. */
    var onLockScreenVisible: (() -> Unit)? = null

    private val _unlocked = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val unlocked: SharedFlow<String> = _unlocked

    val lockActive: Flow<Boolean> = combine(wifi.state, store.settings) { snapshot, settings ->
        LockRules.isLockActive(zone(settings, snapshot), settings.zoneExitConfirmed)
    }.distinctUntilChanged()

    init {
        scope.launch {
            combine(wifi.state, store.settings) { snapshot, settings -> zone(settings, snapshot) to settings }
                .collect { (zone, settings) ->
                    val confirmed = LockRules.exitConfirmedAfterZoneChange(zone, settings.zoneExitConfirmed)
                    if (confirmed != settings.zoneExitConfirmed) {
                        store.update { it.copy(zoneExitConfirmed = confirmed) }
                    }
                }
        }
    }

    fun networkState(
        settings: AppSettings = store.settings.value,
        snapshot: WifiSnapshot = wifi.state.value,
    ): NetworkState = when (snapshot) {
        WifiSnapshot.Unreadable -> NetworkState.UNKNOWN
        WifiSnapshot.NotConnected -> NetworkState.UNTRUSTED
        is WifiSnapshot.Connected ->
            if (settings.trustedNetworks.any { it.bssid == snapshot.bssid }) NetworkState.TRUSTED
            else NetworkState.UNTRUSTED
    }

    private fun zone(settings: AppSettings, snapshot: WifiSnapshot = wifi.state.value): LockZone =
        LockRules.zone(settings.networkRule, networkState(settings, snapshot), settings.trustedNetworks.isNotEmpty())

    fun isLockActive(): Boolean {
        val settings = store.settings.value
        return LockRules.isLockActive(zone(settings), settings.zoneExitConfirmed)
    }

    fun shouldLock(packageName: String): Boolean {
        val settings = store.settings.value
        return LockRules.shouldLock(
            packageName, now(), settings.ruleSettings, networkState(settings), settings.zoneExitConfirmed, session,
        )
    }

    fun onForegroundChanged(packageName: String) {
        val lock = shouldLock(packageName)
        session = session.onForegroundChanged(packageName, now(), store.settings.value.relockPolicy)
        if (lock) presentLock?.invoke(packageName)
    }

    fun onAuthenticated(packageName: String) {
        session = session.onAuthenticated(packageName, now())
        onOwnerVerified()
        _unlocked.tryEmit(packageName)
    }

    /** The owner proved their identity; if the device is outside the lock zone, the lock may now rest. */
    fun onOwnerVerified() {
        val settings = store.settings.value
        val confirmed = LockRules.exitConfirmedAfterAuthentication(zone(settings), settings.zoneExitConfirmed)
        if (confirmed != settings.zoneExitConfirmed) {
            scope.launch { store.update { it.copy(zoneExitConfirmed = confirmed) } }
        }
    }

    /**
     * Puts the lock screen up while the display is still off, so the app that was in front is
     * already covered when the screen comes back.
     */
    fun onScreenOff() {
        val inFront = session.foregroundPackage
        session = session.onScreenOff(now(), store.settings.value.relockPolicy)
        if (inFront != null && shouldLock(inFront)) presentLock?.invoke(inFront)
    }

    fun onAppUnlisted(packageName: String) {
        session = session.onPackageRelocked(packageName)
    }
}
