package com.ljyh.mei.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ljyh.mei.R
import com.ljyh.mei.constants.DownloadQuality
import com.ljyh.mei.ui.glass.GlassButton
import com.ljyh.mei.ui.glass.GlassEmphasis
import com.ljyh.mei.ui.glass.IosAlertSurface
import com.ljyh.mei.ui.glass.IosGroupedList
import com.ljyh.mei.ui.glass.IosListRow

@Composable
fun DownloadConfirmDialog(
    currentQuality: DownloadQuality,
    downloadPath: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onGoToSettings: () -> Unit,
    onGoToDownloadManage: () -> Unit = {}
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        IosAlertSurface(
            title = stringResource(R.string.download_confirm_title),
            modifier = Modifier.padding(horizontal = 24.dp),
        ) {
            IosGroupedList {
                IosListRow(
                    title = stringResource(R.string.download_current_quality),
                    subtitle = currentQuality.description,
                    detail = currentQuality.label,
                    showTopSeparator = false,
                )
                IosListRow(
                    title = stringResource(R.string.download_save_location),
                    subtitle = downloadPath,
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
            ) {
                GlassButton(onClick = onGoToSettings, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.download_change_settings))
                }
                GlassButton(onClick = onGoToDownloadManage, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.download_view_queue))
                }
            }
            GlassButton(
                onClick = onConfirm,
                emphasis = GlassEmphasis.Prominent,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Text(stringResource(R.string.download_start))
            }
        }
    }
}
