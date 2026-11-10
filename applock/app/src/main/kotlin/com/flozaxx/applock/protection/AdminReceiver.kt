package com.flozaxx.applock.protection

import android.app.admin.DeviceAdminReceiver

/** Exists only so the app can be a device admin: an active admin cannot be uninstalled. */
class AdminReceiver : DeviceAdminReceiver()
