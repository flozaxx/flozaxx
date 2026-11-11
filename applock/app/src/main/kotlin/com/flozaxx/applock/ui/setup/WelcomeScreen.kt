package com.flozaxx.applock.ui.setup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.flozaxx.applock.R
import com.flozaxx.applock.ui.components.Group
import com.flozaxx.applock.ui.components.GroupDivider
import com.flozaxx.applock.ui.components.IconBadge
import com.flozaxx.applock.ui.components.PrimaryButton
import com.flozaxx.applock.ui.theme.Spacing
import com.flozaxx.applock.ui.theme.appColors

@Composable
fun WelcomeScreen(onStart: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.appColors.page) {
        Column(Modifier.systemBarsPadding()) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Spacer(Modifier.height(72.dp))
                Column(Modifier.padding(horizontal = Spacing.page + Spacing.lg)) {
                    IconBadge(R.drawable.ic_lock, size = 64.dp)
                    Spacer(Modifier.height(Spacing.xl))
                    Text(stringResource(R.string.welcome_title), style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        stringResource(R.string.welcome_body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(Spacing.xxl))
                Group {
                    Feature(R.drawable.ic_lock, R.string.welcome_feature_lock_title, R.string.welcome_feature_lock_body)
                    GroupDivider()
                    Feature(
                        R.drawable.ic_notifications,
                        R.string.welcome_feature_notifications_title,
                        R.string.welcome_feature_notifications_body,
                    )
                    GroupDivider()
                    Feature(R.drawable.ic_wifi, R.string.welcome_feature_network_title, R.string.welcome_feature_network_body)
                }
                Spacer(Modifier.height(Spacing.lg))
            }
            Column(
                Modifier.padding(horizontal = Spacing.page, vertical = Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(R.string.welcome_duration),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(Spacing.md))
                PrimaryButton(stringResource(R.string.welcome_start), onStart)
            }
        }
    }
}

@Composable
private fun Feature(iconRes: Int, titleRes: Int, bodyRes: Int) {
    Row(Modifier.fillMaxWidth().padding(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
        IconBadge(iconRes, size = 36.dp)
        Spacer(Modifier.width(Spacing.lg))
        Column {
            Text(stringResource(titleRes), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(bodyRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
