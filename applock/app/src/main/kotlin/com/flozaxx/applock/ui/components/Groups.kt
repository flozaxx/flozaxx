package com.flozaxx.applock.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flozaxx.applock.R
import com.flozaxx.applock.ui.theme.Spacing
import com.flozaxx.applock.ui.theme.appColors

private val ROW_MIN_HEIGHT = 56.dp

@Composable
fun GroupTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = Spacing.page + Spacing.lg, end = Spacing.page, top = Spacing.xl, bottom = Spacing.sm),
    )
}

/** A card holding related rows. Put a [GroupDivider] between rows. */
@Composable
fun Group(modifier: Modifier = Modifier, title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier) {
        if (title != null) GroupTitle(title)
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.appColors.card,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.page),
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun GroupDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = Spacing.lg),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )
}

/** Explanatory text that belongs to the group above it. */
@Composable
fun GroupHint(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = Spacing.page + Spacing.lg, vertical = Spacing.sm),
    )
}

/** Where a row sits inside a group that is built item by item in a lazy list. */
enum class GroupPosition {
    ONLY, FIRST, MIDDLE, LAST;

    companion object {
        fun of(index: Int, count: Int): GroupPosition = when {
            count == 1 -> ONLY
            index == 0 -> FIRST
            index == count - 1 -> LAST
            else -> MIDDLE
        }
    }
}

/** One row of a lazily built group: together the items look like a single [Group] card. */
@Composable
fun GroupItem(position: GroupPosition, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val corner = MaterialTheme.shapes.large.topStart
    val flat = CornerSize(0.dp)
    val shape: Shape = when (position) {
        GroupPosition.ONLY -> RoundedCornerShape(corner)
        GroupPosition.FIRST -> RoundedCornerShape(corner, corner, flat, flat)
        GroupPosition.MIDDLE -> RoundedCornerShape(flat)
        GroupPosition.LAST -> RoundedCornerShape(flat, flat, corner, corner)
    }
    Surface(
        shape = shape,
        color = MaterialTheme.appColors.card,
        modifier = modifier.fillMaxWidth().padding(horizontal = Spacing.page),
    ) {
        Column {
            content()
            if (position == GroupPosition.FIRST || position == GroupPosition.MIDDLE) GroupDivider()
        }
    }
}

/** An icon on a round tinted plate; the app's one way of giving an icon emphasis. */
@Composable
fun IconBadge(
    iconRes: Int,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    container: Color = MaterialTheme.colorScheme.primaryContainer,
    tint: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    Surface(shape = MaterialTheme.shapes.extraLarge, color = container, modifier = modifier.size(size)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(painterResource(iconRes), contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.55f))
        }
    }
}

@Composable
private fun RowText(title: String, supporting: String?, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        if (supporting != null) {
            Text(supporting, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun Modifier.groupRow(): Modifier =
    fillMaxWidth().heightIn(min = ROW_MIN_HEIGHT).padding(horizontal = Spacing.lg, vertical = Spacing.md)

@Composable
fun RadioRow(title: String, selected: Boolean, onSelect: () -> Unit, supporting: String? = null) {
    Row(
        Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onSelect).groupRow(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(Spacing.lg))
        RowText(title, supporting)
    }
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
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .groupRow(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowText(title, supporting, Modifier.weight(1f))
        Spacer(Modifier.width(Spacing.lg))
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
fun LinkRow(iconRes: Int, title: String, supporting: String? = null, onClick: () -> Unit) {
    Row(Modifier.clickable(onClick = onClick).groupRow(), verticalAlignment = Alignment.CenterVertically) {
        IconBadge(iconRes, size = 36.dp)
        Spacer(Modifier.width(Spacing.lg))
        RowText(title, supporting, Modifier.weight(1f))
        Icon(painterResource(R.drawable.ic_chevron_right), null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
