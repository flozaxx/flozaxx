package com.flozaxx.applock.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flozaxx.applock.R
import com.flozaxx.applock.context.WifiSnapshot
import com.flozaxx.applock.graph
import com.flozaxx.applock.lock.LockScreenStyles
import com.flozaxx.applock.lock.hasEnrolledBiometric
import com.flozaxx.applock.rules.NetworkRule
import com.flozaxx.applock.rules.RelockPolicy
import com.flozaxx.applock.storage.AppSettings
import com.flozaxx.applock.storage.NotificationMode
import com.flozaxx.applock.storage.TrustedNetwork
import com.flozaxx.applock.ui.components.LinkRow
import com.flozaxx.applock.ui.components.RadioRow
import com.flozaxx.applock.ui.components.ScreenScaffold
import com.flozaxx.applock.ui.components.SectionTitle
import kotlinx.coroutines.launch

private val RELOCK_MINUTES = listOf(1, 5, 15, 30)

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenStyle: () -> Unit,
    onOpenProtection: () -> Unit,
    onOpenLimits: () -> Unit,
    onChangePin: () -> Unit,
    onNewRecoveryCode: () -> Unit,
) {
    val context = LocalContext.current
    val graph = context.graph
    val scope = rememberCoroutineScope()
    val settings by graph.store.settings.collectAsStateWithLifecycle()
    val lockActive by graph.coordinator.lockActive.collectAsStateWithLifecycle(initialValue = true)
    val update = { transform: (AppSettings) -> AppSettings -> scope.launch { graph.store.update(transform) }; Unit }

    ScreenScaffold(title = stringResource(R.string.settings_title), onBack = onBack) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
            SectionTitle(stringResource(R.string.network_rule_title))
            RadioRow(
                stringResource(R.string.network_rule_on_trusted),
                selected = settings.networkRule == NetworkRule.ON_TRUSTED_NETWORK,
                onSelect = { update { it.copy(networkRule = NetworkRule.ON_TRUSTED_NETWORK) } },
                supporting = stringResource(R.string.network_rule_on_trusted_hint),
            )
            RadioRow(
                stringResource(R.string.network_rule_off_trusted),
                selected = settings.networkRule == NetworkRule.OFF_TRUSTED_NETWORK,
                onSelect = { update { it.copy(networkRule = NetworkRule.OFF_TRUSTED_NETWORK) } },
                supporting = stringResource(R.string.network_rule_off_trusted_hint),
            )
            RadioRow(
                stringResource(R.string.network_rule_always),
                selected = settings.networkRule == NetworkRule.ALWAYS,
                onSelect = { update { it.copy(networkRule = NetworkRule.ALWAYS) } },
            )
            Text(
                stringResource(if (lockActive) R.string.network_state_active else R.string.network_state_inactive),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )

            if (settings.networkRule != NetworkRule.ALWAYS) {
                SectionTitle(stringResource(R.string.trusted_networks_title))
                TrustedNetworkEditor(settings, horizontalPadding = 20.dp)
            }

            SectionTitle(stringResource(R.string.relock_title))
            RadioRow(
                stringResource(R.string.relock_immediately),
                selected = settings.relockPolicy == RelockPolicy.IMMEDIATELY,
                onSelect = { update { it.copy(relockPolicy = RelockPolicy.IMMEDIATELY) } },
                supporting = stringResource(R.string.relock_immediately_hint),
            )
            RadioRow(
                stringResource(R.string.relock_screen_off),
                selected = settings.relockPolicy == RelockPolicy.ON_SCREEN_OFF,
                onSelect = { update { it.copy(relockPolicy = RelockPolicy.ON_SCREEN_OFF) } },
            )
            RadioRow(
                stringResource(R.string.relock_timeout),
                selected = settings.relockPolicy == RelockPolicy.AFTER_TIMEOUT,
                onSelect = { update { it.copy(relockPolicy = RelockPolicy.AFTER_TIMEOUT) } },
                supporting = stringResource(R.string.relock_timeout_hint),
            )
            if (settings.relockPolicy == RelockPolicy.AFTER_TIMEOUT) {
                Row(Modifier.padding(start = 68.dp, end = 20.dp)) {
                    RELOCK_MINUTES.forEach { minutes ->
                        FilterChip(
                            selected = settings.relockTimeoutMinutes == minutes,
                            onClick = { update { it.copy(relockTimeoutMinutes = minutes) } },
                            label = { Text(stringResource(R.string.relock_minutes, minutes)) },
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    }
                }
            }

            SectionTitle(stringResource(R.string.appearance_title))
            LinkRow(
                R.drawable.ic_palette,
                stringResource(R.string.style_setting),
                stringResource(LockScreenStyles.byId(settings.lockStyleId).nameRes),
                onClick = onOpenStyle,
            )

            SectionTitle(stringResource(R.string.notification_default_title))
            NotificationModePicker(settings.defaultNotificationMode) { mode -> update { it.copy(defaultNotificationMode = mode) } }
            Text(
                stringResource(R.string.notification_default_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )

            SectionTitle(stringResource(R.string.security_title))
            val biometricEnrolled = remember { context.hasEnrolledBiometric() }
            SwitchRow(
                title = stringResource(R.string.biometric_setting),
                supporting = stringResource(
                    if (biometricEnrolled) R.string.biometric_setting_hint else R.string.biometric_not_enrolled,
                ),
                checked = settings.biometricEnabled && biometricEnrolled,
                enabled = biometricEnrolled,
                onCheckedChange = { enabled -> update { it.copy(biometricEnabled = enabled) } },
            )
            LinkRow(R.drawable.ic_key, stringResource(R.string.pin_change_title), onClick = onChangePin)
            LinkRow(
                R.drawable.ic_mail,
                stringResource(R.string.recovery_renew),
                stringResource(R.string.recovery_renew_hint),
                onClick = onNewRecoveryCode,
            )

            SectionTitle(stringResource(R.string.protection_title))
            LinkRow(R.drawable.ic_shield, stringResource(R.string.protection_permissions), onClick = onOpenProtection)
            LinkRow(R.drawable.ic_info, stringResource(R.string.limits_title), onClick = onOpenLimits)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun NotificationModePicker(selected: NotificationMode, onSelect: (NotificationMode) -> Unit) {
    RadioRow(
        stringResource(R.string.notification_mode_untouched),
        selected = selected == NotificationMode.UNTOUCHED,
        onSelect = { onSelect(NotificationMode.UNTOUCHED) },
        supporting = stringResource(R.string.notification_mode_untouched_hint),
    )
    RadioRow(
        stringResource(R.string.notification_mode_hide_content),
        selected = selected == NotificationMode.HIDE_CONTENT,
        onSelect = { onSelect(NotificationMode.HIDE_CONTENT) },
        supporting = stringResource(R.string.notification_mode_hide_content_hint),
    )
    RadioRow(
        stringResource(R.string.notification_mode_hide_completely),
        selected = selected == NotificationMode.HIDE_COMPLETELY,
        onSelect = { onSelect(NotificationMode.HIDE_COMPLETELY) },
        supporting = stringResource(R.string.notification_mode_hide_completely_hint),
    )
}

@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    supporting: String? = null,
    enabled: Boolean = true,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (supporting != null) {
                Text(
                    supporting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

/** The saved trusted networks plus a button to add the network the phone is connected to right now. */
@Composable
fun TrustedNetworkEditor(settings: AppSettings, horizontalPadding: Dp) {
    val graph = LocalContext.current.graph
    val scope = rememberCoroutineScope()
    val wifi by graph.wifi.state.collectAsStateWithLifecycle()

    Column {
        settings.trustedNetworks.forEach { network ->
            Row(
                Modifier.fillMaxWidth().padding(start = horizontalPadding, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(R.drawable.ic_wifi), null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(network.ssid.ifEmpty { stringResource(R.string.trusted_network_unnamed) }, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        network.bssid,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = {
                        scope.launch { graph.store.update { it.copy(trustedNetworks = it.trustedNetworks - network) } }
                    },
                ) {
                    Icon(painterResource(R.drawable.ic_delete), stringResource(R.string.trusted_network_remove))
                }
            }
        }
        if (settings.trustedNetworks.isNotEmpty()) {
            HorizontalDivider(Modifier.padding(horizontal = horizontalPadding, vertical = 8.dp))
        }

        val connected = wifi as? WifiSnapshot.Connected
        val alreadySaved = connected != null && settings.trustedNetworks.any { it.bssid == connected.bssid }
        Text(
            when {
                connected == null && wifi == WifiSnapshot.NotConnected -> stringResource(R.string.trusted_network_no_wifi)
                connected == null -> stringResource(R.string.trusted_network_unreadable)
                alreadySaved -> stringResource(R.string.trusted_network_already, connected.ssid)
                else -> stringResource(R.string.trusted_network_connected, connected.ssid)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = horizontalPadding),
        )
        OutlinedButton(
            enabled = connected != null && !alreadySaved,
            onClick = {
                if (connected != null) {
                    val network = TrustedNetwork(connected.bssid, connected.ssid)
                    scope.launch { graph.store.update { it.copy(trustedNetworks = it.trustedNetworks + network) } }
                }
            },
            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 8.dp),
        ) {
            Text(stringResource(R.string.trusted_network_add))
        }
        Text(
            stringResource(R.string.trusted_network_bands_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = horizontalPadding),
        )
    }
}
