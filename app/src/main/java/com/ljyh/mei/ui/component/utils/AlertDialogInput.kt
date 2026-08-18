package com.ljyh.mei.ui.component.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ljyh.mei.ui.glass.IosAlertButton
import com.ljyh.mei.ui.glass.IosAlertButtonRole
import com.ljyh.mei.ui.glass.IosAlertFieldGroup
import com.ljyh.mei.ui.glass.IosAlertSurface
import com.ljyh.mei.ui.glass.IosAlertTextField
import com.ljyh.mei.ui.glass.LocalGlassColors


@Composable
fun AlertDialogInput(
    showDialog: Boolean,
    title: String,
    label:String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var inputText by remember { mutableStateOf(TextFieldValue()) }

    if (showDialog) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            IosAlertSurface(title = title) {
                IosAlertFieldGroup {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(LocalGlassColors.current.separator),
                    )
                    IosAlertTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = label,
                    )
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    IosAlertButton(
                        text = stringResource(android.R.string.cancel),
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        role = IosAlertButtonRole.Cancel,
                    )
                    IosAlertButton(
                        text = stringResource(android.R.string.ok),
                        onClick = {
                            onConfirm(inputText.text)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
