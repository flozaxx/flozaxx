package com.flozaxx.applock.rules

/**
 * Which apps the user has already authenticated for.
 *
 * [unlockedAt] maps a package to the last moment it was known to be in use; the timeout policy
 * counts from there, so time spent inside the app never expires the unlock.
 */
data class LockSession(
    val unlockedAt: Map<String, Long> = emptyMap(),
    val foregroundPackage: String? = null,
) {

    fun isUnlocked(packageName: String, nowMillis: Long, policy: RelockPolicy, timeoutMillis: Long): Boolean {
        val lastActive = unlockedAt[packageName] ?: return false
        return when (policy) {
            RelockPolicy.IMMEDIATELY, RelockPolicy.ON_SCREEN_OFF -> true
            RelockPolicy.AFTER_TIMEOUT ->
                packageName == foregroundPackage || nowMillis - lastActive <= timeoutMillis
        }
    }

    fun onForegroundChanged(packageName: String, nowMillis: Long, policy: RelockPolicy): LockSession {
        val previous = foregroundPackage
        if (previous == packageName) return this
        val unlocked = when {
            previous == null || previous !in unlockedAt -> unlockedAt
            policy == RelockPolicy.IMMEDIATELY -> unlockedAt - previous
            else -> unlockedAt + (previous to nowMillis)
        }
        return LockSession(unlocked, packageName)
    }

    fun onAuthenticated(packageName: String, nowMillis: Long): LockSession =
        copy(unlockedAt = unlockedAt + (packageName to nowMillis))

    fun onScreenOff(nowMillis: Long, policy: RelockPolicy): LockSession = when (policy) {
        RelockPolicy.IMMEDIATELY, RelockPolicy.ON_SCREEN_OFF -> LockSession()
        RelockPolicy.AFTER_TIMEOUT -> {
            val foreground = foregroundPackage
            val unlocked =
                if (foreground != null && foreground in unlockedAt) unlockedAt + (foreground to nowMillis)
                else unlockedAt
            LockSession(unlocked, null)
        }
    }

    fun onPackageRelocked(packageName: String): LockSession = copy(unlockedAt = unlockedAt - packageName)
}
