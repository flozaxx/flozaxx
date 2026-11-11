package com.flozaxx.applock.ui

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.ResolveInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.flozaxx.applock.graph
import com.flozaxx.applock.lock.LockScreenModel
import com.flozaxx.applock.lock.LockScreenStyles
import com.flozaxx.applock.lock.LockStatus
import com.flozaxx.applock.lock.LockTarget
import com.flozaxx.applock.lock.LockUiState
import com.flozaxx.applock.storage.AppSettings
import com.flozaxx.applock.storage.LockedApp
import com.flozaxx.applock.storage.NotificationMode
import com.flozaxx.applock.ui.apps.AppsScreen
import com.flozaxx.applock.ui.settings.LimitsScreen
import com.flozaxx.applock.ui.settings.SettingsScreen
import com.flozaxx.applock.ui.settings.StylePickerScreen
import com.flozaxx.applock.ui.setup.CredentialSetup
import com.flozaxx.applock.ui.setup.CredentialSetupMode
import com.flozaxx.applock.ui.setup.ProtectionScreen
import com.flozaxx.applock.ui.setup.WelcomeScreen
import com.flozaxx.applock.ui.theme.AppTheme
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the screens off-device. The images land in build/outputs/roborazzi and are meant for a
 * human eye; the test itself only fails when a screen cannot be composed.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h780dp-xxhdpi")
class ScreenshotTest {

    private val target = LockTarget("Mesajlar", icon = null)

    /** A few launchable apps and two locked ones, so the app list has something to show. */
    @Before
    fun installSampleApps() {
        val context = RuntimeEnvironment.getApplication()
        val packages = shadowOf(context.packageManager)
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val samples = listOf(
            "com.example.messages" to "Mesajlar",
            "com.example.gallery" to "Galeri",
            AppSettings.SETTINGS_PACKAGE to "Ayarlar",
            "com.example.calculator" to "Hesap Makinesi",
            "com.example.weather" to "Hava Durumu",
            "com.example.notes" to "Notlar",
        )
        samples.forEach { (name, label) ->
            val app = ApplicationInfo().apply {
                packageName = name
                nonLocalizedLabel = label
            }
            packages.installPackage(
                PackageInfo().apply {
                    packageName = name
                    applicationInfo = app
                },
            )
            val entry = ResolveInfo().apply {
                activityInfo = ActivityInfo().apply {
                    packageName = name
                    this.name = "$name.Main"
                    applicationInfo = app
                }
            }
            @Suppress("DEPRECATION")
            packages.addResolveInfoForIntent(launcher, entry)
        }
        runBlocking {
            context.graph.store.update {
                it.copy(
                    lockedApps = mapOf(
                        "com.example.messages" to LockedApp(NotificationMode.HIDE_CONTENT),
                        "com.example.gallery" to LockedApp(NotificationMode.UNTOUCHED),
                    ),
                )
            }
        }
    }

    private fun model(state: LockUiState, withBiometric: Boolean = true) =
        LockScreenModel(state, target, {}, {}, ({}).takeIf { withBiometric }, {}, {})

    @Test
    fun lockStyles() {
        LockScreenStyles.all.forEach { style ->
            captureRoboImage("build/outputs/roborazzi/lock_${style.id}.png") {
                AppTheme(darkTheme = true, dynamicColor = false) {
                    style.Content(model(LockUiState(pinLength = 4, enteredCount = 2)), Modifier)
                }
            }
        }
    }

    @Test
    fun lockStylesLockedOut() {
        LockScreenStyles.all.forEach { style ->
            captureRoboImage("build/outputs/roborazzi/lock_${style.id}_lockout.png") {
                AppTheme(darkTheme = true, dynamicColor = false) {
                    val state = LockUiState(pinLength = 6, status = LockStatus.LOCKED_OUT, lockoutSecondsLeft = 28)
                    style.Content(model(state, withBiometric = false), Modifier)
                }
            }
        }
    }

    @Test
    fun appScreens() {
        val screens = mapOf<String, @Composable () -> Unit>(
            "welcome" to { WelcomeScreen(onStart = {}) },
            "pin_create" to { CredentialSetup(CredentialSetupMode.FIRST_TIME, onFinished = {}, onBack = null) },
            "setup" to { ProtectionScreen(setupMode = true, onDone = {}, onBack = null) },
            "apps" to { AppsScreen(onOpenSettings = {}, onOpenProtection = {}) },
            "settings" to { SettingsScreen({}, {}, {}, {}, {}, {}) },
            "style_picker" to { StylePickerScreen(onBack = {}) },
            "limits" to { LimitsScreen(onBack = {}) },
        )
        screens.forEach { (name, screen) ->
            listOf(false to "light", true to "dark").forEach { (dark, suffix) ->
                captureRoboImage("build/outputs/roborazzi/screen_${name}_$suffix.png") {
                    // The app's own palette: wallpaper colours do not exist off-device.
                    AppTheme(darkTheme = dark, dynamicColor = false) { screen() }
                }
            }
        }
    }
}
