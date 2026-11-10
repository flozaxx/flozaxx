package com.flozaxx.applock.rules

enum class NetworkRule { ALWAYS, ON_TRUSTED_NETWORK, OFF_TRUSTED_NETWORK }

enum class NetworkState { TRUSTED, UNTRUSTED, UNKNOWN }

enum class RelockPolicy { IMMEDIATELY, ON_SCREEN_OFF, AFTER_TIMEOUT }

/** Where the device stands relative to the area in which the lock must be enforced. */
enum class LockZone { INSIDE, OUTSIDE, UNCERTAIN }

data class RuleSettings(
    val lockedPackages: Set<String>,
    val networkRule: NetworkRule,
    val hasTrustedNetworks: Boolean,
    val relockPolicy: RelockPolicy,
    val relockTimeoutMillis: Long,
)

object LockRules {

    fun zone(rule: NetworkRule, network: NetworkState, hasTrustedNetworks: Boolean): LockZone {
        // Security: a network rule without any saved network would unlock everything after one PIN entry.
        if (rule == NetworkRule.ALWAYS || !hasTrustedNetworks) return LockZone.INSIDE
        return when (network) {
            NetworkState.UNKNOWN -> LockZone.UNCERTAIN
            NetworkState.TRUSTED ->
                if (rule == NetworkRule.ON_TRUSTED_NETWORK) LockZone.INSIDE else LockZone.OUTSIDE
            NetworkState.UNTRUSTED ->
                if (rule == NetworkRule.ON_TRUSTED_NETWORK) LockZone.OUTSIDE else LockZone.INSIDE
        }
    }

    /**
     * Leaving the zone only lifts the lock after one successful authentication, so switching
     * Wi-Fi off at home is not enough to open the locked apps.
     */
    fun isLockActive(zone: LockZone, exitConfirmed: Boolean): Boolean =
        zone != LockZone.OUTSIDE || !exitConfirmed

    fun exitConfirmedAfterZoneChange(zone: LockZone, exitConfirmed: Boolean): Boolean =
        if (zone == LockZone.INSIDE) false else exitConfirmed

    fun exitConfirmedAfterAuthentication(zone: LockZone, exitConfirmed: Boolean): Boolean =
        zone == LockZone.OUTSIDE || exitConfirmed

    fun shouldLock(
        packageName: String,
        nowMillis: Long,
        settings: RuleSettings,
        network: NetworkState,
        exitConfirmed: Boolean,
        session: LockSession,
    ): Boolean {
        if (packageName !in settings.lockedPackages) return false
        val zone = zone(settings.networkRule, network, settings.hasTrustedNetworks)
        if (!isLockActive(zone, exitConfirmed)) return false
        return !session.isUnlocked(packageName, nowMillis, settings.relockPolicy, settings.relockTimeoutMillis)
    }
}
