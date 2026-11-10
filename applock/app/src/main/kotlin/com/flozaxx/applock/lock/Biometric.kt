package com.flozaxx.applock.lock

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.flozaxx.applock.R

fun Context.hasEnrolledBiometric(): Boolean =
    BiometricManager.from(this).canAuthenticate(BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS

/** Shows the system biometric sheet. Cancelling or failing simply leaves the PIN pad in place. */
fun FragmentActivity.showBiometricPrompt(title: String, onSuccess: () -> Unit) {
    val callback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
    }
    val prompt = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle(getString(R.string.biometric_subtitle))
        .setNegativeButtonText(getString(R.string.biometric_use_pin))
        .setAllowedAuthenticators(BIOMETRIC_WEAK)
        .build()
    BiometricPrompt(this, ContextCompat.getMainExecutor(this), callback).authenticate(prompt)
}
