package com.ljyh.mei.ui.component.player.component.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ljyh.mei.R
import com.ljyh.mei.ui.component.player.PlayerViewModel
import com.ljyh.mei.ui.glass.IosAlertSurface
import com.ljyh.mei.ui.glass.IosGroupedList
import com.ljyh.mei.ui.glass.IosListRow
import com.ljyh.mei.ui.glass.IosModalSheet
import com.ljyh.mei.ui.glass.SfIcon
import com.ljyh.mei.ui.glass.SfSymbol
import com.ljyh.mei.ui.model.MoreAction
import com.ljyh.mei.ui.model.SortOrder

@Composable
fun MoreActionsSheet(
    onDismissRequest: () -> Unit,
    onActionClick: (MoreAction) -> Unit,
    viewModel: PlayerViewModel,
) {
    val sortOrder by viewModel.moreSortOrder.collectAsState()
    val moreActions by viewModel.sortedMoreActions.collectAsState()
    var showSortOptions by remember { mutableStateOf(false) }

    if (showSortOptions) {
        SortOptionsDialog(
            currentOrder = sortOrder,
            onOrderSelected = {
                viewModel.setMoreSortOrder(it)
                showSortOptions = false
            },
            onDismiss = { showSortOptions = false },
        )
    }

    IosModalSheet(
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.more_actions_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { showSortOptions = true }) {
                    SfIcon(SfSymbol.Settings, stringResource(R.string.more_actions_sort))
                }
            }
            LazyColumn(
                // Size to content, capped by the sheet's available height (fill = false);
                // a hardcoded max clipped longer lists instead of matching them.
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                item {
                    IosGroupedList {
                        moreActions.forEachIndexed { index, action ->
                            IosListRow(
                                title = stringResource(action.labelRes),
                                systemName = action.systemName,
                                showTopSeparator = index > 0,
                                onClick = { onActionClick(action) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SortOptionsDialog(
    currentOrder: SortOrder,
    onOrderSelected: (SortOrder) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        IosAlertSurface(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.more_actions_sort_title),
        ) {
            IosGroupedList {
                SortOptionRow(
                    text = stringResource(R.string.more_actions_sort_frequency),
                    selected = currentOrder == SortOrder.FREQUENCY,
                    showTopSeparator = false,
                    onClick = { onOrderSelected(SortOrder.FREQUENCY) },
                )
                SortOptionRow(
                    text = stringResource(R.string.more_actions_sort_risk),
                    selected = currentOrder == SortOrder.RISK,
                    onClick = { onOrderSelected(SortOrder.RISK) },
                )
                IosListRow(title = stringResource(R.string.done), onClick = onDismiss)
            }
        }
    }
}

@Composable
private fun SortOptionRow(
    text: String,
    selected: Boolean,
    showTopSeparator: Boolean = true,
    onClick: () -> Unit,
) {
    IosListRow(
        title = text,
        showTopSeparator = showTopSeparator,
        onClick = onClick,
        trailing = if (selected) {
            { SfIcon("checkmark", null, size = 17.dp) }
        } else {
            null
        },
    )
}
