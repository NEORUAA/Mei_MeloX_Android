package com.ljyh.mei.ui.screen.podcast

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import coil3.compose.AsyncImage
import com.ljyh.mei.R
import com.ljyh.mei.data.model.MediaMetadata
import com.ljyh.mei.data.model.melox.Podcast
import com.ljyh.mei.data.model.melox.PodcastProgram
import com.ljyh.mei.data.model.toMediaItem
import com.ljyh.mei.playback.queue.ListQueue
import com.ljyh.mei.ui.glass.GlassButton
import com.ljyh.mei.ui.glass.GlassCard
import com.ljyh.mei.ui.glass.GlassEmphasis
import com.ljyh.mei.ui.glass.GlassIconButton
import com.ljyh.mei.ui.glass.SfIcon
import com.ljyh.mei.ui.glass.SfSymbol
import com.ljyh.mei.ui.glass.IosPinnedListPage
import com.ljyh.mei.ui.glass.IosGroupedList
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.screen.Screen

@Composable
fun PodcastScreen(
    viewModel: PodcastViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val navController = LocalNavController.current
    val insets = LocalPlayerAwareWindowInsets.current
    val bottomPadding = insets.asPaddingValues().calculateBottomPadding()
    val topPadding = insets.asPaddingValues().calculateTopPadding()

    when {
        state.isLoading && state.home == null -> LoadingState()
        state.error != null && state.home == null -> ErrorState(state.error, viewModel::refresh)
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = topPadding + 12.dp, bottom = bottomPadding + 20.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.podcasts),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            state.home?.categories?.takeIf(List<*>::isNotEmpty)?.let { categories ->
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(categories, key = { it.id }) { category ->
                            GlassButton(
                                onClick = { viewModel.selectCategory(category.id) },
                                emphasis = if (state.selectedCategoryId == category.id) {
                                    GlassEmphasis.Prominent
                                } else {
                                    GlassEmphasis.Regular
                                },
                            ) {
                                Text(category.name, maxLines = 1)
                            }
                        }
                    }
                }
            }
            val visible = state.categoryPodcasts.takeIf { state.selectedCategoryId != null }
                ?: state.home?.personalized.orEmpty()
            item {
                PodcastSection(
                    title = stringResource(
                        if (state.selectedCategoryId == null) R.string.podcast_for_you else R.string.podcast_category,
                    ),
                    podcasts = visible,
                    onClick = { Screen.PodcastDetail.navigate(navController) { addPath(it.toString()) } },
                )
            }
            state.home?.featured?.takeIf(List<*>::isNotEmpty)?.let { featured ->
                item {
                    PodcastSection(
                        title = stringResource(R.string.podcast_featured),
                        podcasts = featured,
                        onClick = { Screen.PodcastDetail.navigate(navController) { addPath(it.toString()) } },
                    )
                }
            }
        }
    }
}

@Composable
private fun PodcastSection(
    title: String,
    podcasts: List<Podcast>,
    onClick: (Long) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(podcasts, key = { it.id }) { podcast ->
                GlassCard(
                    modifier = Modifier.size(width = 176.dp, height = 244.dp),
                    onClick = { onClick(podcast.id) },
                ) {
                    Column(Modifier.padding(10.dp)) {
                        AsyncImage(
                            model = podcast.picUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(18.dp)),
                        )
                        Text(
                            podcast.name,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        Text(
                            podcast.recommendation ?: podcast.host?.nickname.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PodcastDetailScreen(
    id: Long,
    viewModel: PodcastDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val navController = LocalNavController.current
    val playerConnection = LocalPlayerConnection.current
    val bottomPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()
    LaunchedEffect(id) { viewModel.load(id) }

    when {
        state.isLoading && state.detail == null -> LoadingState()
        state.error != null && state.detail == null -> ErrorState(state.error) { viewModel.load(id, true) }
        else -> state.detail?.let { detail ->
            IosPinnedListPage(
                title = detail.podcast.name,
                subtitle = detail.podcast.host?.nickname,
                bottomPadding = bottomPadding,
                onNavigateBack = navController::navigateUp,
                actions = {
                    GlassButton(
                        onClick = viewModel::toggleSubscription,
                        emphasis = if (detail.podcast.isSubscribed) GlassEmphasis.Prominent else GlassEmphasis.Regular,
                    ) {
                        Text(stringResource(if (detail.podcast.isSubscribed) R.string.podcast_subscribed else R.string.podcast_subscribe))
                    }
                },
            ) {
                item {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = detail.podcast.picUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(112.dp).clip(RoundedCornerShape(22.dp)),
                            )
                            Column(Modifier.padding(start = 14.dp)) {
                                Text(detail.podcast.host?.nickname.orEmpty(), fontWeight = FontWeight.SemiBold)
                                Text(
                                    detail.podcast.description.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 5,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
                item {
                    IosGroupedList {
                        detail.programs.forEachIndexed { index, program ->
                            PodcastProgramRow(program) {
                                val playable = detail.programs.filter { it.mainSongId != null }
                                val startIndex = playable.indexOfFirst { it.id == program.id }.coerceAtLeast(0)
                                val items = playable.map { item ->
                                    val song = item.asMediaMetadata().toMediaItem()
                                    song.mediaId to song
                                }
                                if (items.isNotEmpty()) {
                                    playerConnection?.playQueue(
                                        ListQueue("podcast_${detail.podcast.id}", detail.podcast.name, items, startIndex),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PodcastProgramRow(program: PodcastProgram, onClick: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = program.coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)),
            )
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(program.name, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    stringResource(R.string.podcast_duration_minutes, program.durationMs / 60_000),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SfIcon(SfSymbol.PlayFilled, stringResource(R.string.player_play))
        }
    }
}

private fun PodcastProgram.asMediaMetadata() = MediaMetadata(
    id = requireNotNull(mainSongId),
    title = name,
    coverUrl = coverUrl.orEmpty(),
    artists = listOf(MediaMetadata.Artist(host?.id ?: 0, host?.nickname ?: radioName)),
    duration = durationMs,
    album = MediaMetadata.Album(radioId, radioName),
)

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
private fun ErrorState(message: String?, retry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(message ?: stringResource(R.string.load_failed))
            GlassButton(onClick = retry) { Text(stringResource(R.string.retry)) }
        }
    }
}
