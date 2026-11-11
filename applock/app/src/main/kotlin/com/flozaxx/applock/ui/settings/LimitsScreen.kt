package com.flozaxx.applock.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.flozaxx.applock.R
import com.flozaxx.applock.ui.components.Group
import com.flozaxx.applock.ui.components.GroupDivider
import com.flozaxx.applock.ui.components.ScreenScaffold
import com.flozaxx.applock.ui.theme.Spacing

/** What this kind of lock cannot do, said plainly. */
@Composable
fun LimitsScreen(onBack: () -> Unit) {
    val titles = stringArrayResource(R.array.limit_titles)
    val bodies = stringArrayResource(R.array.limit_bodies)

    ScreenScaffold(title = stringResource(R.string.limits_title), onBack = onBack) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
            Text(
                stringResource(R.string.limits_intro),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.page + Spacing.lg).padding(bottom = Spacing.lg),
            )
            Group {
                titles.forEachIndexed { index, title ->
                    if (index > 0) GroupDivider()
                    Column(Modifier.padding(Spacing.lg)) {
                        Text(title, style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(Spacing.xs))
                        Text(
                            bodies[index],
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(Spacing.xl))
        }
    }
}
