package com.flozaxx.applock.ui.apps

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flozaxx.applock.R
import com.flozaxx.applock.graph
import com.flozaxx.applock.protection.ProtectionStatus
import com.flozaxx.applock.protection.protectionStatus
import com.flozaxx.applock.storage.AppSettings
import com.flozaxx.applock.storage.LockedApp
import com.flozaxx.applock.storage.NotificationMode
import com.flozaxx.applock.ui.components.ScreenScaffold
import com.flozaxx.applock.ui.components.SectionTitle
import com.flozaxx.applock.ui.settings.NotificationModePicker
import com.flozaxx.applock.ui.settings.SwitchRow
import com.flozaxx.applock.ui.setup.missingProtectionSummary
import kotlinx.coroutines.launch

/** Locking these keeps the lock itself from being switched off or uninstalled. */
private val SELF_PROTECTION_PACKAGES = setOf(
    AppSettings.SETTINGS_PACKAGE,
    "com.android.vending",
    "com.sec.android.app.samsungapps",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(onOpenSettings: () -> Unit, onOpenProtection: () -> Unit) {
    val context = LocalContext.current
    val graph = context.graph
    val scope = rememberCoroutineScope()
    val settings by graph.store.settings.collectAsStateWithLifecycle()
    val apps by produceState<List<InstalledApp>?>(null) { value = context.loadLaunchableApps() }
    var query by rememberSaveable { mutableStateOf("") }
    var detailsFor by rememberSaveable { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf(context.protectionStatus()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { status = context.protectionStatus() }

    val setLocked = { app: InstalledApp, locked: Boolean ->
        scope.launch {
            graph.store.update { current ->
                val lockedApps =
                    if (locked) current.lockedApps + (app.packageName to LockedApp(current.defaultNotificationMode))
                    else current.lockedApps - app.packageName
                current.copy(lockedApps = lockedApps)
            }
            if (!locked) graph.coordinator.onAppUnlisted(app.packageName)
        }
        Unit
    }

    ScreenScaffold(
        title = stringResource(R.string.app_name),
        onBack = null,
        actions = {
            IconButton(onClick = onOpenSettings) {
                Icon(painterResource(R.drawable.ic_settings), stringResource(R.string.settings_title))
            }
        },
    ) { padding ->
        val loaded = apps
        if (loaded == null) {
            Column(
                Modifier.padding(padding).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(64.dp))
                CircularProgressIndicator()
            }
            return@ScreenScaffold
        }

        val matching = loaded.filter { it.label.contains(query.trim(), ignoreCase = true) }
        val (locked, others) = matching.partition { it.packageName in settings.lockedApps }
        val (suggested, rest) = others.partition { it.packageName in SELF_PROTECTION_PACKAGES }

        LazyColumn(Modifier.padding(padding).fillMaxSize()) {
            missingProtectionItem(status, onOpenProtection)
            item(key = "search") {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(R.string.apps_search)) },
                    leadingIcon = { Icon(painterResource(R.drawable.ic_search), null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(painterResource(R.drawable.ic_close), stringResource(R.string.apps_search_clear))
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            if (locked.isNotEmpty()) {
                item(key = "locked-title") { SectionTitle(stringResource(R.string.apps_locked, locked.size)) }
                items(locked, key = { it.packageName }) { app ->
                    AppRow(
                        app = app,
                        supporting = notificationModeLabel(settings.lockedApps.getValue(app.packageName).notificationMode),
                        locked = true,
                        onLockedChange = { setLocked(app, it) },
                        onClick = { detailsFor = app.packageName },
                    )
                }
            }
            if (suggested.isNotEmpty()) {
                item(key = "suggested-title") { SectionTitle(stringResource(R.string.apps_suggested)) }
                items(suggested, key = { it.packageName }) { app ->
                    AppRow(
                        app = app,
                        supporting = stringResource(R.string.apps_suggested_hint),
                        locked = false,
                        onLockedChange = { setLocked(app, it) },
                        onClick = { setLocked(app, true) },
                    )
                }
            }
            if (rest.isNotEmpty()) {
                item(key = "rest-title") { SectionTitle(stringResource(R.string.apps_all)) }
                items(rest, key = { it.packageName }) { app ->
                    AppRow(app, supporting = null, locked = false, onLockedChange = { setLocked(app, it) }, onClick = { setLocked(app, true) })
                }
            }
            if (matching.isEmpty()) {
                item(key = "empty") {
                    Text(
                        if (query.isBlank()) stringResource(R.string.apps_none)
                        else stringResource(R.string.apps_no_match, query.trim()),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }
            item(key = "bottom") { Spacer(Modifier.height(24.dp)) }
        }
    }

    val details = detailsFor?.let { packageName -> apps?.firstOrNull { it.packageName == packageName } }
    val lockedApp = details?.let { settings.lockedApps[it.packageName] }
    if (details != null && lockedApp != null) {
        val updateApp = { transform: (LockedApp) -> LockedApp ->
            scope.launch {
                graph.store.update { current ->
                    val existing = current.lockedApps[details.packageName] ?: return@update current
                    current.copy(lockedApps = current.lockedApps + (details.packageName to transform(existing)))
                }
            }
            Unit
        }
        ModalBottomSheet(onDismissRequest = { detailsFor = null }) {
            Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                AppIcon(details.packageName, 40.dp)
                Spacer(Modifier.width(16.dp))
                Text(details.label, style = MaterialTheme.typography.titleLarge)
            }
            SectionTitle(stringResource(R.string.app_details_notifications))
            NotificationModePicker(lockedApp.notificationMode) { mode -> updateApp { it.copy(notificationMode = mode) } }
            SectionTitle(stringResource(R.string.app_details_unlock))
            SwitchRow(
                title = stringResource(R.string.app_details_biometric),
                supporting = stringResource(
                    if (settings.biometricEnabled) R.string.app_details_biometric_hint else R.string.app_details_biometric_off,
                ),
                checked = lockedApp.biometric && settings.biometricEnabled,
                enabled = settings.biometricEnabled,
                onCheckedChange = { allowed -> updateApp { it.copy(biometric = allowed) } },
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun LazyListScope.missingProtectionItem(
    status: ProtectionStatus,
    onOpenProtection: () -> Unit,
) {
    item(key = "protection") {
        val missing = missingProtectionSummary(status) ?: return@item
        Card(
            onClick = onOpenProtection,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_warning), null, tint = MaterialTheme.colorScheme.onErrorContainer)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(missing, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                    Text(
                        stringResource(R.string.missing_fix),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun AppRow(
    app: InstalledApp,
    supporting: String?,
    locked: Boolean,
    onLockedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(app.packageName, 40.dp)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(app.label, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (supporting != null) {
                Text(
                    supporting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = locked, onCheckedChange = onLockedChange)
    }
}

@Composable
private fun notificationModeLabel(mode: NotificationMode): String = stringResource(
    when (mode) {
        NotificationMode.UNTOUCHED -> R.string.notification_mode_untouched
        NotificationMode.HIDE_CONTENT -> R.string.notification_mode_hide_content
        NotificationMode.HIDE_COMPLETELY -> R.string.notification_mode_hide_completely
    },
)
