package com.ljyh.mei.ui.component.player.component

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.ljyh.mei.constants.PlayerActionKey
import com.ljyh.mei.playback.PlayMode
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.glass.SfIcon
import com.ljyh.mei.ui.model.PlayerAction
import com.ljyh.mei.utils.TimeUtils.makeTimeString
import com.ljyh.mei.utils.rememberPreference
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import timber.log.Timber

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PlayerActionToolbar(
    modifier: Modifier = Modifier,
    onLyricClick: () -> Unit,
    onPlaylistClick: () -> Unit,
    onSleepTimerClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current

    val (actionString, _) = rememberPreference(
        key = PlayerActionKey,
        defaultValue = PlayerAction.toSettings(PlayerAction.defaultActions)
    )
    val actions = remember(actionString) {
        val actions = PlayerAction.fromSettings(actionString)
        Timber.tag("PlayerActionToolbar").d(actionString)
        actions
    }


    // 播放模式
    val playModeValue by playerConnection.repeatMode.collectAsState()
    val playMode = remember(playModeValue) {
        PlayMode.fromInt(playModeValue) ?: PlayMode.REPEAT_MODE_ALL
    }

    // 睡眠定时器逻辑
    var sleepTimerTimeLeft by remember { mutableLongStateOf(0L) }
    val sleepTimer = playerConnection.service.sleepTimer
    val sleepTimerEnabled = remember(sleepTimer.triggerTime, sleepTimer.pauseWhenSongEnd) {
        sleepTimer.isActive
    }

    LaunchedEffect(sleepTimerEnabled) {
        if (sleepTimerEnabled) {
            while (isActive) {
                sleepTimerTimeLeft = if (sleepTimer.pauseWhenSongEnd) {
                    playerConnection.player.duration - playerConnection.player.currentPosition
                } else {
                    sleepTimer.triggerTime - System.currentTimeMillis()
                }
                delay(1000L)
            }
        }
    }



    Spacer(Modifier.height(16.dp))

    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {

        actions.forEach { action ->
            when (action) {
                PlayerAction.PLAY_MODE -> {
                    ShadowedIconButton(
                        onClick = { playerConnection.switchPlayMode() }
                    ) {
                        val systemName = when (playMode) {
                            PlayMode.REPEAT_MODE_ONE -> "repeat.1"
                            PlayMode.REPEAT_MODE_ALL -> "repeat"
                            PlayMode.SHUFFLE_MODE_ALL -> "shuffle"
                        }
                        AnimatedContent(targetState = systemName, label = "PlayModeIcon") { target ->
                            SfIcon(target, stringResource(com.ljyh.mei.R.string.player_action_play_mode), tint = Color.White)
                        }
                    }
                }

                PlayerAction.QUEUE -> {
                    ShadowedIconButton(
                        onClick = onPlaylistClick
                    ) {
                        SfIcon(action.systemName, stringResource(action.labelRes), tint = Color.White)
                    }
                }

                PlayerAction.LYRICS -> {
                    ShadowedIconButton(
                        onClick = onLyricClick
                    ) {
                        SfIcon(action.systemName, stringResource(action.labelRes), tint = Color.White)
                    }

                }

                PlayerAction.SLEEP_TIMER -> {
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = sleepTimerEnabled,
                            label = "sleepTimer"
                        ) { isEnabled ->
                            if (isEnabled) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(Color.White.copy(alpha = 0.2f))
                                        .clickable(onClick = sleepTimer::clear)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = makeTimeString(sleepTimerTimeLeft),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            } else {
                                // 普通图标状态
                                ShadowedIconButton(
                                    onClick = onSleepTimerClick
                                ) {
                                    SfIcon(action.systemName, stringResource(action.labelRes), tint = Color.White)
                                }
                            }
                        }
                    }
                }

                PlayerAction.ADD_TO_PLAYLIST -> {
                    ShadowedIconButton(
                        onClick = onAddToPlaylistClick
                    ) {
                        SfIcon(action.systemName, stringResource(action.labelRes), tint = Color.White)
                    }
                }

                PlayerAction.DOWNLOAD -> {
                    ShadowedIconButton(
                        onClick = onDownloadClick,
                    ) {
                        SfIcon(action.systemName, stringResource(action.labelRes), tint = Color.White)
                    }
                }

                PlayerAction.MORE -> {
                    ShadowedIconButton(
                        onClick = onMoreClick
                    ) {
                        SfIcon(action.systemName, stringResource(action.labelRes), tint = Color.White)
                    }
                }
            }
        }

    }
}

@Composable
fun ShadowedIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .size(48.dp),
    ) {
        Box(
            modifier = Modifier
        ) {
            content()
        }
    }
}
