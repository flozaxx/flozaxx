package com.flozaxx.applock.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.flozaxx.applock.rules.NetworkRule
import com.flozaxx.applock.rules.RelockPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

class SettingsStore(private val dataStore: DataStore<Preferences>, scope: CoroutineScope) {

    // Perf: one small blocking read at process start. The accessibility service must know the
    // locked apps before its first window event, otherwise an app opened right after boot slips through.
    val settings: StateFlow<AppSettings> = dataStore.data
        .map(::read)
        .stateIn(scope, SharingStarted.Eagerly, runBlocking { read(dataStore.data.first()) })

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { preferences -> write(preferences, transform(read(preferences))) }
    }

    private fun read(p: Preferences): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            setupComplete = p[SETUP_COMPLETE] ?: defaults.setupComplete,
            lockedApps = p[LOCKED_APPS]?.let { json.decodeFromString(it) } ?: defaults.lockedApps,
            networkRule = p[NETWORK_RULE]?.let(NetworkRule::valueOf) ?: defaults.networkRule,
            trustedNetworks = p[TRUSTED_NETWORKS]?.let { json.decodeFromString(it) } ?: defaults.trustedNetworks,
            relockPolicy = p[RELOCK_POLICY]?.let(RelockPolicy::valueOf) ?: defaults.relockPolicy,
            relockTimeoutMinutes = p[RELOCK_TIMEOUT_MINUTES] ?: defaults.relockTimeoutMinutes,
            lockStyleId = p[LOCK_STYLE] ?: defaults.lockStyleId,
            themeMode = p[THEME_MODE]?.let(ThemeMode::valueOf) ?: defaults.themeMode,
            dynamicColor = p[DYNAMIC_COLOR] ?: defaults.dynamicColor,
            defaultNotificationMode = p[DEFAULT_NOTIFICATION_MODE]?.let(NotificationMode::valueOf)
                ?: defaults.defaultNotificationMode,
            biometricEnabled = p[BIOMETRIC_ENABLED] ?: defaults.biometricEnabled,
            pin = p[PIN]?.let { json.decodeFromString(it) },
            recoveryCode = p[RECOVERY_CODE]?.let { json.decodeFromString(it) },
            recoveryEmail = p[RECOVERY_EMAIL] ?: defaults.recoveryEmail,
            failedAttempts = p[FAILED_ATTEMPTS] ?: defaults.failedAttempts,
            lockoutUntilMillis = p[LOCKOUT_UNTIL] ?: defaults.lockoutUntilMillis,
            zoneExitConfirmed = p[ZONE_EXIT_CONFIRMED] ?: defaults.zoneExitConfirmed,
        )
    }

    private fun write(p: MutablePreferences, s: AppSettings) {
        p[SETUP_COMPLETE] = s.setupComplete
        p[LOCKED_APPS] = json.encodeToString(s.lockedApps)
        p[NETWORK_RULE] = s.networkRule.name
        p[TRUSTED_NETWORKS] = json.encodeToString(s.trustedNetworks)
        p[RELOCK_POLICY] = s.relockPolicy.name
        p[RELOCK_TIMEOUT_MINUTES] = s.relockTimeoutMinutes
        p[LOCK_STYLE] = s.lockStyleId
        p[THEME_MODE] = s.themeMode.name
        p[DYNAMIC_COLOR] = s.dynamicColor
        p[DEFAULT_NOTIFICATION_MODE] = s.defaultNotificationMode.name
        p[BIOMETRIC_ENABLED] = s.biometricEnabled
        p.setOrRemove(PIN, s.pin?.let { json.encodeToString(it) })
        p.setOrRemove(RECOVERY_CODE, s.recoveryCode?.let { json.encodeToString(it) })
        p[RECOVERY_EMAIL] = s.recoveryEmail
        p[FAILED_ATTEMPTS] = s.failedAttempts
        p[LOCKOUT_UNTIL] = s.lockoutUntilMillis
        p[ZONE_EXIT_CONFIRMED] = s.zoneExitConfirmed
    }

    private fun <T> MutablePreferences.setOrRemove(key: Preferences.Key<T>, value: T?) {
        if (value == null) remove(key) else this[key] = value
    }

    private companion object {
        val json = Json { ignoreUnknownKeys = true }

        val SETUP_COMPLETE = booleanPreferencesKey("setup_complete")
        val LOCKED_APPS = stringPreferencesKey("locked_apps")
        val NETWORK_RULE = stringPreferencesKey("network_rule")
        val TRUSTED_NETWORKS = stringPreferencesKey("trusted_networks")
        val RELOCK_POLICY = stringPreferencesKey("relock_policy")
        val RELOCK_TIMEOUT_MINUTES = intPreferencesKey("relock_timeout_minutes")
        val LOCK_STYLE = stringPreferencesKey("lock_style")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val DEFAULT_NOTIFICATION_MODE = stringPreferencesKey("default_notification_mode")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val PIN = stringPreferencesKey("pin")
        val RECOVERY_CODE = stringPreferencesKey("recovery_code")
        val RECOVERY_EMAIL = stringPreferencesKey("recovery_email")
        val FAILED_ATTEMPTS = intPreferencesKey("failed_attempts")
        val LOCKOUT_UNTIL = longPreferencesKey("lockout_until")
        val ZONE_EXIT_CONFIRMED = booleanPreferencesKey("zone_exit_confirmed")
    }
}
