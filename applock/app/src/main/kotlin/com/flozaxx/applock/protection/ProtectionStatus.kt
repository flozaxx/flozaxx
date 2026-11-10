package com.flozaxx.applock.protection

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.flozaxx.applock.detection.AppLockAccessibilityService
import com.flozaxx.applock.notifications.NotificationGuardService

/** Which of the permissions the lock depends on are currently granted. */
data class ProtectionStatus(
    val accessibility: Boolean,
    val overlay: Boolean,
    val fineLocation: Boolean,
    val backgroundLocation: Boolean,
    val locationEnabled: Boolean,
    val batteryExempt: Boolean,
    val deviceAdmin: Boolean,
    val notificationAccess: Boolean,
    val postNotifications: Boolean,
) {
    val location: Boolean get() = fineLocation && backgroundLocation && locationEnabled
    val notifications: Boolean get() = notificationAccess && postNotifications

    /** Without these the app cannot lock anything or can be removed in two taps. */
    val essentialsGranted: Boolean get() = accessibility && deviceAdmin && batteryExempt
}

fun Context.protectionStatus(): ProtectionStatus = ProtectionStatus(
    accessibility = isAccessibilityServiceEnabled(),
    overlay = Settings.canDrawOverlays(this),
    fineLocation = isGranted(Manifest.permission.ACCESS_FINE_LOCATION),
    backgroundLocation = isGranted(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
    locationEnabled = getSystemService(LocationManager::class.java).isLocationEnabled,
    batteryExempt = getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName),
    deviceAdmin = getSystemService(DevicePolicyManager::class.java).isAdminActive(adminComponent()),
    notificationAccess = packageName in NotificationManagerCompat.getEnabledListenerPackages(this),
    postNotifications = isGranted(Manifest.permission.POST_NOTIFICATIONS),
)

private fun Context.isGranted(permission: String): Boolean =
    checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

private fun Context.isAccessibilityServiceEnabled(): Boolean {
    val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
    val ours = ComponentName(this, AppLockAccessibilityService::class.java)
    return enabled.split(':').any { ComponentName.unflattenFromString(it) == ours }
}

fun Context.adminComponent() = ComponentName(this, AdminReceiver::class.java)

private fun Context.packageUri(): Uri = Uri.fromParts("package", packageName, null)

object ProtectionIntents {
    fun appInfo(context: Context) =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, context.packageUri())

    fun accessibility() = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)

    fun overlay(context: Context) =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, context.packageUri())

    fun locationSwitch() = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)

    fun batteryExemption(context: Context) =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, context.packageUri())

    fun deviceAdmin(context: Context, explanation: String) =
        Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, context.adminComponent())
            .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, explanation)

    fun notificationAccess(context: Context) =
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
            .putExtra(
                Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                ComponentName(context, NotificationGuardService::class.java).flattenToString(),
            )
}
