package com.ljyh.mei.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ljyh.mei.R
import com.ljyh.mei.constants.DownloadQuality
import com.ljyh.mei.ui.glass.IosAlertButton
import com.ljyh.mei.ui.glass.IosAlertButtonRole
import com.ljyh.mei.ui.glass.IosAlertDialog
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
    IosAlertDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.download_confirm_title),
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
            IosAlertButton(
                text = stringResource(R.string.download_change_settings),
                onClick = onGoToSettings,
                modifier = Modifier.weight(1f),
                role = IosAlertButtonRole.Cancel,
            )
            IosAlertButton(
                text = stringResource(R.string.download_view_queue),
                onClick = onGoToDownloadManage,
                modifier = Modifier.weight(1f),
                role = IosAlertButtonRole.Cancel,
            )
        }
        IosAlertButton(
            text = stringResource(R.string.download_start),
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
    }
}
