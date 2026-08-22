package com.ljyh.mei.ui.screen.history

import android.text.format.DateUtils
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import coil3.compose.AsyncImage
import com.kyant.capsule.ContinuousRoundedRectangle
import com.ljyh.mei.R
import com.ljyh.mei.data.model.room.HistoryItem
import com.ljyh.mei.playback.queue.ListQueue
import com.ljyh.mei.ui.glass.GlassCard
import com.ljyh.mei.ui.glass.GlassIconButton
import com.ljyh.mei.ui.glass.IosListRow
import com.ljyh.mei.ui.glass.IosPinnedListPage
import com.ljyh.mei.ui.glass.SfIcon
import com.ljyh.mei.ui.glass.SfSymbol
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.screen.main.library.component.groupedLazyItems

@OptIn(UnstableApi::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
    val navController = LocalNavController.current
    val playerConnection = LocalPlayerConnection.current
    val historyList by viewModel.historyList.collectAsState(initial = emptyList())
    val insets = LocalPlayerAwareWindowInsets.current.asPaddingValues()

    IosPinnedListPage(
        title = stringResource(R.string.listening_history),
        onNavigateBack = navController::navigateUp,
        bottomPadding = insets.calculateBottomPadding(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
        actions = {
            if (historyList.isNotEmpty()) {
                GlassIconButton(viewModel::clearHistory) {
                    SfIcon("trash", stringResource(R.string.clear_history))
                }
            }
        },
    ) {
        if (historyList.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                    EmptyHistoryState()
                }
            }
        } else {
            groupedLazyItems(
                items = historyList,
                key = { "history-${it.historyId}" },
                contentType = "history-item",
                firstItemTopPadding = 10.dp,
            ) { item, index ->
                IosListRow(
                    title = item.song.title,
                    subtitle = item.song.artist.joinToString(" / "),
                    detail = relativeTime(item.playedAt),
                    showTopSeparator = index > 0,
                    leading = {
                        AsyncImage(
                            model = item.song.cover,
                            contentDescription = null,
                            modifier = Modifier.size(44.dp).clip(ContinuousRoundedRectangle(10.dp)),
                            contentScale = ContentScale.Crop,
                        )
                    },
                    onClick = {
                        playerConnection?.playQueue(
                            ListQueue(
                                id = "history",
                                title = navController.context.getString(R.string.listening_history),
                                items = historyList.map { it.song.id to null },
                                startIndex = index,
                                position = 0,
                            ),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun HistoryItemRow(item: HistoryItem, onClick: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = item.song.cover,
                contentDescription = null,
                modifier = Modifier.size(58.dp).clip(ContinuousRoundedRectangle(13.dp)),
                contentScale = ContentScale.Crop,
            )
            Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(item.song.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    item.song.artist.joinToString(" / "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                relativeTime(item.playedAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyHistoryState() {
    GlassCard(Modifier.fillMaxWidth().padding(top = 42.dp)) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 52.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SfIcon(SfSymbol.Clock, null, size = 48.dp)
            Text(stringResource(R.string.no_listening_history), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun relativeTime(timestamp: Long): String = DateUtils.getRelativeTimeSpanString(
    timestamp,
    System.currentTimeMillis(),
    DateUtils.MINUTE_IN_MILLIS,
    DateUtils.FORMAT_ABBREV_RELATIVE,
).toString()
