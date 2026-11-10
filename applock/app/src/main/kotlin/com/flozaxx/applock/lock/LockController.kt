package com.flozaxx.applock.lock

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.flozaxx.applock.rules.AttemptPolicy
import com.flozaxx.applock.storage.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class LockStatus { ENTERING, CHECKING, WRONG, LOCKED_OUT, UNLOCKED }

data class LockUiState(
    val pinLength: Int,
    val enteredCount: Int = 0,
    val status: LockStatus = LockStatus.ENTERING,
    /** Grows with every wrong PIN; styles key their shake animation on it. */
    val wrongCount: Int = 0,
    val lockoutSecondsLeft: Int = 0,
)

/**
 * PIN entry logic shared by every lock screen style: collecting digits, verifying, counting wrong
 * attempts and enforcing the wait after too many of them.
 *
 * @param store where wrong attempts are persisted; null for the settings preview, which never locks out.
 */
class LockController(
    private val scope: CoroutineScope,
    pinLength: Int,
    private val verify: suspend (String) -> Boolean,
    private val store: SettingsStore?,
    private val onUnlocked: () -> Unit,
) {
    var state by mutableStateOf(LockUiState(pinLength))
        private set

    private val digits = StringBuilder()

    init {
        val lockoutUntil = store?.settings?.value?.lockoutUntilMillis ?: 0
        if (lockoutUntil > System.currentTimeMillis()) countDownLockout(lockoutUntil)
    }

    fun onDigit(digit: Int) {
        if (state.status != LockStatus.ENTERING && state.status != LockStatus.WRONG) return
        digits.append(digit)
        state = state.copy(enteredCount = digits.length, status = LockStatus.ENTERING)
        if (digits.length == state.pinLength) submit()
    }

    fun onBackspace() {
        if (state.status != LockStatus.ENTERING && state.status != LockStatus.WRONG) return
        if (digits.isNotEmpty()) digits.deleteCharAt(digits.length - 1)
        state = state.copy(enteredCount = digits.length, status = LockStatus.ENTERING)
    }

    /** Biometric success: same outcome as a correct PIN. */
    fun unlockWithoutPin() {
        if (state.status == LockStatus.UNLOCKED) return
        scope.launch {
            store?.update { it.copy(failedAttempts = 0, lockoutUntilMillis = 0) }
            finishUnlocked()
        }
    }

    private fun submit() {
        val pin = digits.toString()
        digits.clear()
        state = state.copy(status = LockStatus.CHECKING)
        scope.launch {
            if (verify(pin)) {
                store?.update { it.copy(failedAttempts = 0, lockoutUntilMillis = 0) }
                finishUnlocked()
            } else {
                rejectPin()
            }
        }
    }

    private suspend fun rejectPin() {
        var lockoutUntil = 0L
        store?.update { settings ->
            val failed = settings.failedAttempts + 1
            val wait = AttemptPolicy.lockoutMillis(failed)
            lockoutUntil = if (wait > 0) System.currentTimeMillis() + wait else 0
            settings.copy(failedAttempts = failed, lockoutUntilMillis = lockoutUntil)
        }
        state = state.copy(enteredCount = 0, status = LockStatus.WRONG, wrongCount = state.wrongCount + 1)
        if (lockoutUntil > 0) countDownLockout(lockoutUntil)
    }

    private fun countDownLockout(untilMillis: Long) {
        scope.launch {
            while (true) {
                val left = untilMillis - System.currentTimeMillis()
                if (left <= 0 || state.status == LockStatus.UNLOCKED) break
                state = state.copy(
                    enteredCount = 0,
                    status = LockStatus.LOCKED_OUT,
                    lockoutSecondsLeft = ((left + 999) / 1000).toInt(),
                )
                delay(250)
            }
            if (state.status == LockStatus.LOCKED_OUT) state = state.copy(status = LockStatus.ENTERING)
        }
    }

    private suspend fun finishUnlocked() {
        state = state.copy(status = LockStatus.UNLOCKED)
        delay(UNLOCK_ANIMATION_MILLIS)
        onUnlocked()
    }

    companion object {
        /** Time the styles get to play their "correct PIN" animation before the screen closes. */
        const val UNLOCK_ANIMATION_MILLIS = 180L
    }
}
