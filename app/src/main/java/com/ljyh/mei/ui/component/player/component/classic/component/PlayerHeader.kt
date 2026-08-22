package com.ljyh.mei.ui.component.player.component.classic.component

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kyant.capsule.ContinuousRoundedRectangle
import com.ljyh.mei.R
import com.ljyh.mei.data.model.MediaMetadata
import com.ljyh.mei.ui.glass.SfIcon
import com.ljyh.mei.ui.glass.SfSymbol

@Composable
fun PlayerHeader(
    modifier: Modifier,
    mediaMetadata: MediaMetadata,
    isLiked: Boolean,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    onLikeClick: () -> Unit,
    iconColor: Color = Color.White
) {

    val shadowStyle = Shadow(
        color = Color.Black.copy(alpha = 0.5f),
        offset = Offset(2f, 2f),
        blurRadius = 8f
    )

    Row(
        modifier = modifier
            .statusBarsPadding()
            .fillMaxWidth()
            .clickable(onClick = { onClick() }),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧：标题和副标题
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier
                .weight(1f) // 占据剩余空间
                .padding(end = 8.dp) // 与右侧按钮保持距离
        ) {
            // --- 标题部分 ---
            Text(
                text = mediaMetadata.title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    shadow = shadowStyle,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1,
                color = Color.White,
                overflow = TextOverflow.Ellipsis,
            )

            // --- 副标题部分 (歌手 & 专辑) ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
                    .basicMarquee(iterations = Int.MAX_VALUE)
            ) {
                val subTitleStyle = MaterialTheme.typography.titleMedium.copy(
                    shadow = shadowStyle,
                    color = Color.White.copy(alpha = 0.7f)
                )

                if (mediaMetadata.artists.isNotEmpty()) {
                    Text(
                        text = mediaMetadata.artists.joinToString(", ") { it.name },
                        style = subTitleStyle,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.clip(ContinuousRoundedRectangle(4.dp))
                    )
                }

                // 分隔符
                if (mediaMetadata.artists.isNotEmpty() && mediaMetadata.album.title.isNotEmpty()) {
                    Text(
                        text = " - ",
                        style = subTitleStyle,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                }

                // 专辑部分
                if (mediaMetadata.album.title.isNotEmpty()) {
                    Text(
                        text = mediaMetadata.album.title,
                        style = subTitleStyle,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.clip(ContinuousRoundedRectangle(4.dp))
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 10.dp)
        ) {
            IconButton(
                onClick = onLikeClick,
                modifier = Modifier
                    .size(34.dp),
            ) {
                Box(
                    modifier = Modifier
                        .requiredSize(34.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    SfIcon(
                        symbol = if (isLiked) SfSymbol.StarFilled else SfSymbol.Star,
                        contentDescription = stringResource(R.string.app_tab_library_songs),
                        tint = iconColor.copy(alpha = 0.8f),
                        size = 22.dp,
                        weight = FontWeight.Bold
                    )
                }
            }
            // 右侧：更多按钮
            IconButton(
                onClick = onMoreClick,
                modifier = Modifier
                    .size(34.dp),
            ) {
                Box(
                    modifier = Modifier
                        .requiredSize(34.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    SfIcon(
                        symbol = SfSymbol.Ellipsis,
                        contentDescription = stringResource(R.string.more_actions_title),
                        tint = iconColor.copy(alpha = 0.8f),
                        size = 22.dp,
                    )
                }
            }
        }


    }
}
