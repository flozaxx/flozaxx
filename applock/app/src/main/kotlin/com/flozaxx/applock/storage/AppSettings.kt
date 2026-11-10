package com.flozaxx.applock.storage

import com.flozaxx.applock.rules.NetworkRule
import com.flozaxx.applock.rules.RelockPolicy
import com.flozaxx.applock.rules.RuleSettings
import kotlinx.serialization.Serializable

enum class NotificationMode { UNTOUCHED, HIDE_CONTENT, HIDE_COMPLETELY }

@Serializable
data class LockedApp(
    val notificationMode: NotificationMode,
    val biometric: Boolean = true,
)

@Serializable
data class TrustedNetwork(val bssid: String, val ssid: String)

@Serializable
data class Credential(val salt: String, val hash: String, val length: Int)

data class AppSettings(
    val setupComplete: Boolean = false,
    val lockedApps: Map<String, LockedApp> = emptyMap(),
    val networkRule: NetworkRule = NetworkRule.ON_TRUSTED_NETWORK,
    val trustedNetworks: List<TrustedNetwork> = emptyList(),
    val relockPolicy: RelockPolicy = RelockPolicy.IMMEDIATELY,
    val relockTimeoutMinutes: Int = 5,
    val lockStyleId: String = "",
    val defaultNotificationMode: NotificationMode = NotificationMode.HIDE_CONTENT,
    val biometricEnabled: Boolean = true,
    val pin: Credential? = null,
    val recoveryCode: Credential? = null,
    val recoveryEmail: String = "",
    val failedAttempts: Int = 0,
    val lockoutUntilMillis: Long = 0,
    val zoneExitConfirmed: Boolean = false,
) {
    val ruleSettings: RuleSettings
        get() = RuleSettings(
            lockedPackages = lockedApps.keys,
            networkRule = networkRule,
            hasTrustedNetworks = trustedNetworks.isNotEmpty(),
            relockPolicy = relockPolicy,
            relockTimeoutMillis = relockTimeoutMinutes * 60_000L,
        )

    fun biometricAllowedFor(packageName: String): Boolean =
        biometricEnabled && lockedApps[packageName]?.biometric == true
}
