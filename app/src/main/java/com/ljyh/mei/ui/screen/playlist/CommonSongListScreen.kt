package com.ljyh.mei.ui.screen.playlist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.compose.LazyPagingItems
import com.ljyh.mei.constants.PlaylistTrackTableHeaderKey
import com.ljyh.mei.data.model.MediaMetadata
import com.ljyh.mei.data.model.room.Like
import com.ljyh.mei.ui.component.player.OverlayState
import com.ljyh.mei.ui.glass.GlassButton
import com.ljyh.mei.ui.glass.IosPinnedListPage
import com.ljyh.mei.ui.glass.IosTypography
import com.ljyh.mei.ui.glass.LocalGlassColors
import com.ljyh.mei.ui.glass.SfIcon
import com.ljyh.mei.ui.glass.SfSymbol
import com.ljyh.mei.ui.component.utils.rememberDeviceInfo
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.model.UiPlaylist
import com.ljyh.mei.ui.screen.playlist.component.PlaylistActionOverlay
import com.ljyh.mei.ui.screen.playlist.component.PlaylistHeader
import com.ljyh.mei.ui.screen.playlist.component.PlaylistShimmer
import com.ljyh.mei.ui.screen.playlist.component.playlistTrackItems
import com.ljyh.mei.utils.rememberPreference

@Composable
fun CommonSongListScreen(
    uiData: UiPlaylist,
    pagingItems: LazyPagingItems<MediaMetadata>? = null,
    isLoading: Boolean,
    onPlayAll: () -> Unit,
    onHeaderAction: () -> Unit,
    onDownload: (() -> Unit)? = null,
    headerActionIcon: ImageVector,
    headerActionLabel: String,
    isSubscribed: Boolean = uiData.isSubscribed,
    onTrackClick: (MediaMetadata, Int) -> Unit,
    onTrackDownload: ((MediaMetadata) -> Unit)? = null,
    onBack: () -> Unit,
    viewModel: PlaylistViewModel = hiltViewModel(),
) {
    val device = rememberDeviceInfo()
    val bottomPadding = LocalPlayerAwareWindowInsets.current
        .asPaddingValues()
        .calculateBottomPadding()
    val allMePlaylist by viewModel.playlist.collectAsState()
    var currentOverlay by remember { mutableStateOf<OverlayState>(OverlayState.None) }
    val playlistTrackTableHeader by rememberPreference(PlaylistTrackTableHeaderKey, false)

    LaunchedEffect(uiData.title, uiData.tracks) {
        if (uiData.title.endsWith("喜欢的音乐")) {
            viewModel.updateAllLike(uiData.tracks.map { Like(it.id.toString()) })
        }
    }

    Box(Modifier.fillMaxSize()) {
        IosPinnedListPage(
            title = uiData.title,
            subtitle = uiData.creatorName.takeIf { it.isNotBlank() },
            showsLargeTitle = false,
            bottomPadding = bottomPadding,
            onNavigateBack = onBack,
        ) {
            if (isLoading) {
                item(key = "playlist-loading") {
                    Box(Modifier.fillMaxWidth().height(620.dp)) {
                        PlaylistShimmer()
                    }
                }
            } else {
                item(key = "playlist-hero") {
                    PlaylistHeader(
                        title = uiData.title,
                        cover = uiData.cover,
                        coverList = uiData.coverList,
                        creator = uiData.creatorName,
                        onPlayAll = onPlayAll,
                        onDownload = onDownload,
                        actionIcon = headerActionIcon,
                        actionLabel = headerActionLabel,
                        count = uiData.count,
                        playCount = uiData.playCount ?: -1L,
                        subscribeCount = uiData.subscriberCount,
                        isSubscribed = isSubscribed,
                        onSubscribed = { onHeaderAction() },
                    )
                }
                playlistTrackItems(
                    pagingItems = pagingItems,
                    staticTracks = uiData.tracks,
                    isTablet = device.isTablet && device.isLandscape,
                    showTableHeader = playlistTrackTableHeader,
                    onTrackClick = onTrackClick,
                    onMoreClick = { currentOverlay = OverlayState.TrackActionMenu(it) },
                )
            }
        }

        PlaylistActionOverlay(
            overlay = currentOverlay,
            isCreator = uiData.isCreator,
            playlistId = uiData.id,
            allMePlaylist = allMePlaylist,
            onDismiss = { currentOverlay = OverlayState.None },
            onUpdateOverlay = { currentOverlay = it },
            onDownloadTrack = onTrackDownload,
            viewModel = viewModel,
        )
    }
}

/** Small reusable iOS action used by callers that still expose a text action. */
@Composable
fun ActionButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    color: androidx.compose.ui.graphics.Color = LocalGlassColors.current.secondaryContent,
) {
    GlassButton(onClick = onClick) {
        SfIcon(SfSymbol.Ellipsis, text, size = 18.dp, tint = color)
        Text(text, style = IosTypography.caption, color = color, modifier = Modifier.padding(start = 6.dp))
    }
}
