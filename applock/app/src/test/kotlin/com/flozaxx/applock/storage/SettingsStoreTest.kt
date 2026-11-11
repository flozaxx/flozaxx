package com.flozaxx.applock.storage

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.flozaxx.applock.rules.NetworkRule
import com.flozaxx.applock.rules.RelockPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SettingsStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    // Real dispatchers: the store reads its file with a blocking call that a paused test scheduler would never serve.
    private val scope = CoroutineScope(Dispatchers.Default + Job())

    @After
    fun tearDown() = scope.cancel()

    @Test
    fun `settings survive a new store on the same file`() = runBlocking {
        val file = File(folder.root, "settings.preferences_pb")
        val saved = AppSettings(
            setupComplete = true,
            lockedApps = mapOf(
                "com.example.chat" to LockedApp(NotificationMode.HIDE_COMPLETELY, biometric = false),
                "com.example.gallery" to LockedApp(NotificationMode.UNTOUCHED),
            ),
            networkRule = NetworkRule.OFF_TRUSTED_NETWORK,
            trustedNetworks = listOf(TrustedNetwork("aa:bb:cc:dd:ee:ff", "Ev")),
            relockPolicy = RelockPolicy.AFTER_TIMEOUT,
            relockTimeoutMinutes = 15,
            lockStyleId = "pixel",
            themeMode = ThemeMode.DARK,
            dynamicColor = false,
            defaultNotificationMode = NotificationMode.UNTOUCHED,
            biometricEnabled = false,
            pin = Credential("salt", "hash", 6),
            recoveryCode = Credential("salt2", "hash2", 12),
            recoveryEmail = "kisi@example.com",
            failedAttempts = 3,
            lockoutUntilMillis = 42,
            zoneExitConfirmed = true,
        )

        val dataStore = PreferenceDataStoreFactory.create(scope = scope) { file }
        SettingsStore(dataStore, scope).update { saved }
        val reopened = SettingsStore(dataStore, scope)

        assertEquals(saved, reopened.settings.value)
    }

    @Test
    fun `fresh install starts unconfigured and locks by network rule`() = runBlocking {
        val dataStore = PreferenceDataStoreFactory.create(scope = scope) { File(folder.root, "fresh.preferences_pb") }
        val settings = SettingsStore(dataStore, scope).settings.value

        assertFalse(settings.setupComplete)
        assertEquals(null, settings.pin)
        assertEquals(NetworkRule.ON_TRUSTED_NETWORK, settings.networkRule)
        assertTrue(settings.lockedApps.isEmpty())
    }

    @Test
    fun `biometric needs both the global switch and the per app switch`() {
        val settings = AppSettings(
            lockedApps = mapOf(
                "a" to LockedApp(NotificationMode.UNTOUCHED, biometric = true),
                "b" to LockedApp(NotificationMode.UNTOUCHED, biometric = false),
            ),
        )
        assertTrue(settings.biometricAllowedFor("a"))
        assertFalse(settings.biometricAllowedFor("b"))
        assertFalse(settings.copy(biometricEnabled = false).biometricAllowedFor("a"))
    }

    @Test
    fun `locking settings also locks the settings pages that live in other packages`() {
        val chatOnly = AppSettings(lockedApps = mapOf("com.example.chat" to LockedApp(NotificationMode.UNTOUCHED)))
        assertFalse("com.samsung.accessibility" in chatOnly.ruleSettings.lockedPackages)

        val withSettings = chatOnly.copy(
            lockedApps = chatOnly.lockedApps + (AppSettings.SETTINGS_PACKAGE to LockedApp(NotificationMode.UNTOUCHED, biometric = false)),
        )
        assertTrue("com.samsung.accessibility" in withSettings.ruleSettings.lockedPackages)
        assertFalse(withSettings.biometricAllowedFor("com.samsung.accessibility"))
    }
}
