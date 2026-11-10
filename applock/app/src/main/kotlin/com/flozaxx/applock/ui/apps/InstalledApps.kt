package com.flozaxx.applock.ui.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale

data class InstalledApp(val packageName: String, val label: String)

/** Every app with a launcher icon except this one, sorted the way a Turkish reader expects. */
suspend fun Context.loadLaunchableApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
    val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val collator = Collator.getInstance(Locale.forLanguageTag("tr"))
    packageManager.queryIntentActivities(launcher, 0)
        .map { it.activityInfo.applicationInfo }
        .distinctBy { it.packageName }
        .filter { it.packageName != packageName }
        .map { InstalledApp(it.packageName, packageManager.getApplicationLabel(it).toString()) }
        .sortedWith(compareBy(collator) { it.label })
}

private const val ICON_PIXELS = 96
private val iconCache = LruCache<String, ImageBitmap>(200)

@Composable
fun AppIcon(packageName: String, size: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val icon by produceState(iconCache[packageName], packageName) {
        if (value == null) {
            value = withContext(Dispatchers.IO) {
                try {
                    context.packageManager.getApplicationIcon(packageName).toBitmap(ICON_PIXELS, ICON_PIXELS).asImageBitmap()
                } catch (_: PackageManager.NameNotFoundException) {
                    null
                }
            }?.also { iconCache.put(packageName, it) }
        }
    }
    val loaded = icon
    if (loaded != null) {
        Image(loaded, contentDescription = null, modifier = modifier.size(size))
    } else {
        Box(modifier.size(size))
    }
}
