package com.hourlyvoiceclock.ui.schedulesettings

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hourlyvoiceclock.R
import com.hourlyvoiceclock.scheduler.AlarmPermissionChecker
import com.hourlyvoiceclock.ui.theme.*

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
        viewModel.onResume()
    }

    // Permission explanation dialog with device-specific guidance
    var showExactAlarmDialog by remember { mutableStateOf(false) }
    val deviceGuidance = remember { AlarmPermissionChecker.getDeviceGuidance() }

    if (showExactAlarmDialog) {
        AlertDialog(
            onDismissRequest = { showExactAlarmDialog = false },
            title = { Text(stringResource(R.string.exact_alarm_permission_title), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(stringResource(R.string.exact_alarm_permission_explanation))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        buildString {
                            appendLine("${deviceGuidance.manufacturerLabel} device:")
                            appendLine(deviceGuidance.permissionPath)
                            deviceGuidance.extraNote?.let {
                                appendLine()
                                append(it)
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExactAlarmDialog = false
                        context.startActivity(
                            AlarmPermissionChecker.buildExactAlarmSettingsIntent(context)
                        )
                    }
                ) {
                    Text(stringResource(R.string.grant_permission), fontWeight = FontWeight.Bold)
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

    val isDark = isSystemInDarkTheme()
    val bgGradient = Brush.verticalGradient(
        colors = listOf(
            if (isDark) DarkBgStart else LightBgStart,
            if (isDark) DarkBgEnd else LightBgEnd
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(R.string.schedule_settings), fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                // ── Exact alarms card ──────────────────────────────────────────────
                val cardBorder = if (needsExactPermission) {
                    BorderStroke(1.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                } else {
                    BorderStroke(1.dp, if (isDark) GlassBorderDark else GlassBorderLight)
                }
                
                val cardBg = if (needsExactPermission) {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                } else {
                    if (isDark) GlassBgDark else GlassBgLight
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = cardBorder
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (needsExactPermission)
                                                MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                            else
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (needsExactPermission) Icons.Default.Warning else Icons.Default.Alarm,
                                        contentDescription = null,
                                        tint = if (needsExactPermission) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                }
                                Column {
                                    Text(
                                        stringResource(R.string.exact_alarms),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (needsExactPermission) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        when {
                                            needsExactPermission -> stringResource(R.string.permission_denied)
                                            exactAlarms && canScheduleExact -> stringResource(R.string.active)
                                            exactAlarms -> stringResource(R.string.pending_permission)
                                            else -> stringResource(R.string.inactive)
                                        },
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = when {
                                            needsExactPermission -> MaterialTheme.colorScheme.error
                                            exactAlarms && canScheduleExact -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                            Switch(
                                checked = exactAlarms,
                                onCheckedChange = { enabled ->
                                    if (enabled && !canScheduleExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            // Device-specific helper text
                            Text(
                                buildString {
                                    appendLine("${deviceGuidance.manufacturerLabel} device: ${deviceGuidance.permissionPath}")
                                    deviceGuidance.extraNote?.let {
                                        appendLine()
                                        append(it)
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    context.startActivity(
                                        AlarmPermissionChecker.buildExactAlarmSettingsIntent(context)
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text(stringResource(R.string.open_alarm_permission), color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // ── Quiet hours card ───────────────────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) GlassBgDark else GlassBgLight),
                    border = BorderStroke(1.dp, if (isDark) GlassBorderDark else GlassBorderLight)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LockClock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Column {
                                    Text(
                                        stringResource(R.string.quiet_hours),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (quietEnabled) "Quiet hours active" else "Quiet hours inactive",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = quietEnabled,
                                onCheckedChange = { viewModel.setQuietHoursEnabled(it) }
                            )
                        }

                        if (quietEnabled) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 14.dp),
                                color = if (isDark) GlassBorderDark else GlassBorderLight
                            )

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

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 6.dp),
                                color = if (isDark) GlassBorderDark.copy(alpha = 0.05f) else GlassBorderLight.copy(alpha = 0.05f)
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

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = if (isDark) GlassBorderDark else GlassBorderLight
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    stringResource(R.string.allow_manual_during_quiet),
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) GlassBgDark else GlassBgLight),
                    border = BorderStroke(1.dp, if (isDark) GlassBorderDark else GlassBorderLight)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Column(modifier = Modifier.widthIn(max = 200.dp)) {
                                    Text(
                                        stringResource(R.string.notification_logging),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        stringResource(R.string.notification_logging_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
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

                Spacer(modifier = Modifier.height(30.dp))
            }
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
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurface)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                value,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
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
