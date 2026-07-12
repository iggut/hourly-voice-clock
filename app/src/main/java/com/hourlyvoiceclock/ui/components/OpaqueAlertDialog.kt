package com.hourlyvoiceclock.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.hourlyvoiceclock.ui.theme.dialogContainerColor
import com.hourlyvoiceclock.ui.theme.dialogContentColor

@Composable
fun OpaqueAlertDialog(
    onDismissRequest: () -> Unit,
    title: String,
    text: @Composable () -> Unit,
    confirmLabel: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    dismissLabel: String? = null,
    onDismiss: (() -> Unit)? = null,
    confirmEnabled: Boolean = true
) {
    val container = dialogContainerColor()
    val content = dialogContentColor()
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        containerColor = container,
        titleContentColor = content,
        textContentColor = content,
        title = {
            Text(text = title, fontWeight = FontWeight.Bold)
        },
        text = text,
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = confirmEnabled) {
                Text(confirmLabel, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = if (dismissLabel != null) {
            {
                TextButton(onClick = onDismiss ?: onDismissRequest) {
                    Text(dismissLabel)
                }
            }
        } else {
            null
        }
    )
}
