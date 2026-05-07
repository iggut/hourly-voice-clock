package com.hourlyvoiceclock.ui.schedulesettings

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hourlyvoiceclock.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleSettingsScreen(
    onBack: () -> Unit,
    viewModel: ScheduleSettingsViewModel = viewModel(
        factory = ScheduleSettingsViewModelFactory(
            LocalContext.current.applicationContext as android.app.Application
        )
    )
) {
    val quietEnabled by viewModel.quietHoursEnabled.collectAsState()
    val quietStart by viewModel.quietStart.collectAsState()
    val quietEnd by viewModel.quietEnd.collectAsState()
    val allowManual by viewModel.allowManualDuringQuiet.collectAsState()
    val exactAlarms by viewModel.exactAlarmsEnabled.collectAsState()
    val canScheduleExact by viewModel.canScheduleExact.collectAsState()
    val needsExactPermission by viewModel.needsExactPermission.collectAsState()
    val notificationLogging by viewModel.notificationLogging.collectAsState()
    val context = LocalContext.current

    // Re-check permission whenever screen becomes visible
    LaunchedEffect(Unit) {
        viewModel.refreshAll()
    }

    // Permission explanation dialog
    var showExactAlarmDialog by remember { mutableStateOf(false) }
    if (showExactAlarmDialog) {
        AlertDialog(
            onDismissRequest = { showExactAlarmDialog = false },
            title = { Text(stringResource(R.string.exact_alarm_permission_title)) },
            text = { Text(stringResource(R.string.exact_alarm_permission_explanation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExactAlarmDialog = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                        } else {
                            context.startActivity(Intent(Settings.ACTION_APPLICATION_SETTINGS))
                        }
                    }
                ) {
                    Text(stringResource(R.string.grant_permission))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExactAlarmDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Notification permission launcher for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /*_granted -> */ }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.schedule_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // ── Exact alarms card ──────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (needsExactPermission)
                        MaterialTheme.colorScheme.errorContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.exact_alarms),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                when {
                                    needsExactPermission -> stringResource(R.string.permission_denied)
                                    exactAlarms && canScheduleExact -> stringResource(R.string.active)
                                    exactAlarms -> stringResource(R.string.pending_permission)
                                    else -> stringResource(R.string.inactive)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = when {
                                    needsExactPermission -> MaterialTheme.colorScheme.error
                                    exactAlarms && canScheduleExact -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                        Switch(
                            checked = exactAlarms,
                            onCheckedChange = { enabled ->
                                if (enabled && !canScheduleExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    // Show explanation dialog first
                                    showExactAlarmDialog = true
                                } else {
                                    viewModel.setExactAlarmsEnabled(enabled)
                                }
                            }
                        )
                    }

                    if (needsExactPermission) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            stringResource(R.string.exact_alarm_permission_explanation),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                                } else {
                                    context.startActivity(Intent(Settings.ACTION_APPLICATION_SETTINGS))
                                }
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(stringResource(R.string.open_alarm_permission))
                        }
                    }
                }
            }

            // ── Quiet hours card ───────────────────────────────────────────────
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.quiet_hours),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Switch(
                            checked = quietEnabled,
                            onCheckedChange = { viewModel.setQuietHoursEnabled(it) }
                        )
                    }

                    if (quietEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // Start time row
                        ClickableRow(
                            label = stringResource(R.string.quiet_hours_start),
                            value = formatTime(quietStart),
                            onClick = {
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute ->
                                        viewModel.setQuietStart(java.time.LocalTime.of(hour, minute))
                                    },
                                    quietStart.hour,
                                    quietStart.minute,
                                    false
                                ).show()
                            }
                        )

                        // End time row
                        ClickableRow(
                            label = stringResource(R.string.quiet_hours_end),
                            value = formatTime(quietEnd),
                            onClick = {
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute ->
                                        viewModel.setQuietEnd(java.time.LocalTime.of(hour, minute))
                                    },
                                    quietEnd.hour,
                                    quietEnd.minute,
                                    false
                                ).show()
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.allow_manual_during_quiet),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Switch(
                                checked = allowManual,
                                onCheckedChange = { viewModel.setAllowManualDuringQuiet(it) }
                            )
                        }
                    }
                }
            }

            // ── Notifications card ─────────────────────────────────────────────
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.notification_logging),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                stringResource(R.string.notification_logging_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = notificationLogging,
                            onCheckedChange = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    notificationLogging == false
                                ) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                viewModel.setNotificationLogging(it)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ClickableRow(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatTime(time: java.time.LocalTime): String {
    val hour = time.hour % 12
    val displayHour = if (hour == 0) 12 else hour
    val amPm = if (time.hour < 12) "AM" else "PM"
    return String.format("%d:%02d %s", displayHour, time.minute, amPm)
}
