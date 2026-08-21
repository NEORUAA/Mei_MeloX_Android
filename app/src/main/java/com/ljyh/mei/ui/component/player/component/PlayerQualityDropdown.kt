package com.ljyh.mei.ui.component.player.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyant.capsule.ContinuousRoundedRectangle
import com.ljyh.mei.constants.MusicQuality
import com.ljyh.mei.ui.glass.IosMenuItem
import com.ljyh.mei.ui.glass.IosPopupMenu
import com.ljyh.mei.ui.glass.SfIcon

@Composable
fun PlayerQualityDropdown(
    quality: MusicQuality,
    onQualitySelected: (MusicQuality) -> Unit,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    IosPopupMenu(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        itemCount = MusicQuality.entries.size,
        modifier = modifier.background(Color.White.copy(alpha = 0.15f), ContinuousRoundedRectangle(6.dp)),
        anchor = { onClick ->
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp).clickable(
                    interactionSource = null,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SfIcon(
                    systemName = "waveform",
                    contentDescription = null,
                    tint = color,
                    size = 10.dp,
                    modifier = Modifier.padding(end = 4.dp),
                )
                Text(
                    text = quality.explanation,
                    style = style,
                    color = color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
    ) { childBackdrop, close ->
        MusicQuality.entries.forEach { option ->
            IosMenuItem(
                title = "${option.explanation} · ${option.text}",
                onClick = {
                    onQualitySelected(option)
                    close()
                },
                systemName = if (option == quality) "checkmark" else null,
                backdrop = childBackdrop,
            )
        }
    }
}
