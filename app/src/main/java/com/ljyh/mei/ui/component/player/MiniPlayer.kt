package com.ljyh.mei.ui.component.player

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import coil3.compose.AsyncImage
import com.kyant.backdrop.Backdrop
import com.kyant.capsule.ContinuousRoundedRectangle
import com.kyant.shapes.Capsule
import com.ljyh.mei.R
import com.ljyh.mei.constants.MiniPlayerHeight
import com.ljyh.mei.constants.ThumbnailCornerRadius
import com.ljyh.mei.data.model.MediaMetadata
import com.ljyh.mei.extensions.togglePlayPause
import com.ljyh.mei.ui.glass.GlassSurface
import com.ljyh.mei.ui.glass.LocalGlassBackdrop
import com.ljyh.mei.ui.glass.SfIcon
import com.ljyh.mei.ui.glass.SfSymbol
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.utils.smallImage

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun MiniPlayer(
    @Suppress("UNUSED_PARAMETER") position: Long,
    @Suppress("UNUSED_PARAMETER") duration: Long,
    modifier: Modifier = Modifier,
    backdrop: Backdrop = LocalGlassBackdrop.current,
    compactProgress: Float = 0f,
    verticalOffset: Dp = 0.dp,
    onClick: () -> Unit,
    onCoverBoundsChanged: ((Rect) -> Unit)? = null,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val playbackState by playerConnection.playbackState.collectAsState()
    val error by playerConnection.error.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()
    val progress = compactProgress.coerceIn(0f, 1f)
    val horizontalInset = 12.dp + 60.dp * progress
    val nextVisibility = 1f - progress

    GlassSurface(
        modifier = modifier
            .offset(y = verticalOffset)
            .fillMaxWidth()
            .height(MiniPlayerHeight)
            .padding(horizontal = horizontalInset),
        backdrop = backdrop,
        shape = Capsule(),
        refractionHeight = 16.dp,
        refractionAmount = 28.dp,
        opticalHighlightBoost = 0.06f,
        onClick = onClick,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 2.dp),
        ) {
            Box(Modifier.weight(1f)) {
                mediaMetadata?.let {
                    MiniMediaInfo(
                        mediaMetadata = it,
                        error = error,
                        onCoverBoundsChanged = onCoverBoundsChanged,
                        modifier = Modifier.padding(horizontal = 2.dp),
                    )
                }
            }

            IconButton(
                modifier = Modifier.size(40.dp),
                onClick = {
                    if (playbackState == Player.STATE_ENDED) {
                        playerConnection.player.seekTo(0, 0)
                        playerConnection.player.playWhenReady = true
                    } else {
                        playerConnection.player.togglePlayPause()
                    }
                },
            ) {
                SfIcon(
                    symbol = when {
                        playbackState == Player.STATE_ENDED -> SfSymbol.ArrowClockwise
                        isPlaying -> SfSymbol.PauseFilled
                        else -> SfSymbol.PlayFilled
                    },
                    contentDescription = stringResource(
                        if (isPlaying) R.string.player_pause else R.string.player_play,
                    ),
                    tint = MaterialTheme.colorScheme.onSurface,
                    size = 22.dp,
                    weight = FontWeight.SemiBold,
                )
            }

            if (nextVisibility > 0.001f) {
                Box(
                    modifier = Modifier
                        .width(40.dp * nextVisibility)
                        .graphicsLayer {
                            alpha = nextVisibility
                            val scale = 0.82f + 0.18f * nextVisibility
                            scaleX = scale
                            scaleY = scale
                            clip = true
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    IconButton(
                        modifier = Modifier.size(40.dp),
                        enabled = canSkipNext,
                        onClick = playerConnection::seekToNext,
                    ) {
                        SfIcon(
                            symbol = SfSymbol.ForwardFilled,
                            contentDescription = stringResource(R.string.player_next),
                            tint = MaterialTheme.colorScheme.onSurface,
                            size = 22.dp,
                            weight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MiniMediaInfo(
    mediaMetadata: MediaMetadata,
    error: PlaybackException?,
    modifier: Modifier = Modifier,
    onCoverBoundsChanged: ((Rect) -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Box(modifier = Modifier.padding(4.dp)) {
            Spacer(modifier = Modifier.size(32.dp))
            AsyncImage(
                model = mediaMetadata.coverUrl.smallImage(),
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .onGloballyPositioned { coordinates ->
                        onCoverBoundsChanged?.invoke(coordinates.boundsInRoot())
                    }
                    .alpha(0f)
                    .clip(ContinuousRoundedRectangle(ThumbnailCornerRadius)),
            )
            androidx.compose.animation.AnimatedVisibility(
                visible = error != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Box(
                    Modifier
                        .size(32.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = ContinuousRoundedRectangle(ThumbnailCornerRadius),
                        ),
                ) {
                    SfIcon(
                        symbol = SfSymbol.Warning,
                        contentDescription = stringResource(R.string.player_playback_error),
                        tint = MaterialTheme.colorScheme.error,
                        size = 22.dp,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 6.dp),
        ) {
            Text(
                text = mediaMetadata.title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee(),
            )
            Text(
                text = mediaMetadata.artists.joinToString { it.name },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee(),
            )
        }
    }
}
