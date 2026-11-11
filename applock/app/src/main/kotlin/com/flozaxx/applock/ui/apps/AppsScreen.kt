package com.flozaxx.applock.ui.apps

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.Color
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
import com.flozaxx.applock.protection.protectionStatus
import com.flozaxx.applock.storage.AppSettings
import com.flozaxx.applock.storage.LockedApp
import com.flozaxx.applock.storage.NotificationMode
import com.flozaxx.applock.ui.components.Group
import com.flozaxx.applock.ui.components.GroupItem
import com.flozaxx.applock.ui.components.GroupPosition
import com.flozaxx.applock.ui.components.GroupTitle
import com.flozaxx.applock.ui.components.IconBadge
import com.flozaxx.applock.ui.components.ScreenScaffold
import com.flozaxx.applock.ui.components.SwitchRow
import com.flozaxx.applock.ui.settings.NotificationModePicker
import com.flozaxx.applock.ui.setup.missingProtectionSummary
import com.flozaxx.applock.ui.theme.Spacing
import com.flozaxx.applock.ui.theme.appColors
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
    val lockActive by graph.coordinator.lockActive.collectAsStateWithLifecycle(initialValue = true)
    val apps by produceState<List<InstalledApp>?>(null) { value = context.loadLaunchableApps() }
    var query by rememberSaveable { mutableStateOf("") }
    var detailsFor by rememberSaveable { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf(context.protectionStatus()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { status = context.protectionStatus() }

    val setLocked: (InstalledApp, Boolean) -> Unit = { app, locked ->
        scope.launch {
            graph.store.update { current ->
                val lockedApps =
                    if (locked) current.lockedApps + (app.packageName to LockedApp(current.defaultNotificationMode))
                    else current.lockedApps - app.packageName
                current.copy(lockedApps = lockedApps)
            }
            if (!locked) graph.coordinator.onAppUnlisted(app.packageName)
        }
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
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@ScreenScaffold
        }

        val matching = loaded.filter { it.label.contains(query.trim(), ignoreCase = true) }
        val (locked, others) = matching.partition { it.packageName in settings.lockedApps }
        val (suggested, rest) = others.partition { it.packageName in SELF_PROTECTION_PACKAGES }

        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(bottom = Spacing.xl)) {
            item(key = "status") {
                StatusCard(
                    missing = missingProtectionSummary(status),
                    lockActive = lockActive,
                    lockedCount = settings.lockedApps.size,
                    onClick = onOpenProtection,
                )
            }
            item(key = "search") { SearchField(query, onQueryChange = { query = it }) }

            appGroup("locked", { stringResource(R.string.apps_locked, locked.size) }, locked) { app ->
                AppRow(
                    app = app,
                    supporting = notificationModeLabel(settings.lockedApps.getValue(app.packageName).notificationMode),
                    locked = true,
                    onLockedChange = { setLocked(app, it) },
                    onClick = { detailsFor = app.packageName },
                )
            }
            appGroup("suggested", { stringResource(R.string.apps_suggested) }, suggested) { app ->
                AppRow(
                    app = app,
                    supporting = stringResource(R.string.apps_suggested_hint),
                    locked = false,
                    onLockedChange = { setLocked(app, it) },
                    onClick = { setLocked(app, true) },
                )
            }
            appGroup("rest", { stringResource(R.string.apps_all) }, rest) { app ->
                AppRow(app, supporting = null, locked = false, onLockedChange = { setLocked(app, it) }, onClick = { setLocked(app, true) })
            }

            if (matching.isEmpty()) {
                item(key = "empty") {
                    Text(
                        if (query.isBlank()) stringResource(R.string.apps_none)
                        else stringResource(R.string.apps_no_match, query.trim()),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Spacing.page + Spacing.lg, vertical = Spacing.xl),
                    )
                }
            }
        }
    }

    val details = detailsFor?.let { packageName -> apps?.firstOrNull { it.packageName == packageName } }
    val lockedApp = details?.let { settings.lockedApps[it.packageName] }
    if (details != null && lockedApp != null) {
        val updateApp: ((LockedApp) -> LockedApp) -> Unit = { transform ->
            scope.launch {
                graph.store.update { current ->
                    val existing = current.lockedApps[details.packageName] ?: return@update current
                    current.copy(lockedApps = current.lockedApps + (details.packageName to transform(existing)))
                }
            }
        }
        ModalBottomSheet(onDismissRequest = { detailsFor = null }, containerColor = MaterialTheme.appColors.page) {
            Row(
                Modifier.padding(horizontal = Spacing.page + Spacing.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIcon(details.packageName, 44.dp)
                Spacer(Modifier.width(Spacing.lg))
                Text(details.label, style = MaterialTheme.typography.titleLarge)
            }
            Group(title = stringResource(R.string.app_details_notifications)) {
                NotificationModePicker(lockedApp.notificationMode) { mode -> updateApp { it.copy(notificationMode = mode) } }
            }
            Group(title = stringResource(R.string.app_details_unlock)) {
                SwitchRow(
                    title = stringResource(R.string.app_details_biometric),
                    supporting = stringResource(
                        if (settings.biometricEnabled) R.string.app_details_biometric_hint else R.string.app_details_biometric_off,
                    ),
                    checked = lockedApp.biometric && settings.biometricEnabled,
                    enabled = settings.biometricEnabled,
                    onCheckedChange = { allowed -> updateApp { it.copy(biometric = allowed) } },
                )
            }
            Spacer(Modifier.height(Spacing.xl))
        }
    }
}

private fun LazyListScope.appGroup(
    key: String,
    title: @Composable () -> String,
    apps: List<InstalledApp>,
    row: @Composable (InstalledApp) -> Unit,
) {
    if (apps.isEmpty()) return
    item(key = "$key-title") { GroupTitle(title()) }
    itemsIndexed(apps, key = { _, app -> app.packageName }) { index, app ->
        GroupItem(GroupPosition.of(index, apps.size)) { row(app) }
    }
}

/** What state the lock is in, at the top of the list. One card, three looks. */
@Composable
private fun StatusCard(missing: String?, lockActive: Boolean, lockedCount: Int, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val container: Color
    val content: Color
    val iconRes: Int
    val title: String
    val body: String
    when {
        missing != null -> {
            container = colors.errorContainer
            content = colors.onErrorContainer
            iconRes = R.drawable.ic_warning
            title = missing
            body = stringResource(R.string.missing_fix)
        }
        lockedCount == 0 -> {
            container = colors.secondaryContainer
            content = colors.onSecondaryContainer
            iconRes = R.drawable.ic_shield
            title = stringResource(R.string.status_none_title)
            body = stringResource(R.string.status_none_body)
        }
        lockActive -> {
            container = colors.primaryContainer
            content = colors.onPrimaryContainer
            iconRes = R.drawable.ic_verified_user
            title = stringResource(R.string.status_active_title)
            body = stringResource(R.string.status_locked_count, lockedCount)
        }
        else -> {
            container = colors.secondaryContainer
            content = colors.onSecondaryContainer
            iconRes = R.drawable.ic_shield
            title = stringResource(R.string.status_inactive_title)
            body = stringResource(R.string.status_inactive_body)
        }
    }
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = container,
        contentColor = content,
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.page, vertical = Spacing.sm),
    ) {
        Row(Modifier.padding(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(iconRes, size = 44.dp, container = content.copy(alpha = 0.12f), tint = content)
            Spacer(Modifier.width(Spacing.lg))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(body, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(R.string.apps_search)) },
        leadingIcon = { Icon(painterResource(R.drawable.ic_search), null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(painterResource(R.drawable.ic_close), stringResource(R.string.apps_search_clear))
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.extraLarge,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.appColors.card,
            unfocusedContainerColor = MaterialTheme.appColors.card,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.page, vertical = Spacing.sm),
    )
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
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(app.packageName, 40.dp)
        Spacer(Modifier.width(Spacing.lg))
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
        Spacer(Modifier.width(Spacing.md))
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
