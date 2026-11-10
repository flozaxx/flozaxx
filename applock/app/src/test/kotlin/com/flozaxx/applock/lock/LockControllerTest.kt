package com.flozaxx.applock.lock

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.flozaxx.applock.storage.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class LockControllerTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun TestScope.controller(store: SettingsStore? = null, onUnlocked: () -> Unit = {}) =
        LockController(backgroundScope, pinLength = 4, verify = { it == "1234" }, store = store, onUnlocked = onUnlocked)

    private fun LockController.type(pin: String) = pin.forEach { onDigit(it.digitToInt()) }

    @Test
    fun `wrong pin clears the entry and reports the error`() = runTest {
        val controller = controller()
        controller.type("1111")
        runCurrent()

        assertEquals(LockStatus.WRONG, controller.state.status)
        assertEquals(1, controller.state.wrongCount)
        assertEquals(0, controller.state.enteredCount)
    }

    @Test
    fun `correct pin unlocks after the closing animation`() = runTest {
        var unlocked = false
        val controller = controller(onUnlocked = { unlocked = true })
        controller.type("1234")
        runCurrent()

        assertEquals(LockStatus.UNLOCKED, controller.state.status)
        assertFalse(unlocked)
        advanceTimeBy(LockController.UNLOCK_ANIMATION_MILLIS + 1)
        assertTrue(unlocked)
    }

    @Test
    fun `backspace removes the last digit`() = runTest {
        val controller = controller()
        controller.type("12")
        controller.onBackspace()
        assertEquals(1, controller.state.enteredCount)
    }

    @Test
    fun `fifth wrong pin blocks further entry and is remembered`() = runBlocking {
        // Real dispatchers: the store reads its file with a blocking call that a paused test scheduler would never serve.
        val scope = CoroutineScope(Dispatchers.Default + Job())
        val file = File(folder.root, "settings.preferences_pb")
        val store = SettingsStore(PreferenceDataStoreFactory.create(scope = scope) { file }, scope)
        val controller = LockController(scope, pinLength = 4, verify = { it == "1234" }, store = store, onUnlocked = {})

        repeat(5) { attempt ->
            controller.type("0000")
            withTimeout(5_000) {
                while (controller.state.wrongCount <= attempt) delay(5)
            }
        }
        withTimeout(5_000) {
            while (controller.state.status != LockStatus.LOCKED_OUT) delay(5)
        }

        assertTrue(controller.state.lockoutSecondsLeft in 1..30)
        assertEquals(5, store.settings.first { it.failedAttempts == 5 }.failedAttempts)

        controller.type("1234")
        delay(50)
        assertEquals("digits are ignored while blocked", LockStatus.LOCKED_OUT, controller.state.status)
        scope.cancel()
    }
}
