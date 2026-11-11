package com.flozaxx.applock.ui.setup

import android.Manifest
import android.app.Activity
import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flozaxx.applock.R
import com.flozaxx.applock.graph
import com.flozaxx.applock.protection.ProtectionIntents
import com.flozaxx.applock.protection.ProtectionStatus
import com.flozaxx.applock.protection.protectionStatus
import com.flozaxx.applock.ui.components.Group
import com.flozaxx.applock.ui.components.GroupHint
import com.flozaxx.applock.ui.components.PrimaryButton
import com.flozaxx.applock.ui.components.ScreenScaffold
import com.flozaxx.applock.ui.settings.TrustedNetworkEditor
import com.flozaxx.applock.ui.theme.Spacing
import com.flozaxx.applock.ui.theme.appColors
import kotlinx.coroutines.launch

/**
 * The permission checklist. During first-time setup it ends with a "finish" button; later the
 * same list is reachable from settings to see what is missing.
 */
@Composable
fun ProtectionScreen(setupMode: Boolean, onDone: () -> Unit, onBack: (() -> Unit)?) {
    val context = LocalContext.current
    val activity = LocalActivity.current as Activity
    val graph = context.graph
    val scope = rememberCoroutineScope()
    val settings by graph.store.settings.collectAsStateWithLifecycle()

    var status by remember { mutableStateOf(context.protectionStatus()) }
    val refresh = {
        val updated = context.protectionStatus()
        // The system only reveals the BSSID to a subscription made after location access was granted.
        if (updated.location != status.location) graph.wifi.start()
        status = updated
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh() }

    val open = { intent: Intent -> context.startActivity(intent) }
    val requestPermissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        refresh()
        // After a "don't ask again" the system dialog no longer appears; the app info page is the only way left.
        val denied = results.filterValues { granted -> !granted }.keys
        if (denied.isNotEmpty() && denied.none(activity::shouldShowRequestPermissionRationale)) {
            open(ProtectionIntents.appInfo(context))
        }
    }
    var restrictedSettingsDone by rememberSaveable { mutableStateOf(false) }
    val adminExplanation = stringResource(R.string.step_admin_body)
    val required = listOf(status.accessibility, status.location, status.batteryExempt, status.deviceAdmin, status.notifications)

    ScreenScaffold(
        title = stringResource(if (setupMode) R.string.setup_title else R.string.protection_title),
        onBack = onBack,
        bottomBar = {
            if (setupMode) {
                Column(Modifier.navigationBarsPadding().padding(horizontal = Spacing.page, vertical = Spacing.md)) {
                    if (!status.accessibility) {
                        Text(
                            stringResource(R.string.setup_finish_blocked),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.sm),
                        )
                    }
                    PrimaryButton(
                        text = stringResource(R.string.setup_finish),
                        enabled = status.accessibility,
                        onClick = {
                            scope.launch { graph.store.update { it.copy(setupComplete = true) } }
                            onDone()
                        },
                    )
                }
            }
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Progress(done = required.count { it }, total = required.size, showIntro = setupMode)

            if (!status.accessibility) {
                Step(
                    title = stringResource(R.string.step_restricted_title),
                    body = stringResource(R.string.step_restricted_body),
                    done = restrictedSettingsDone,
                    actionLabel = stringResource(R.string.step_restricted_action),
                    onAction = { open(ProtectionIntents.appInfo(context)) },
                    secondaryLabel = stringResource(R.string.step_restricted_confirm),
                    onSecondary = { restrictedSettingsDone = true },
                )
            }
            Step(
                title = stringResource(R.string.step_accessibility_title),
                body = stringResource(R.string.step_accessibility_body),
                done = status.accessibility,
                actionLabel = stringResource(R.string.step_open_settings),
                onAction = { open(ProtectionIntents.accessibility()) },
            )
            Step(
                title = stringResource(R.string.step_location_title),
                body = stringResource(R.string.step_location_body),
                done = status.location,
                actionLabel = stringResource(
                    when {
                        !status.fineLocation -> R.string.step_location_grant
                        !status.backgroundLocation -> R.string.step_location_always
                        else -> R.string.step_location_switch
                    },
                ),
                onAction = {
                    when {
                        !status.fineLocation -> requestPermissions.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                        )
                        !status.backgroundLocation ->
                            requestPermissions.launch(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
                        else -> open(ProtectionIntents.locationSwitch())
                    }
                },
            )
            Step(
                title = stringResource(R.string.step_battery_title),
                body = stringResource(R.string.step_battery_body),
                done = status.batteryExempt,
                actionLabel = stringResource(R.string.step_battery_action),
                onAction = { open(ProtectionIntents.batteryExemption(context)) },
            )
            Step(
                title = stringResource(R.string.step_admin_title),
                body = adminExplanation,
                done = status.deviceAdmin,
                actionLabel = stringResource(R.string.step_admin_action),
                onAction = { open(ProtectionIntents.deviceAdmin(context, adminExplanation)) },
            )
            Step(
                title = stringResource(R.string.step_notifications_title),
                body = stringResource(R.string.step_notifications_body),
                done = status.notifications,
                actionLabel = stringResource(
                    if (!status.postNotifications) R.string.step_notifications_grant else R.string.step_notifications_access,
                ),
                onAction = {
                    if (!status.postNotifications) {
                        requestPermissions.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                    } else {
                        open(ProtectionIntents.notificationAccess(context))
                    }
                },
            )
            Step(
                title = stringResource(R.string.step_overlay_title),
                body = stringResource(R.string.step_overlay_body),
                done = status.overlay,
                actionLabel = stringResource(R.string.step_open_settings),
                onAction = { open(ProtectionIntents.overlay(context)) },
            )

            if (setupMode) {
                Column {
                    Group(title = stringResource(R.string.trusted_networks_title)) { TrustedNetworkEditor(settings) }
                    GroupHint(stringResource(R.string.setup_network_body))
                }
            }
            Spacer(Modifier.height(Spacing.sm))
        }
    }
}

@Composable
private fun Progress(done: Int, total: Int, showIntro: Boolean) {
    Column(Modifier.padding(horizontal = Spacing.page + Spacing.lg)) {
        if (showIntro) {
            Text(
                stringResource(R.string.setup_intro),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.lg))
        }
        Text(stringResource(R.string.setup_progress, done, total), style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(Spacing.sm))
        LinearProgressIndicator(
            progress = { done.toFloat() / total },
            color = if (done == total) MaterialTheme.appColors.success else MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth().height(6.dp),
        )
    }
}

/** One permission. Open steps explain themselves and offer the action; finished ones shrink to a title with a tick. */
@Composable
private fun Step(
    title: String,
    body: String,
    done: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    Group {
        Row(Modifier.padding(Spacing.lg)) {
            StatusMark(done)
            Spacer(Modifier.width(Spacing.lg))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (!done) {
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Spacing.md))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        FilledTonalButton(onClick = onAction) { Text(actionLabel) }
                        if (secondaryLabel != null && onSecondary != null) {
                            TextButton(onClick = onSecondary) { Text(secondaryLabel) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusMark(done: Boolean) {
    Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
        if (done) {
            Icon(
                painterResource(R.drawable.ic_check_circle),
                stringResource(R.string.step_done),
                tint = MaterialTheme.appColors.success,
                modifier = Modifier.size(24.dp),
            )
        } else {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.appColors.card,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.size(20.dp),
            ) {}
        }
    }
}

/** Short summary for the status card on the main screen; null when nothing essential is missing. */
@Composable
fun missingProtectionSummary(status: ProtectionStatus): String? = when {
    !status.accessibility -> stringResource(R.string.missing_accessibility)
    !status.deviceAdmin -> stringResource(R.string.missing_admin)
    !status.batteryExempt -> stringResource(R.string.missing_battery)
    else -> null
}
