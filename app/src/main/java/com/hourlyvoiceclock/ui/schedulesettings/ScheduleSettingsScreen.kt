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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.hourlyvoiceclock.scheduler.ExactAlarmState
import com.hourlyvoiceclock.ui.components.GlassCard
import com.hourlyvoiceclock.ui.components.GlassFilterChip
import com.hourlyvoiceclock.ui.components.GlassScreen
import com.hourlyvoiceclock.ui.components.OpaqueAlertDialog
import com.hourlyvoiceclock.ui.components.glassPagePadding
import com.hourlyvoiceclock.ui.theme.GlassBgDark
import com.hourlyvoiceclock.ui.theme.GlassBgLight
import com.hourlyvoiceclock.ui.theme.GlassBorderDark
import com.hourlyvoiceclock.ui.theme.GlassBorderLight
import com.hourlyvoiceclock.ui.theme.GlassShapes
import com.hourlyvoiceclock.ui.theme.GlassSpacing
import com.hourlyvoiceclock.ui.theme.dialogContainerColor
import com.hourlyvoiceclock.ui.theme.dialogContentColor
import com.hourlyvoiceclock.ui.theme.glassBorderColor
import com.hourlyvoiceclock.ui.theme.glassContainerColor

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
    val exactAlarmState by viewModel.exactAlarmState.collectAsState()
    val canScheduleExact = exactAlarmState is ExactAlarmState.Granted
    val needsExactPermission = exactAlarms && exactAlarmState is ExactAlarmState.Denied
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
    // ⚡ Bolt: Use rememberSaveable to prevent UI state loss on configuration changes.
    var showExactAlarmDialog by rememberSaveable { mutableStateOf(false) }
    val deviceGuidance = viewModel.deviceGuidance()

    if (showExactAlarmDialog) {
        OpaqueAlertDialog(
            onDismissRequest = { showExactAlarmDialog = false },
            title = stringResource(R.string.exact_alarm_permission_title),
            text = {
                Column {
                    Text(stringResource(R.string.exact_alarm_permission_explanation))
                    Spacer(modifier = Modifier.height(12.dp))
                    if (deviceGuidance != null) {
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
                }
            },
            confirmLabel = stringResource(R.string.grant_permission),
            onConfirm = {
                showExactAlarmDialog = false
                viewModel.openExactAlarmSettings()
            },
            dismissLabel = stringResource(R.string.cancel)
        )
    }

    // Notification permission launcher for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        viewModel.checkNotificationPermission()
    }

    val isDark = isSystemInDarkTheme()

    GlassScreen(
        title = stringResource(R.string.schedule_settings),
        onBack = onBack
    ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .glassPagePadding(padding)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(GlassSpacing.SectionGap)
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                // ── Exact alarms card ──────────────────────────────────────────────
                val cardBorder = if (needsExactPermission) {
                    BorderStroke(1.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                } else {
                    BorderStroke(1.dp, glassBorderColor())
                }
                
                val cardBg = if (needsExactPermission) {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                } else {
                    glassContainerColor()
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = GlassShapes.Section,
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = cardBorder
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(GlassShapes.Chip)
                                .semantics(mergeDescendants = true) {}
                                .toggleable(
                                    value = exactAlarms,
                                    onValueChange = { enabled ->
                                        viewModel.setExactAlarmsEnabled(enabled)
                                        if (enabled && !canScheduleExact) {
                                            showExactAlarmDialog = true
                                        }
                                    },
                                    role = Role.Switch
                                )
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(GlassShapes.Chip)
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
                                        contentDescription = if (needsExactPermission) stringResource(R.string.a11y_warning) else stringResource(R.string.a11y_clock),
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
                            val guidance = deviceGuidance ?: return@Column
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                stringResource(R.string.exact_alarm_permission_explanation),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            // Device-specific helper text
                            Text(
                                buildString {
                                    appendLine("${guidance.manufacturerLabel} device: ${guidance.permissionPath}")
                                    guidance.extraNote?.let {
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
                                onClick = { viewModel.openExactAlarmSettings() },
                                shape = GlassShapes.Chip,
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
                    shape = GlassShapes.Section,
                    colors = CardDefaults.cardColors(containerColor = glassContainerColor()),
                    border = BorderStroke(1.dp, glassBorderColor())
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(GlassShapes.Chip)
                                .semantics(mergeDescendants = true) {}
                                .toggleable(value = quietEnabled, onValueChange = { viewModel.setQuietHoursEnabled(it) }, role = Role.Switch)
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(GlassShapes.Chip)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LockClock,
                                        contentDescription = stringResource(R.string.a11y_clock),
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
                                        text = if (quietEnabled) stringResource(R.string.quiet_hours_active) else stringResource(R.string.quiet_hours_inactive),
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
                                    .clip(GlassShapes.Chip)
                                    .semantics(mergeDescendants = true) {}
                                    .toggleable(value = allowManual, onValueChange = { viewModel.setAllowManualDuringQuiet(it) }, role = Role.Switch)
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
                                val locale = configuration.locales[0]
                                val errorColor = MaterialTheme.colorScheme.error
                                val errorBgColor = errorColor.copy(alpha = 0.2f)

                                // ⚡ Bolt: Extract shared object instantiations and formatting out of loops
                                val chipColors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = errorBgColor,
                                    selectedLabelColor = errorColor
                                )
                                val baseBoxModifier = remember { Modifier.size(40.dp).clip(CircleShape) }

                                val fullDayNames = remember(locale) {
                                    DayOfWeek.entries.associateWith { day ->
                                        day.getDisplayName(java.time.format.TextStyle.FULL, locale)
                                    }
                                }
                                val narrowDayNames = remember(locale) {
                                    DayOfWeek.entries.associateWith { day ->
                                        day.getDisplayName(java.time.format.TextStyle.NARROW, locale)
                                    }
                                }

                                if (showFullText) {
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
                                                        fullDayNames[day] ?: "",
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

                                    Row(
                                        modifier = layoutModifier,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        for (day in DayOfWeek.entries) {
                                            val isDisabled = day in quietDaysDisabled
                                            val narrowName = narrowDayNames[day] ?: ""

                                            val bgColor = if (isDisabled) errorBgColor else transparentColor
                                            val borderColor = if (isDisabled) errorColor else outlineColor
                                            val textColor = if (isDisabled) errorColor else onSurfaceColor
                                            Box(
                                                modifier = baseBoxModifier
                                                    .clip(CircleShape)
                                                    .background(bgColor)
                                                    .border(
                                                        width = 1.dp,
                                                        color = borderColor,
                                                        shape = CircleShape
                                                    )
                                                    .selectable(selected = !isDisabled, onClick = { viewModel.toggleQuietDay(day, !isDisabled) }),
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
                    BorderStroke(1.dp, glassBorderColor())
                }
                val notifCardBg = if (showPermissionWarning) {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                } else {
                    glassContainerColor()
                }

                var showRationaleDialog by rememberSaveable { mutableStateOf(false) }
                var showSettingsRedirectDialog by rememberSaveable { mutableStateOf(false) }

                if (showRationaleDialog) {
                    OpaqueAlertDialog(
                        onDismissRequest = { showRationaleDialog = false },
                        title = "Notification Permission Required",
                        text = {
                            Text("To log announcements to the notification drawer, this app needs permission to post notifications. Would you like to grant it?")
                        },
                        confirmLabel = "Grant",
                        onConfirm = {
                            showRationaleDialog = false
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        dismissLabel = stringResource(R.string.cancel)
                    )
                }

                if (showSettingsRedirectDialog) {
                    OpaqueAlertDialog(
                        onDismissRequest = { showSettingsRedirectDialog = false },
                        title = "Notification Permission Denied",
                        text = {
                            Text("Notification permissions have been permanently denied. Please enable them in System Settings to use notification logging.")
                        },
                        confirmLabel = "Open Settings",
                        onConfirm = {
                            showSettingsRedirectDialog = false
                            context.openAppNotificationSettings()
                        },
                        dismissLabel = stringResource(R.string.cancel)
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = GlassShapes.Section,
                    colors = CardDefaults.cardColors(containerColor = notifCardBg),
                    border = notifCardBorder
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(GlassShapes.Chip)
                                .semantics(mergeDescendants = true) {}
                                .toggleable(
                                    value = notificationLogging,
                                    onValueChange = { checked ->
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
                                    },
                                    role = Role.Switch
                                )
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(GlassShapes.Chip)
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
                                        contentDescription = if (showPermissionWarning) stringResource(R.string.a11y_warning) else stringResource(R.string.notification_logging),
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
                                    shape = GlassShapes.Chip,
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
                    BorderStroke(1.dp, glassBorderColor())
                }
                val batteryCardBg = glassContainerColor()

                var oemGuidesExpanded by rememberSaveable { mutableStateOf(false) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = GlassShapes.Section,
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
                                        .clip(GlassShapes.Chip)
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
                                        contentDescription = stringResource(R.string.a11y_clock),
                                        tint = if (isOptimized) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                                    )
                                }
                                Column(modifier = Modifier.widthIn(max = 240.dp)) {
                                    Text(
                                        stringResource(R.string.battery_optimization),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        when (batteryStatus) {
                                            ScheduleSettingsViewModel.BatteryOptimizationStatus.UNRESTRICTED -> stringResource(R.string.battery_optimization_unrestricted)
                                            ScheduleSettingsViewModel.BatteryOptimizationStatus.OPTIMIZED -> stringResource(R.string.battery_optimization_optimized)
                                            ScheduleSettingsViewModel.BatteryOptimizationStatus.UNKNOWN -> stringResource(R.string.battery_optimization_unknown)
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
                                shape = GlassShapes.Chip
                            ) {
                                Text("Disable Optimization", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 14.dp),
                            color = if (isDark) GlassBorderDark else GlassBorderLight
                        )

                        val expandedText = stringResource(R.string.expanded)
                        val collapsedText = stringResource(R.string.collapsed)
                        val toggleOemText = stringResource(R.string.toggle_oem_instructions)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(GlassShapes.Chip)
                                .semantics(mergeDescendants = true) {
                                    stateDescription = if (oemGuidesExpanded) expandedText else collapsedText
                                }
                                .clickable(
                                    role = Role.Button,
                                    onClickLabel = toggleOemText,
                                    onClick = { oemGuidesExpanded = !oemGuidesExpanded }
                                )
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.oem_specific_instructions),
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
                                    stringResource(R.string.battery_optimization_samsung),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    stringResource(R.string.battery_optimization_xiaomi),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    stringResource(R.string.battery_optimization_oneplus),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    stringResource(R.string.battery_optimization_huawei),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    stringResource(R.string.battery_optimization_vivo),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(GlassSpacing.BottomSpacer))
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
            .clip(GlassShapes.Chip)
            .semantics(mergeDescendants = true) {}
            .clickable(
                role = Role.Button,
                onClickLabel = label
            ) { onClick() }
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
                contentDescription = stringResource(R.string.cd_navigate),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ⚡ Bolt: Precomputed minute strings to prevent allocations from padStart during recomposition
private val MINUTES_CACHE = Array(60) { it.toString().padStart(2, '0') }

private fun formatTime(time: java.time.LocalTime): String {
    val hour = time.hour % 12
    val displayHour = if (hour == 0) 12 else hour
    val amPm = if (time.hour < 12) "AM" else "PM"
    // ⚡ Bolt: Replaced String.format with string interpolation and cached minutes to prevent format parsing overhead
    return "$displayHour:${MINUTES_CACHE[time.minute]} $amPm"
}
