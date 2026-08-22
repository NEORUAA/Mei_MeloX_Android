package com.ljyh.mei.ui.screen.main.library.component

import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.kyant.capsule.ContinuousRoundedRectangle
import com.ljyh.mei.R
import com.ljyh.mei.data.model.MediaMetadata
import com.ljyh.mei.data.model.room.DownloadStatus
import com.ljyh.mei.data.model.room.DownloadTask
import com.ljyh.mei.data.model.room.HistoryItem
import com.ljyh.mei.data.model.room.Playlist
import com.ljyh.mei.data.model.toMediaItem
import com.ljyh.mei.di.AppDatabase
import com.ljyh.mei.playback.PlayerConnection
import com.ljyh.mei.playback.queue.ListQueue
import com.ljyh.mei.ui.component.GlobalProfileAvatarButton
import com.ljyh.mei.ui.component.player.OverlayState
import com.ljyh.mei.ui.glass.GlassSegmentedControl
import com.ljyh.mei.ui.glass.IosGroupedList
import com.ljyh.mei.ui.glass.IosListRow
import com.ljyh.mei.ui.glass.IosPinnedPage
import com.ljyh.mei.ui.glass.IosScrollableTabRow
import com.ljyh.mei.ui.glass.IosTypography
import com.ljyh.mei.ui.glass.LocalGlassColors
import com.ljyh.mei.ui.glass.LocalGroupedListBackgroundAlpha
import com.ljyh.mei.ui.glass.LocalGroupedListIconColor
import com.ljyh.mei.ui.glass.SfIcon
import com.ljyh.mei.ui.glass.SfSymbol
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.model.Album
import com.ljyh.mei.ui.navigation.LibraryPage
import com.ljyh.mei.ui.screen.Screen
import com.ljyh.mei.ui.screen.cloud.CloudMusicViewModel
import com.ljyh.mei.ui.screen.cloud.CloudMusicUiState
import com.ljyh.mei.ui.screen.history.HistoryViewModel
import com.ljyh.mei.ui.screen.playlist.component.StandaloneTrackActionOverlay
import com.ljyh.mei.ui.screen.podcast.PodcastViewModel
import com.ljyh.mei.ui.screen.podcast.PodcastUiState

/**
 * Adds rows directly to the parent lazy list while retaining the visual treatment of
 * [IosGroupedList]. The original component owns a ColumnScope, so putting a large dynamic list
 * inside it eagerly composes every row. Each row gets the corresponding edge shape here and can
 * therefore be composed and disposed independently by the parent LazyColumn.
 */
internal fun <T> LazyListScope.groupedLazyItems(
    items: List<T>,
    key: ((T) -> Any)? = null,
    contentType: Any? = "grouped-row",
    firstItemTopPadding: Dp = 0.dp,
    itemContent: @Composable (item: T, index: Int) -> Unit,
) {
    itemsIndexed(
        items = items,
        key = key?.let { itemKey -> { _, item -> itemKey(item) } },
        contentType = { _, _ -> contentType },
    ) { index, item ->
        GroupedLazyListRow(
            index = index,
            itemCount = items.size,
            modifier = if (index == 0) Modifier.padding(top = firstItemTopPadding) else Modifier,
        ) {
            itemContent(item, index)
        }
    }
}

@Composable
private fun GroupedLazyListRow(
    index: Int,
    itemCount: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = LocalGlassColors.current
    val background = colors.elevatedBackground.copy(
        alpha = LocalGroupedListBackgroundAlpha.current.coerceIn(0f, 1f),
    )
    val shape = when {
        itemCount == 1 -> ContinuousRoundedRectangle(26.dp)
        index == 0 -> ContinuousRoundedRectangle(topStart = 26.dp, topEnd = 26.dp)
        index == itemCount - 1 -> ContinuousRoundedRectangle(bottomStart = 26.dp, bottomEnd = 26.dp)
        else -> RectangleShape
    }
    Box(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background, shape),
    ) {
        CompositionLocalProvider(
            LocalGroupedListIconColor provides colors.accent,
            LocalContentColor provides colors.content,
        ) {
            content()
        }
    }
}

/** MeloX Library: six user-configurable content pages under the shared collapsing title. */
@Composable
fun LibraryMobileLayout(
    @Suppress("UNUSED_PARAMETER") userPhoto: String,
    selectedPage: LibraryPage,
    onPageSelect: (LibraryPage) -> Unit,
    createdPlaylists: List<Playlist>,
    collectedPlaylists: List<Playlist>,
    albums: List<Album>,
    likedSongs: List<MediaMetadata>,
    likedSongsLoading: Boolean,
    userId: String,
    onPlaylistClick: (String) -> Unit,
    onAlbumClick: (String) -> Unit,
) {
    val navController = LocalNavController.current
    val playerConnection = LocalPlayerConnection.current
    val listState = rememberLazyListState()
    val insets = LocalPlayerAwareWindowInsets.current.asPaddingValues()
    val podcastViewModel: PodcastViewModel? = if (selectedPage == LibraryPage.Podcasts) hiltViewModel() else null
    val podcastState = podcastViewModel?.state?.collectAsState()?.value
    val cloudViewModel: CloudMusicViewModel? = if (selectedPage == LibraryPage.Cloud) hiltViewModel() else null
    val cloudState = cloudViewModel?.state?.collectAsState()?.value
    val historyViewModel: HistoryViewModel? = if (selectedPage == LibraryPage.History) hiltViewModel() else null
    val history = historyViewModel?.historyList?.collectAsState(initial = emptyList())?.value.orEmpty()
    val downloadTasks = if (selectedPage == LibraryPage.Downloads) {
        val context = LocalContext.current
        val dao = remember(context) { AppDatabase.getDatabase(context).downloadDao() }
        val tasksFlow = remember(dao) { dao.getAll() }
        tasksFlow.collectAsState(initial = emptyList()).value
    } else {
        emptyList()
    }
    val collapseDistance = with(LocalDensity.current) { 56.dp.toPx() }
    val collapseProgress by remember(listState) {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / collapseDistance).coerceIn(0f, 1f)
        }
    }
    val pages = LibraryPage.entries
    val title = stringResource(R.string.app_tab_library)
    val likedTitle = stringResource(R.string.app_tab_library_songs)
    val usesGroupedLazyRows = selectedPage == LibraryPage.Podcasts ||
        selectedPage == LibraryPage.Downloads ||
        selectedPage == LibraryPage.Cloud ||
        selectedPage == LibraryPage.History
    val pageSpacing = if (usesGroupedLazyRows) 12.dp else 0.dp
    var currentOverlay by remember { mutableStateOf<OverlayState>(OverlayState.None) }

    IosPinnedPage(
        title = title,
        bottomPadding = insets.calculateBottomPadding(),
        collapseProgress = collapseProgress,
        actions = { GlobalProfileAvatarButton() },
    ) { contentPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = contentPadding.calculateTopPadding(),
                end = 16.dp,
                bottom = contentPadding.calculateBottomPadding(),
            ),
            verticalArrangement = if (usesGroupedLazyRows) Arrangement.spacedBy(0.dp) else Arrangement.spacedBy(12.dp),
        ) {
            item(key = "library-title") {
                Text(
                    title,
                    style = IosTypography.largeTitle,
                    color = LocalGlassColors.current.content,
                    modifier = Modifier
                        .offset(y = (-10).dp)
                        .padding(vertical = 6.dp)
                        .padding(bottom = pageSpacing),
                )
            }
            item(key = "library-pages") {
                GlassSegmentedControl(
                    items = pages.map { it to stringResource(it.titleRes) },
                    selected = selectedPage,
                    onSelected = onPageSelect,
                    modifier = Modifier.fillMaxWidth().padding(bottom = pageSpacing),
                )
            }

            when (selectedPage) {
                LibraryPage.Songs -> {
                    if (likedSongs.isEmpty() && likedSongsLoading) {
                        item(key = "liked-loading") {
                            Box(
                                Modifier.fillMaxWidth().height(120.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    } else if (likedSongs.isNotEmpty()) {
                        item(key = "liked-actions") {
                            IosGroupedList {
                                IosListRow(
                                    title = stringResource(R.string.library_play_all),
                                    systemName = "play.fill",
                                    showTopSeparator = false,
                                    onClick = {
                                        playerConnection?.playQueue(
                                            ListQueue(
                                                id = "library-liked",
                                                title = likedTitle,
                                                items = likedSongs.map { it.id.toString() to it.toMediaItem() },
                                            ),
                                        )
                                    },
                                )
                                IosListRow(
                                    title = stringResource(R.string.library_heart_mode),
                                    systemName = "heart.circle.fill",
                                    onClick = { playerConnection?.fmStart(likedSongs.randomOrNull()?.id?.toString()) },
                                )
                            }
                        }
                        items(
                            likedSongs,
                            key = { "liked-${it.id}" },
                            contentType = { "liked-song" },
                        ) { song ->
                            LibrarySongRow(
                                song = song,
                                onClick = {
                                    val index = likedSongs.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
                                    playerConnection?.playQueue(
                                        ListQueue(
                                            id = "library-liked",
                                            title = likedTitle,
                                            items = likedSongs.map { it.id.toString() to it.toMediaItem() },
                                            startIndex = index,
                                        ),
                                    )
                                },
                                onMoreClick = {
                                    currentOverlay = OverlayState.TrackActionMenu(song)
                                },
                            )
                        }
                    } else {
                        item { EmptyState(stringResource(R.string.library_empty_songs), SfSymbol.MusicNote) }
                    }
                }

                LibraryPage.Playlists -> {
                    item(key = "playlist-rank") {
                        IosGroupedList {
                            IosListRow(
                                title = stringResource(R.string.account_listening_rank),
                                systemName = "chart.bar.xaxis",
                                showTopSeparator = false,
                                onClick = {
                                    Screen.AccountListeningRank.navigate(navController) { addPath(userId) }
                                },
                            )
                        }
                    }
                    val playlists = createdPlaylists + collectedPlaylists
                    if (playlists.isEmpty()) {
                        item { EmptyState(stringResource(R.string.library_empty_collected), SfSymbol.MusicNoteList) }
                    } else {
                        items(
                            playlists,
                            key = { "playlist-${it.id}" },
                            contentType = { "playlist" },
                        ) { playlist ->
                            LibraryPlaylistRow(playlist) { onPlaylistClick(playlist.id) }
                        }
                    }
                }

                LibraryPage.Podcasts -> {
                    podcastViewModel?.let { viewModel ->
                        podcastState?.let { state ->
                            libraryPodcastItems(navController, state, viewModel)
                        }
                    }
                }

                LibraryPage.Downloads -> libraryDownloadItems(downloadTasks)

                LibraryPage.Cloud -> {
                    cloudState?.let { state ->
                        libraryCloudItems(state, playerConnection)
                    }
                }

                LibraryPage.History -> libraryHistoryItems(history, playerConnection)
            }

            if (selectedPage == LibraryPage.Playlists && albums.isNotEmpty()) {
                item {
                    Text(
                        stringResource(R.string.library_collected_albums),
                        style = IosTypography.headline,
                        color = LocalGlassColors.current.content,
                    )
                }
                items(
                    albums,
                    key = { "album-${it.id}" },
                    contentType = { "album" },
                ) { album ->
                    LibraryMediaRow(
                        album.cover,
                        album.title,
                        album.artist.joinToString(" / ") { it.name },
                        onClick = { onAlbumClick(album.id.toString()) },
                    )
                }
            }
        }
    }

    StandaloneTrackActionOverlay(
        overlay = currentOverlay,
        onDismiss = { currentOverlay = OverlayState.None },
        onUpdateOverlay = { currentOverlay = it },
    )
}

@Composable
private fun LibrarySongRow(
    song: MediaMetadata,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
) {
    LibraryMediaRow(
        image = song.coverUrl,
        title = song.title,
        subtitle = song.artists.joinToString(" / ") { it.name },
        onClick = onClick,
        trailing = {
            Box(
                Modifier.size(44.dp).clip(ContinuousRoundedRectangle(22.dp)).clickable(
                    interactionSource = null,
                    indication = null,
                    onClick = onMoreClick,
                ),
                contentAlignment = Alignment.Center,
            ) {
                SfIcon(
                    "ellipsis",
                    stringResource(R.string.more_actions_title),
                    size = 18.dp,
                )
            }
        },
    )
}

@Composable
private fun LibraryPlaylistRow(playlist: Playlist, onClick: () -> Unit) {
    LibraryMediaRow(
        playlist.cover,
        playlist.title,
        stringResource(R.string.playlist_song_count, playlist.count),
        onClick,
    )
}

@Composable
private fun LibraryMediaRow(
    image: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            image,
            null,
            Modifier.size(54.dp).clip(ContinuousRoundedRectangle(9.dp)),
            contentScale = ContentScale.Crop,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = IosTypography.body,
                color = LocalGlassColors.current.content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle,
                style = IosTypography.caption,
                color = LocalGlassColors.current.secondaryContent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        trailing?.invoke() ?: SfIcon(
            "chevron.forward",
            null,
            size = 12.dp,
            tint = LocalGlassColors.current.tertiaryContent,
        )
    }
}

private fun LazyListScope.libraryPodcastItems(
    navController: com.ljyh.mei.ui.navigation.MeiNavigator,
    state: PodcastUiState,
    viewModel: PodcastViewModel,
) {
    val podcasts = state.categoryPodcasts.takeIf { state.selectedCategoryId != null }
        ?: state.home?.personalized.orEmpty()
    if (state.isLoading && state.home == null) {
        item(key = "library-podcast-loading") {
            Box(Modifier.fillMaxWidth()) {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
    state.home?.categories?.takeIf(List<*>::isNotEmpty)?.let { categories ->
        item(key = "library-podcast-categories") {
            Box(
                Modifier.fillMaxWidth().padding(
                    bottom = if (podcasts.isNotEmpty() || !state.isLoading) 12.dp else 0.dp,
                ),
            ) {
                IosScrollableTabRow(
                    items = listOf<Long?>(null).map { it to stringResource(R.string.podcast_for_you) } +
                        categories.map { it.id to it.name },
                    selected = state.selectedCategoryId,
                    onSelected = { id -> if (id == null) viewModel.refresh() else viewModel.selectCategory(id) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
    if (podcasts.isNotEmpty()) {
        groupedLazyItems(
            items = podcasts,
            key = { "podcast-${it.id}" },
            contentType = "podcast",
        ) { podcast, index ->
            IosListRow(
                title = podcast.name,
                subtitle = podcast.host?.nickname ?: podcast.category,
                showTopSeparator = index > 0,
                leading = {
                    AsyncImage(
                        podcast.picUrl,
                        null,
                        Modifier.size(44.dp).clip(ContinuousRoundedRectangle(10.dp)),
                        contentScale = ContentScale.Crop,
                    )
                },
                onClick = {
                    Screen.PodcastDetail.navigate(navController) { addPath(podcast.id.toString()) }
                },
            )
        }
    } else if (!state.isLoading) {
        item(key = "library-podcast-empty") {
            EmptyState(state.error ?: stringResource(R.string.library_empty_songs), SfSymbol.Microphone)
        }
    }
}

private fun LazyListScope.libraryDownloadItems(tasks: List<DownloadTask>) {
    if (tasks.isEmpty()) {
        item(key = "library-download-empty") {
            EmptyState(stringResource(R.string.no_download_tasks, stringResource(R.string.download_filter_all)), SfSymbol.Download)
        }
    } else {
        groupedLazyItems(
            items = tasks,
            key = { "download-${it.songId}" },
            contentType = "download-task",
        ) { task, index ->
            val detail = when (task.status) {
                DownloadStatus.DOWNLOADING -> "${task.progress}%"
                DownloadStatus.PAUSED -> stringResource(R.string.download_paused)
                DownloadStatus.COMPLETED -> stringResource(R.string.download_completed)
                DownloadStatus.FAILED -> stringResource(R.string.download_failed)
                DownloadStatus.PENDING -> stringResource(R.string.download_waiting)
            }
            IosListRow(
                title = task.songTitle.ifBlank { task.songId },
                subtitle = task.songArtist,
                detail = detail,
                showTopSeparator = index > 0,
                leading = {
                    AsyncImage(
                        task.songCover.ifBlank { null },
                        null,
                        Modifier.size(44.dp).clip(ContinuousRoundedRectangle(10.dp)),
                        contentScale = ContentScale.Crop,
                    )
                },
            )
        }
    }
}

private fun LazyListScope.libraryCloudItems(
    state: CloudMusicUiState,
    playerConnection: PlayerConnection?,
) {
    val songs = state.page?.songs.orEmpty()
    when {
        state.isLoading && state.page == null -> item(key = "library-cloud-loading") {
            Box(Modifier.fillMaxWidth()) {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
        songs.isEmpty() -> item(key = "library-cloud-empty") {
            EmptyState(state.error ?: stringResource(R.string.library_empty_songs), SfSymbol.Cloud)
        }
        else -> groupedLazyItems(
            items = songs,
            key = { "cloud-${it.id}" },
            contentType = "cloud-song",
        ) { song, index ->
            IosListRow(
                title = song.name,
                subtitle = listOf(song.artist, song.album).filter(String::isNotBlank).joinToString(" · "),
                showTopSeparator = index > 0,
                leading = {
                    AsyncImage(
                        song.coverUrl,
                        null,
                        Modifier.size(44.dp).clip(ContinuousRoundedRectangle(10.dp)),
                        contentScale = ContentScale.Crop,
                    )
                },
                onClick = {
                    val queue = songs.map { item ->
                        val mediaItem = MediaMetadata(
                            id = item.id,
                            title = item.name,
                            coverUrl = item.coverUrl.orEmpty(),
                            artists = listOf(MediaMetadata.Artist(item.artist.hashCode().toLong(), item.artist)),
                            duration = item.durationMs,
                            album = MediaMetadata.Album(item.album.hashCode().toLong(), item.album),
                        ).toMediaItem()
                        mediaItem.mediaId to mediaItem
                    }
                    playerConnection?.playQueue(ListQueue("library-cloud", "Cloud", queue, index))
                },
            )
        }
    }
}

private fun LazyListScope.libraryHistoryItems(
    history: List<HistoryItem>,
    playerConnection: PlayerConnection?,
) {
    if (history.isEmpty()) {
        item(key = "library-history-empty") {
            EmptyState(stringResource(R.string.no_listening_history), SfSymbol.Clock)
        }
    } else {
        groupedLazyItems(
            items = history,
            key = { "history-${it.historyId}" },
            contentType = "history-item",
        ) { item, index ->
            IosListRow(
                title = item.song.title,
                subtitle = item.song.artist.joinToString(" / "),
                detail = DateUtils.getRelativeTimeSpanString(item.playedAt).toString(),
                showTopSeparator = index > 0,
                leading = {
                    AsyncImage(
                        item.song.cover,
                        null,
                        Modifier.size(44.dp).clip(ContinuousRoundedRectangle(10.dp)),
                        contentScale = ContentScale.Crop,
                    )
                },
                onClick = {
                    playerConnection?.playQueue(
                        ListQueue(
                            id = "library-history",
                            title = "History",
                            items = history.map { it.song.id to null },
                            startIndex = index,
                        ),
                    )
                },
            )
        }
    }
}

@Composable
private fun EmptyState(text: String, symbol: SfSymbol) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 52.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SfIcon(symbol, null, size = 38.dp, tint = LocalGlassColors.current.tertiaryContent)
        Text(
            text,
            style = IosTypography.subheadline,
            color = LocalGlassColors.current.secondaryContent,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}
