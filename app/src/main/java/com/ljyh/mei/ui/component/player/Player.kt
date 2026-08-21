package com.ljyh.mei.ui.component.player

import android.os.Build
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import com.ljyh.mei.constants.PlayerStyle
import com.ljyh.mei.constants.PlayerStyleKey
import com.ljyh.mei.ui.component.player.component.applemusic.AppleMusicPlayer
import com.ljyh.mei.ui.component.player.component.classic.ClassicPlayer
import com.ljyh.mei.ui.component.player.overlay.CommonOverlayHandler
import com.ljyh.mei.ui.component.player.overlay.PlayerOverlayHandler
import com.ljyh.mei.ui.component.player.overlay.rememberOverlayHandler
import com.ljyh.mei.ui.component.player.state.PlayerStateContainer
import com.ljyh.mei.ui.component.player.state.rememberPlayerStateContainer
import com.ljyh.mei.ui.component.sheet.BottomSheetState
import com.ljyh.mei.ui.component.utils.rememberDeviceInfo
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.glass.LocalGlassBackdrop
import com.ljyh.mei.ui.glass.LocalGroupedListBackgroundAlpha
import com.ljyh.mei.utils.rememberEnumPreference
import com.ljyh.mei.ui.screen.playlist.PlaylistViewModel
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

/** Publishes the current cover art for the player backdrop's recording stand-in. */
val LocalPlayerBackdropCover = staticCompositionLocalOf<MutableState<ImageBitmap?>?> { null }

@OptIn(UnstableApi::class)
@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun BottomSheetPlayer(
    state: BottomSheetState,
    modifier: Modifier = Modifier,
    compactMiniPlayerProgress: Float,
    miniPlayerVerticalOffset: Dp,
    playerViewModel: PlayerViewModel = hiltViewModel(),
    playlistViewModel: PlaylistViewModel = hiltViewModel(),
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val navController = LocalNavController.current
    val device = rememberDeviceInfo()
    val collapsedBackdrop = LocalGlassBackdrop.current
    val backdropCover = remember { mutableStateOf<ImageBitmap?>(null) }
    // The visible player background is a GLSurfaceView, whose pixels cannot be captured by
    // a Compose layer recording. Record a cover-art stand-in instead so glass sheets above
    // the player sample something faithful to the flowing background.
    val playerBackdrop = rememberLayerBackdrop(
        onDraw = {
            drawContent()
            backdropCover.value?.let { cover ->
                val scale = maxOf(size.width / cover.width, size.height / cover.height)
                val w = cover.width * scale
                val h = cover.height * scale
                withTransform({
                    translate(left = (size.width - w) / 2f, top = (size.height - h) / 2f)
                }) {
                    drawImage(cover, dstSize = IntSize(w.toInt(), h.toInt()))
                }
            }
        },
    )

    // 获取播放器样式
    val playerStyle by rememberEnumPreference(PlayerStyleKey, defaultValue = PlayerStyle.AppleMusic)

    // 创建公共状态容器
    val stateContainer = rememberPlayerStateContainer(
        playerViewModel = playerViewModel,
        playerConnection = playerConnection
    )

    // 创建弹窗处理器
    val overlayHandler = rememberOverlayHandler(
        stateContainer = stateContainer,
        playlistViewModel = playlistViewModel,
        navController = navController
    )

    // 单入口、双实现 - 根据样式渲染不同的播放器
    CompositionLocalProvider(
        LocalPlayerBackdropCover provides backdropCover,
        LocalGlassBackdrop provides playerBackdrop,
    ) {
        when (playerStyle) {
            PlayerStyle.AppleMusic -> {
                // 横屏模式下直接进入经典模式
                if( device.isLandscape){
                    ClassicPlayer(
                        state = state,
                        modifier = modifier,
                        stateContainer = stateContainer,
                        overlayHandler = overlayHandler,
                        collapsedBackdrop = collapsedBackdrop,
                        playerBackdrop = playerBackdrop,
                        compactMiniPlayerProgress = compactMiniPlayerProgress,
                        miniPlayerVerticalOffset = miniPlayerVerticalOffset,
                    )
                }else{
                    AppleMusicPlayer(
                        state = state,
                        modifier = modifier,
                        stateContainer = stateContainer,
                        overlayHandler = overlayHandler,
                        collapsedBackdrop = collapsedBackdrop,
                        playerBackdrop = playerBackdrop,
                        compactMiniPlayerProgress = compactMiniPlayerProgress,
                        miniPlayerVerticalOffset = miniPlayerVerticalOffset,
                    )
                }

            }
            PlayerStyle.Classic -> {
                ClassicPlayer(
                    state = state,
                    modifier = modifier,
                    stateContainer = stateContainer,
                    overlayHandler = overlayHandler,
                    collapsedBackdrop = collapsedBackdrop,
                    playerBackdrop = playerBackdrop,
                    compactMiniPlayerProgress = compactMiniPlayerProgress,
                    miniPlayerVerticalOffset = miniPlayerVerticalOffset,
                )
            }
        }
    }

    // 公共的弹窗处理层
    CompositionLocalProvider(
        LocalGlassBackdrop provides playerBackdrop,
        LocalGroupedListBackgroundAlpha provides 0.55f,
    ) {
        CommonOverlayHandler(
            overlayHandler = overlayHandler,
            stateContainer = stateContainer,
            sheetState = state,
        )
    }
}
