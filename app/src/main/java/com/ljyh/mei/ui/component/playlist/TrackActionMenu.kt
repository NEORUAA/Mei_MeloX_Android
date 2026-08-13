package com.ljyh.mei.ui.component.playlist

import androidx.compose.runtime.Composable
import com.ljyh.mei.R
import com.ljyh.mei.data.model.MediaMetadata
import com.ljyh.mei.ui.glass.IosActionSheetContent
import com.ljyh.mei.ui.glass.IosListRow
import com.ljyh.mei.ui.glass.IosModalSheet
import androidx.compose.ui.res.stringResource

@Composable
fun TrackActionMenu(
    targetTrack: MediaMetadata?,
    isCreator: Boolean = false,
    onDismiss: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDownloadTrack: (() -> Unit)? = null,
    onDelete: () -> Unit? = {},
    onCopyId: () -> Unit,
    onCopyName: () -> Unit
) {
    if (targetTrack != null) {
        val addToPlaylistTitle = stringResource(R.string.track_action_add_playlist)
        val downloadTitle = stringResource(R.string.track_action_download)
        val deleteTitle = stringResource(R.string.track_action_delete)
        val copyNameTitle = stringResource(R.string.track_action_copy_name)
        val copyIdTitle = stringResource(R.string.track_action_copy_id)
        IosModalSheet(
            onDismissRequest = onDismiss,
        ) {
            IosActionSheetContent(
                title = targetTrack.title,
                message = targetTrack.artists.joinToString(", ") { it.name },
            ) {
                IosListRow(
                    showTopSeparator = false,
                    systemName = "plus.circle",
                    title = addToPlaylistTitle,
                    onClick = {
                        onDismiss()
                        onAddToPlaylist()
                    },
                )

                onDownloadTrack?.let { downloadFunc ->
                    IosListRow(
                        systemName = "arrow.down.circle",
                        title = downloadTitle,
                        onClick = {
                            onDismiss()
                            downloadFunc()
                        }
                    )
                }

                if (isCreator) {
                    IosListRow(
                        systemName = "trash",
                        title = deleteTitle,
                        onClick = {
                            onDismiss()
                            onDelete()
                        }
                    )
                }
                IosListRow(
                    systemName = "square.on.square",
                    title = copyNameTitle,
                    onClick = { onDismiss(); onCopyName() })
                IosListRow(
                    systemName = "info.circle",
                    title = copyIdTitle,
                    onClick = { onDismiss(); onCopyId() })
            }
        }
    }
}
