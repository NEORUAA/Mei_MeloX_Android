package com.ljyh.mei.ui.screen.search

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.ljyh.mei.data.model.api.SearchResult
import com.ljyh.mei.data.model.api.toAlbum
import com.ljyh.mei.data.model.api.toMediaData
import com.ljyh.mei.data.model.api.toPlaylist
import com.ljyh.mei.data.model.toMediaItem
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.playback.queue.ListQueue
import com.ljyh.mei.ui.component.item.AlbumItem
import com.ljyh.mei.ui.component.item.ArtistItem
import com.ljyh.mei.ui.component.item.PlaylistItem
import com.ljyh.mei.ui.component.item.Track
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.screen.Screen
import androidx.compose.ui.res.stringResource
import coil3.compose.AsyncImage
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.ljyh.mei.R
import com.ljyh.mei.ui.glass.GlassCard
import com.ljyh.mei.ui.glass.GlassButton
import com.ljyh.mei.ui.glass.GlassEmphasis
import com.ljyh.mei.ui.glass.GlassIconButton
import com.ljyh.mei.ui.glass.IosTopBarStyle
import com.ljyh.mei.ui.glass.IosTopToolbar
import com.ljyh.mei.ui.glass.SfIcon
import com.ljyh.mei.ui.glass.SfSymbol
import com.ljyh.mei.ui.component.player.OverlayState
import com.ljyh.mei.ui.screen.playlist.component.StandaloneTrackActionOverlay
import com.ljyh.mei.constants.PodcastsEnabledKey
import com.ljyh.mei.utils.rememberPreference

@OptIn(UnstableApi::class)
@Composable
fun SearchResultScreen(
    query: String,
    type: Int,
    viewModel: SearchViewModel = hiltViewModel()
) {
    // 状态收集
    val searchState by viewModel.searchResult.collectAsState()
    val selectedType by viewModel.currentTab.collectAsState()

    // 初始化逻辑：只需在进入界面（或 query 改变）时调用一次
    LaunchedEffect(query, type) {
        viewModel.onSearchInit(query, type)
    }

    val playerConnection = LocalPlayerConnection.current
    val navController = LocalNavController.current
    var currentOverlay by remember { mutableStateOf<OverlayState>(OverlayState.None) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = LocalPlayerAwareWindowInsets.current
                .add(WindowInsets(top = 4.dp))
                .asPaddingValues()
        ) {
        item {
            IosTopToolbar(
                title = query,
                style = IosTopBarStyle.Default,
                navigation = {
                    GlassIconButton(onClick = navController::navigateUp) {
                        SfIcon(
                            SfSymbol.ChevronBack,
                            stringResource(R.string.navigation_back),
                            mirrored = true,
                        )
                    }
                },
            )
        }
        // Tab 过滤器
        item {
            SearchTypeFilterRow(
                selected = selectedType,
                onSelect = { viewModel.onTabChange(it) }
            )
        }

        // 结果内容区
        when (val result = searchState) {
            is Resource.Loading -> {
                item {
                    LoadingView()
                }
            }
            is Resource.Error -> {
                item {
                    ErrorView(message = result.message)
                }
            }
            is Resource.Success -> {
                SearchResultList(
                    data = result.data,
                    type = selectedType,
                    navController = navController,
                    onSongMore = { currentOverlay = OverlayState.TrackActionMenu(it) },
                    onSongClick = { songs, index ->
                        playerConnection?.playQueue(
                            ListQueue(
                                id = "SearchQueue-$query", // 加上 query 避免 ID 重复
                                title = "搜索: $query",
                                items = songs.map { s -> s.id.toString() to s.toMediaData().toMediaItem() },
                                startIndex = index,
                                position = 0
                            )
                        )
                    }
                )
            }
        }
        }
        StandaloneTrackActionOverlay(
            overlay = currentOverlay,
            onDismiss = { currentOverlay = OverlayState.None },
            onUpdateOverlay = { currentOverlay = it },
        )
    }
}

// --- 抽离出来的子组件，使主代码更整洁 ---

@Composable
fun SearchTypeFilterRow(
    selected: SearchType,
    onSelect: (SearchType) -> Unit
) {
    val podcastsEnabled by rememberPreference(PodcastsEnabledKey, true)
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            SearchType.entries.filter {
                it != SearchType.History && (it != SearchType.Podcast || podcastsEnabled)
            },
            key = SearchType::name,
        ) { type ->
            GlassButton(
                onClick = { onSelect(type) },
                emphasis = if (selected == type) GlassEmphasis.Prominent else GlassEmphasis.Regular,
            ) {
                Text(stringResource(type.labelRes))
            }
        }
    }
}

@Composable
fun LoadingView() {
    Box(
        Modifier
            .fillMaxSize()
            .padding(top = 50.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorView(message: String) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(top = 50.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(stringResource(R.string.load_failed_message, message))
    }
}

fun androidx.compose.foundation.lazy.LazyListScope.SearchResultList(
    data: SearchResult,
    type: SearchType,
    navController: NavController,
    onSongMore: (com.ljyh.mei.data.model.MediaMetadata) -> Unit,
    onSongClick: (List<SearchResult.Result.Song>, Int) -> Unit
) {
    when (type) {
        SearchType.Song -> {
            val songs = data.result.songs ?: emptyList()
            if (songs.isEmpty()) item { EmptyView() }
            items(songs) { song ->
                Track(
                    track = song.toMediaData(),
                    onClick = {
                        // 找到当前点击歌曲的 index
                        val index = songs.indexOfFirst { it.id == song.id }
                        onSongClick(songs, maxOf(0, index))
                    },
                    onMoreClick = { onSongMore(song.toMediaData()) }
                )
            }
        }
        SearchType.Artist -> {
            val artists = data.result.artists ?: emptyList()
            if (artists.isEmpty()) item { EmptyView() }
            items(artists) { artist ->
                ArtistItem(artist = artist, onClick = {
                    Screen.Artist.navigate(navController) {
                        addPath(artist.id.toString())
                    }
                })
            }
        }
        SearchType.Album -> {
            val albums = data.result.albums ?: emptyList()
            if (albums.isEmpty()) item { EmptyView() }
            items(albums) { album ->
                AlbumItem(album = album.toAlbum(), onClick = {
                    Screen.Album.navigate(navController) {
                        addPath(album.id.toString())
                    }
                })
            }
        }
        SearchType.Playlist -> {
            val playlists = data.result.playlists ?: emptyList()
            if (playlists.isEmpty()) item { EmptyView() }
            items(playlists) { playlist ->
                PlaylistItem(
                    playlist = playlist.toPlaylist(),
                    onClick = {
                        Screen.PlayList.navigate(navController) {
                            addPath(playlist.id.toString())
                        }
                    }
                )
            }
        }
        SearchType.Podcast -> {
            val podcasts = data.result.podcasts.orEmpty()
            if (podcasts.isEmpty()) item { EmptyView() }
            items(podcasts) { podcast ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
                    onClick = {
                        Screen.PodcastDetail.navigate(navController) { addPath(podcast.id.toString()) }
                    },
                ) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = podcast.picUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(58.dp).clip(RoundedCornerShape(14.dp)),
                        )
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(podcast.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                listOfNotNull(podcast.host?.nickname, podcast.category).filter(String::isNotBlank).joinToString(" · "),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
        else -> {}
    }
}

@Composable
fun EmptyView() {
    Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
        Text(
            stringResource(R.string.no_search_results),
            color = com.ljyh.mei.ui.glass.LocalGlassColors.current.secondaryContent,
        )
    }
}
