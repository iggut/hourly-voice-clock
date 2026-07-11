package com.hourlyvoiceclock.ui.schedulesettings

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import com.hourlyvoiceclock.util.openAppNotificationSettings
import com.hourlyvoiceclock.util.openIgnoreBatteryOptimizationSettings
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Scaffold
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
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
import java.time.DayOfWeek
import com.hourlyvoiceclock.R
import com.hourlyvoiceclock.scheduler.AlarmPermissionChecker
import com.hourlyvoiceclock.ui.theme.GlassBgLight
import com.hourlyvoiceclock.ui.theme.GlassBgDark
import com.hourlyvoiceclock.ui.theme.GlassBorderLight
import com.hourlyvoiceclock.ui.theme.GlassBorderDark
import com.hourlyvoiceclock.ui.theme.LightBgStart
import com.hourlyvoiceclock.ui.theme.LightBgEnd
import com.hourlyvoiceclock.ui.theme.DarkBgStart
import com.hourlyvoiceclock.ui.theme.DarkBgEnd
import com.hourlyvoiceclock.ui.theme.dialogContainerColor
import com.hourlyvoiceclock.ui.theme.dialogContentColor

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    val quietDaysQuietStart by viewModel.quietDaysQuietStart.collectAsState()
    val quietDaysQuietEnd by viewModel.quietDaysQuietEnd.collectAsState()
    val allowManual by viewModel.allowManualDuringQuiet.collectAsState()
    val quietDaysDisabled by viewModel.quietDaysDisabled.collectAsState()
    val exactAlarms by viewModel.exactAlarmsEnabled.collectAsState()
    val canScheduleExact by viewModel.canScheduleExact.collectAsState()
    val needsExactPermission by viewModel.needsExactPermission.collectAsState()
    val notificationLogging by viewModel.notificationLogging.collectAsState()
    val hasNotificationPermission by viewModel.hasNotificationPermission.collectAsState()
    val context = LocalContext.current

    // Re-check permission whenever screen becomes visible
    LaunchedEffect(Unit) {
        viewModel.refreshAll()
    }

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.onResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Permission explanation dialog with device-specific guidance
    var showExactAlarmDialog by remember { mutableStateOf(false) }
    val deviceGuidance = remember { AlarmPermissionChecker.getDeviceGuidance() }

    if (showExactAlarmDialog) {
        AlertDialog(
            onDismissRequest = { showExactAlarmDialog = false },
            containerColor = dialogContainerColor(),
            titleContentColor = dialogContentColor(),
            textContentColor = dialogContentColor(),
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
    ) { _ ->
        viewModel.checkNotificationPermission()
    }

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
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    val enabled = !exactAlarms
                                    viewModel.setExactAlarmsEnabled(enabled)
                                    if (enabled && !canScheduleExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        showExactAlarmDialog = true
                                    }
                                }
                                .padding(vertical = 4.dp),
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
                                onCheckedChange = null
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { viewModel.setQuietHoursEnabled(!quietEnabled) }
                                .padding(vertical = 4.dp),
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
                                onCheckedChange = null
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewModel.setAllowManualDuringQuiet(!allowManual) }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
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
                                    onCheckedChange = null
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = if (isDark) GlassBorderDark else GlassBorderLight
                            )

                            Text(
                                stringResource(R.string.quiet_days_alt_label),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                val configuration = LocalConfiguration.current
                                val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                                val showFullText = isLandscape || maxWidth >= 600.dp

                                val layoutModifier = Modifier.fillMaxWidth()
                                val locale = java.util.Locale.getDefault()
                                val errorColor = MaterialTheme.colorScheme.error
                                val errorBgColor = errorColor.copy(alpha = 0.2f)

                                // ⚡ Bolt: Cache day names to prevent string allocation in loops
                                val dayNamesFull = remember(locale) { DayOfWeek.entries.associateWith { it.getDisplayName(java.time.format.TextStyle.FULL, locale) } }
                                val dayNamesNarrow = remember(locale) { DayOfWeek.entries.associateWith { it.getDisplayName(java.time.format.TextStyle.NARROW, locale) } }

                                if (showFullText) {
                                    val chipColors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = errorBgColor,
                                        selectedLabelColor = errorColor
                                    )
                                    FlowRow(
                                        modifier = layoutModifier,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        for (day in DayOfWeek.entries) {
                                            val isDisabled = day in quietDaysDisabled
                                            FilterChip(
                                                selected = isDisabled,
                                                onClick = { viewModel.toggleQuietDay(day, !isDisabled) },
                                                label = {
                                                    Text(
                                                        dayNamesFull[day] ?: "",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                },
                                                colors = chipColors
                                            )
                                        }
                                    }
                                } else {
                                    val transparentColor = Color.Transparent
                                    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
                                    // ⚡ Bolt: Share modifier instance to reduce allocation overhead
                                    val baseCircleModifier = remember { Modifier.size(40.dp).clip(CircleShape) }

                                    Row(
                                        modifier = layoutModifier,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        for (day in DayOfWeek.entries) {
                                            val isDisabled = day in quietDaysDisabled
                                            val narrowName = dayNamesNarrow[day] ?: ""

                                            val bgColor = if (isDisabled) errorBgColor else transparentColor
                                            val borderColor = if (isDisabled) errorColor else outlineColor
                                            val textColor = if (isDisabled) errorColor else onSurfaceColor
                                            Box(
                                                modifier = baseCircleModifier
                                                    .background(bgColor)
                                                    .border(
                                                        width = 1.dp,
                                                        color = borderColor,
                                                        shape = CircleShape
                                                    )
                                                    .clickable { viewModel.toggleQuietDay(day, !isDisabled) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = narrowName,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = textColor
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                stringResource(R.string.quiet_days_alt_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (quietDaysDisabled.isNotEmpty()) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    color = if (isDark) GlassBorderDark else GlassBorderLight
                                )

                                ClickableRow(
                                    label = stringResource(R.string.quiet_days_alt_start),
                                    value = formatTime(quietDaysQuietStart),
                                    onClick = {
                                        TimePickerDialog(
                                            context,
                                            { _, hour, minute ->
                                                viewModel.setQuietDaysQuietStart(java.time.LocalTime.of(hour, minute))
                                            },
                                            quietDaysQuietStart.hour,
                                            quietDaysQuietStart.minute,
                                            false
                                        ).show()
                                    }
                                )

                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    color = if (isDark) GlassBorderDark.copy(alpha = 0.05f) else GlassBorderLight.copy(alpha = 0.05f)
                                )

                                ClickableRow(
                                    label = stringResource(R.string.quiet_days_alt_end),
                                    value = formatTime(quietDaysQuietEnd),
                                    onClick = {
                                        TimePickerDialog(
                                            context,
                                            { _, hour, minute ->
                                                viewModel.setQuietDaysQuietEnd(java.time.LocalTime.of(hour, minute))
                                            },
                                            quietDaysQuietEnd.hour,
                                            quietDaysQuietEnd.minute,
                                            false
                                        ).show()
                                    }
                                )
                            }
                        }
                    }
                }

                // ── Notifications card ─────────────────────────────────────────────
                val showPermissionWarning = !hasNotificationPermission && notificationLogging && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                val notifCardBorder = if (showPermissionWarning) {
                    BorderStroke(1.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                } else {
                    BorderStroke(1.dp, if (isDark) GlassBorderDark else GlassBorderLight)
                }
                val notifCardBg = if (showPermissionWarning) {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                } else {
                    if (isDark) GlassBgDark else GlassBgLight
                }

                var showRationaleDialog by remember { mutableStateOf(false) }
                var showSettingsRedirectDialog by remember { mutableStateOf(false) }

                if (showRationaleDialog) {
                    AlertDialog(
                        onDismissRequest = { showRationaleDialog = false },
                        containerColor = dialogContainerColor(),
                        titleContentColor = dialogContentColor(),
                        textContentColor = dialogContentColor(),
                        title = { Text("Notification Permission Required", fontWeight = FontWeight.Bold) },
                        text = { Text("To log announcements to the notification drawer, this app needs permission to post notifications. Would you like to grant it?") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showRationaleDialog = false
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                            ) {
                                Text("Grant", fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showRationaleDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                if (showSettingsRedirectDialog) {
                    AlertDialog(
                        onDismissRequest = { showSettingsRedirectDialog = false },
                        containerColor = dialogContainerColor(),
                        titleContentColor = dialogContentColor(),
                        textContentColor = dialogContentColor(),
                        title = { Text("Notification Permission Denied", fontWeight = FontWeight.Bold) },
                        text = { Text("Notification permissions have been permanently denied. Please enable them in System Settings to use notification logging.") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showSettingsRedirectDialog = false
                                    context.openAppNotificationSettings()
                                }
                            ) {
                                Text("Open Settings", fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showSettingsRedirectDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = notifCardBg),
                    border = notifCardBorder
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    val checked = !notificationLogging
                                    if (checked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                                        val activity = context as? androidx.activity.ComponentActivity
                                        val shouldShowRationale = activity?.let {
                                            androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                                                it,
                                                Manifest.permission.POST_NOTIFICATIONS
                                            )
                                        } ?: false

                                        if (shouldShowRationale) {
                                            showRationaleDialog = true
                                        } else {
                                            // Either first request or permanently denied.
                                            // We check settings.notificationLogging's prior state or try launching direct.
                                            // Let's trigger launcher first:
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    }
                                    viewModel.setNotificationLogging(checked)
                                }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (showPermissionWarning)
                                                MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                            else
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (showPermissionWarning) Icons.Default.Warning else Icons.Default.Notifications,
                                        contentDescription = null,
                                        tint = if (showPermissionWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                }
                                Column(modifier = Modifier.widthIn(max = 200.dp)) {
                                    Text(
                                        stringResource(R.string.notification_logging),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (showPermissionWarning) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        stringResource(R.string.notification_logging_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (showPermissionWarning) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = notificationLogging,
                                onCheckedChange = null
                            )
                        }

                        if (showPermissionWarning) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Notifications are disabled. The app cannot post logs to the drawer.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            val activity = context as? androidx.activity.ComponentActivity
                                            val shouldShowRationale = activity?.let {
                                                androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                                                    it,
                                                    Manifest.permission.POST_NOTIFICATIONS
                                                )
                                            } ?: false
                                            
                                            if (shouldShowRationale) {
                                                showRationaleDialog = true
                                            } else {
                                                showSettingsRedirectDialog = true
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Grant Permission", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // ── Battery Optimization Guidance Card ─────────────────────────────────────────────
                val batteryStatus by viewModel.batteryStatus.collectAsState()
                val isOptimized = batteryStatus == ScheduleSettingsViewModel.BatteryOptimizationStatus.OPTIMIZED
                val isUnrestricted = batteryStatus == ScheduleSettingsViewModel.BatteryOptimizationStatus.UNRESTRICTED

                val batteryCardBorder = if (isOptimized) {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                } else {
                    BorderStroke(1.dp, if (isDark) GlassBorderDark else GlassBorderLight)
                }
                val batteryCardBg = if (isDark) GlassBgDark else GlassBgLight

                var oemGuidesExpanded by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = batteryCardBg),
                    border = batteryCardBorder
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
                                            if (isOptimized)
                                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                            else
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LockClock,
                                        contentDescription = null,
                                        tint = if (isOptimized) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                                    )
                                }
                                Column(modifier = Modifier.widthIn(max = 240.dp)) {
                                    Text(
                                        "Battery Optimization",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        when (batteryStatus) {
                                            ScheduleSettingsViewModel.BatteryOptimizationStatus.UNRESTRICTED -> "Unrestricted - High Reliability"
                                            ScheduleSettingsViewModel.BatteryOptimizationStatus.OPTIMIZED -> "Optimized - Delayed Announcements Possible"
                                            ScheduleSettingsViewModel.BatteryOptimizationStatus.UNKNOWN -> "Checking status..."
                                        },
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = when (batteryStatus) {
                                            ScheduleSettingsViewModel.BatteryOptimizationStatus.UNRESTRICTED -> MaterialTheme.colorScheme.primary
                                            ScheduleSettingsViewModel.BatteryOptimizationStatus.OPTIMIZED -> MaterialTheme.colorScheme.secondary
                                            ScheduleSettingsViewModel.BatteryOptimizationStatus.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Android aggressive battery savers suspend background processes. To ensure voice announcements fire exactly at the top of the hour, please set the app's battery usage settings to Unrestricted.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (isOptimized) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    context.openIgnoreBatteryOptimizationSettings()
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Disable Optimization", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 14.dp),
                            color = if (isDark) GlassBorderDark else GlassBorderLight
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { oemGuidesExpanded = !oemGuidesExpanded }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "OEM Specific Instructions",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = if (oemGuidesExpanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (oemGuidesExpanded) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "• Samsung: Settings -> Apps -> Hourly Voice Clock -> Battery -> Choose \"Unrestricted\".",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "• Xiaomi / MIUI: Settings -> Apps -> Manage Apps -> Hourly Voice Clock -> Battery saver -> Choose \"No restrictions\".",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "• OnePlus: Settings -> Apps -> App management -> Hourly Voice Clock -> Battery usage -> Choose \"Allow background activity\".",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "• Huawei: Settings -> Apps -> Apps -> Hourly Voice Clock -> Power usage details -> Launch -> Switch to \"Manage manually\" and enable \"Run in background\".",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "• Vivo: Settings -> Battery -> High background power consumption -> Enable \"Hourly Voice Clock\".",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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
