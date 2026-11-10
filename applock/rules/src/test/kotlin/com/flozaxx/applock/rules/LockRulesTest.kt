package com.flozaxx.applock.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockRulesTest {

    private val chat = "com.example.chat"
    private val launcher = "com.example.launcher"

    private fun settings(
        rule: NetworkRule = NetworkRule.ALWAYS,
        policy: RelockPolicy = RelockPolicy.IMMEDIATELY,
        hasTrustedNetworks: Boolean = true,
        timeout: Long = 60_000,
    ) = RuleSettings(setOf(chat), rule, hasTrustedNetworks, policy, timeout)

    private fun shouldLock(
        settings: RuleSettings,
        network: NetworkState = NetworkState.TRUSTED,
        exitConfirmed: Boolean = false,
        session: LockSession = LockSession(),
        now: Long = 0,
        packageName: String = chat,
    ) = LockRules.shouldLock(packageName, now, settings, network, exitConfirmed, session)

    @Test
    fun `app outside the list is never locked`() {
        assertFalse(shouldLock(settings(), packageName = launcher))
    }

    @Test
    fun `always rule locks on every network`() {
        NetworkState.entries.forEach { network ->
            assertTrue(shouldLock(settings(), network, exitConfirmed = true))
        }
    }

    @Test
    fun `trusted network rule locks at home and when the network cannot be read`() {
        val rule = settings(NetworkRule.ON_TRUSTED_NETWORK)
        assertTrue(shouldLock(rule, NetworkState.TRUSTED, exitConfirmed = true))
        assertTrue(shouldLock(rule, NetworkState.UNKNOWN, exitConfirmed = true))
    }

    @Test
    fun `leaving the trusted network keeps the lock until one authentication`() {
        val rule = settings(NetworkRule.ON_TRUSTED_NETWORK)
        assertTrue(shouldLock(rule, NetworkState.UNTRUSTED, exitConfirmed = false))
        assertFalse(shouldLock(rule, NetworkState.UNTRUSTED, exitConfirmed = true))
    }

    @Test
    fun `inverse rule locks everywhere except the trusted network`() {
        val rule = settings(NetworkRule.OFF_TRUSTED_NETWORK)
        assertTrue(shouldLock(rule, NetworkState.UNTRUSTED, exitConfirmed = true))
        assertTrue(shouldLock(rule, NetworkState.UNKNOWN, exitConfirmed = true))
        assertTrue(shouldLock(rule, NetworkState.TRUSTED, exitConfirmed = false))
        assertFalse(shouldLock(rule, NetworkState.TRUSTED, exitConfirmed = true))
    }

    @Test
    fun `network rule without saved networks behaves like always`() {
        val rule = settings(NetworkRule.ON_TRUSTED_NETWORK, hasTrustedNetworks = false)
        assertTrue(shouldLock(rule, NetworkState.UNTRUSTED, exitConfirmed = true))
    }

    @Test
    fun `exit confirmation is granted outside and revoked on return`() {
        assertTrue(LockRules.exitConfirmedAfterAuthentication(LockZone.OUTSIDE, false))
        assertFalse(LockRules.exitConfirmedAfterAuthentication(LockZone.INSIDE, false))
        assertFalse(LockRules.exitConfirmedAfterAuthentication(LockZone.UNCERTAIN, false))
        assertFalse(LockRules.exitConfirmedAfterZoneChange(LockZone.INSIDE, true))
        assertTrue(LockRules.exitConfirmedAfterZoneChange(LockZone.UNCERTAIN, true))
        assertTrue(LockRules.exitConfirmedAfterZoneChange(LockZone.OUTSIDE, true))
    }

    @Test
    fun `immediate policy relocks after leaving the app`() {
        val rule = settings(policy = RelockPolicy.IMMEDIATELY)
        var session = LockSession().onForegroundChanged(chat, 0, rule.relockPolicy)
        assertTrue(shouldLock(rule, session = session))

        session = session.onAuthenticated(chat, 0)
        assertFalse(shouldLock(rule, session = session, now = 10))

        session = session.onForegroundChanged(launcher, 20, rule.relockPolicy)
        assertTrue(shouldLock(rule, session = session, now = 30))
    }

    @Test
    fun `screen off policy keeps the unlock across app switches`() {
        val rule = settings(policy = RelockPolicy.ON_SCREEN_OFF)
        var session = LockSession()
            .onForegroundChanged(chat, 0, rule.relockPolicy)
            .onAuthenticated(chat, 0)
            .onForegroundChanged(launcher, 10, rule.relockPolicy)
        assertFalse(shouldLock(rule, session = session, now = 1_000_000))

        session = session.onScreenOff(1_000_001, rule.relockPolicy)
        assertTrue(shouldLock(rule, session = session, now = 1_000_002))
    }

    @Test
    fun `timeout policy counts from the moment the app was left`() {
        val rule = settings(policy = RelockPolicy.AFTER_TIMEOUT, timeout = 60_000)
        var session = LockSession()
            .onForegroundChanged(chat, 0, rule.relockPolicy)
            .onAuthenticated(chat, 0)

        assertFalse("still inside the app", shouldLock(rule, session = session, now = 500_000))

        session = session.onForegroundChanged(launcher, 500_000, rule.relockPolicy)
        assertFalse(shouldLock(rule, session = session, now = 559_000))
        assertTrue(shouldLock(rule, session = session, now = 561_000))
    }

    @Test
    fun `timeout policy counts from screen off for the app in front`() {
        val rule = settings(policy = RelockPolicy.AFTER_TIMEOUT, timeout = 60_000)
        val session = LockSession()
            .onForegroundChanged(chat, 0, rule.relockPolicy)
            .onAuthenticated(chat, 0)
            .onScreenOff(100_000, rule.relockPolicy)

        assertFalse(shouldLock(rule, session = session, now = 150_000))
        assertTrue(shouldLock(rule, session = session, now = 161_000))
    }

    @Test
    fun `immediate policy drops every unlock on screen off`() {
        val session = LockSession()
            .onForegroundChanged(chat, 0, RelockPolicy.IMMEDIATELY)
            .onAuthenticated(chat, 0)
            .onScreenOff(10, RelockPolicy.IMMEDIATELY)
        assertEquals(LockSession(), session)
    }
}

class AttemptPolicyTest {
    @Test
    fun `first attempts are free then the wait doubles up to the cap`() {
        assertEquals(0, AttemptPolicy.lockoutMillis(4))
        assertEquals(30_000, AttemptPolicy.lockoutMillis(5))
        assertEquals(60_000, AttemptPolicy.lockoutMillis(6))
        assertEquals(900_000, AttemptPolicy.lockoutMillis(10))
        assertEquals(900_000, AttemptPolicy.lockoutMillis(500))
    }
}

class LockSessionTest {
    @Test
    fun `window events during the lock screen do not let the unlock outlive the visit`() {
        val policy = RelockPolicy.IMMEDIATELY
        var session = LockSession()
            .onForegroundChanged("chat", 0, policy)
            // The launcher reports itself once more while the lock screen is already up.
            .onForegroundChanged("launcher", 1, policy)
            .onAuthenticated("chat", 2)
        assertTrue(session.isUnlocked("chat", 3, policy, 0))

        session = session.onForegroundChanged("launcher", 4, policy)
        assertFalse(session.isUnlocked("chat", 5, policy, 0))
    }
}
