package com.hourlyvoiceclock.ui.home

import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hourlyvoiceclock.util.openUrl
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.filled.SystemUpdate
import com.hourlyvoiceclock.data.UpdateStatus
import com.hourlyvoiceclock.R
import com.hourlyvoiceclock.data.PhraseStyle
import com.hourlyvoiceclock.data.TimeFormat
import com.hourlyvoiceclock.tts.local.VoiceModelRegistry
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToVoiceSettings: () -> Unit,
    onNavigateToFormatSettings: () -> Unit,
    onNavigateToScheduleSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(LocalContext.current.applicationContext as android.app.Application))
) {
    val timeState by viewModel.timeState.collectAsState()
    val currentDate by viewModel.currentDate.collectAsState()
    val nextAnnouncement by viewModel.nextAnnouncement.collectAsState()
    val quietHoursActive by viewModel.quietHoursActive.collectAsState()
    val hourlyEnabled by viewModel.hourlyEnabled.collectAsState()
    val canSpeakNow by viewModel.canSpeakNow.collectAsState()
    val settings by viewModel.appSettings.collectAsState()
    val updateStatus by viewModel.updateStatus.collectAsState()

    var showUpdatesDialog by remember { mutableStateOf(false) }

    LaunchedEffect(showUpdatesDialog) {
        if (showUpdatesDialog) {
            when (updateStatus) {
                is UpdateStatus.Idle, is UpdateStatus.Error -> viewModel.checkForUpdates(isManual = true)
                else -> Unit
            }
        }
    }

    val context = LocalContext.current
    val currentVersion = remember {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "0.1"
        } catch (e: Exception) {
            "0.1"
        }
    }

    val isDark = isSystemInDarkTheme()
    val bgGradient = Brush.verticalGradient(
        colors = listOf(
            if (isDark) DarkBgStart else LightBgStart,
            if (isDark) DarkBgEnd else LightBgEnd
        )
    )

    // Pulsing animation for seconds indicator
    // When hourlyEnabled && !quietHoursActive: static solid (announcement active)
    // Otherwise: slow 2s pulse to indicate "monitoring in progress"
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            // No topBar — the app title is intentionally not shown on
            // the home screen, and the TopAppBar slot reserves vertical
            // space for it. Render the main column flush with the
            // top edge instead of leaving a 64dp gap where the title
            // used to be.
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                // Modern Clock Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) GlassBgDark else GlassBgLight
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isDark) GlassBorderDark else GlassBorderLight
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 32.dp, horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Clock layout
                        if (timeState.hoursMinutes.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = timeState.hoursMinutes,
                                    style = MaterialTheme.typography.displayLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.SansSerif,
                                        letterSpacing = (-1).sp
                                    ),
                                    fontSize = 64.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))

                                Column(
                                    horizontalAlignment = Alignment.Start,
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(bottom = 12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .graphicsLayer {
                                                    alpha = pulseAlpha
                                                }
                                                .background(MaterialTheme.colorScheme.tertiary)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = timeState.seconds,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontFamily = FontFamily.Monospace,
                                                fontFeatureSettings = "tnum"
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (timeState.amPm.isNotEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = timeState.amPm,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // Placeholder while loading
                            Text(
                                text = "--:--",
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif,
                                    letterSpacing = (-1).sp
                                ),
                                fontSize = 64.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        // Current Date
                        Text(
                            text = currentDate,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Announce Now Button - embedded as footer inside clock card
                        Button(
                            onClick = { viewModel.announceNow() },
                            enabled = canSpeakNow,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            if (canSpeakNow) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (canSpeakNow) stringResource(R.string.announce_now) else stringResource(R.string.initializing_voice),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                // Hourly Announcement Toggle Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) GlassBgDark else GlassBgLight
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isDark) GlassBorderDark else GlassBorderLight
                    )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .toggleable(
                                    value = hourlyEnabled,
                                    onValueChange = { viewModel.toggleHourly(it) },
                                    role = Role.Switch
                                )
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (hourlyEnabled)
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            else
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.NotificationsActive,
                                        contentDescription = null,
                                        tint = if (hourlyEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column {
                                    Text(
                                        text = stringResource(R.string.hourly_announcements),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (hourlyEnabled) "Announcements active" else "Announcements disabled",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = hourlyEnabled,
                                onCheckedChange = null
                            )
                        }
                        if (hourlyEnabled && nextAnnouncement.isNotBlank()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = if (isDark) GlassBorderDark else GlassBorderLight
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = stringResource(R.string.next_announcement, nextAnnouncement),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Quiet hours status
                if (quietHoursActive) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error)
                            )
                            Text(
                                text = stringResource(R.string.quiet_hours_active),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // Dashboard Category Section
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    val voiceSubtitle = when {
                        settings.selectedLocalModelId != null -> "Local: " +
                            (VoiceModelRegistry.availableVoices
                                .firstOrNull { it.id == settings.selectedLocalModelId }
                                ?.displayName
                                ?: settings.selectedLocalModelId)
                        else -> settings.selectedVoiceName ?: "System Default voice"
                    }
                    DashboardCard(
                        title = stringResource(R.string.voice_settings),
                        subtitle = voiceSubtitle,
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        onClick = onNavigateToVoiceSettings
                    )

                    val formatSubtitle = when (settings.timeFormat) {
                        TimeFormat.HOUR_12 -> "12-hour"
                        TimeFormat.HOUR_24 -> "24-hour"
                    } + " • " + when (settings.phraseStyle) {
                        PhraseStyle.SIMPLE -> "Simple style"
                        PhraseStyle.DETAILED -> "Detailed style"
                        PhraseStyle.FRIENDLY -> "Friendly style"
                        PhraseStyle.CUSTOM -> "Custom prefix/suffix"
                    }
                    DashboardCard(
                        title = stringResource(R.string.format_settings),
                        subtitle = formatSubtitle,
                        icon = Icons.Filled.Tune,
                        onClick = onNavigateToFormatSettings
                    )

                    val scheduleSubtitle = buildString {
                        if (settings.quietHoursEnabled) {
                            append("Quiet hours enabled")
                        } else {
                            append("Quiet hours disabled")
                        }
                        if (settings.exactAlarmsEnabled) {
                            append(" • Exact Alarms")
                        }
                    }
                    DashboardCard(
                        title = stringResource(R.string.schedule_settings),
                        subtitle = scheduleSubtitle,
                        icon = Icons.Filled.Schedule,
                        onClick = onNavigateToScheduleSettings
                    )

                    val updateSubtitle = when (val status = updateStatus) {
                        is UpdateStatus.UpdateAvailable -> "Update available • Version ${status.latestVersion}"
                        is UpdateStatus.Checking -> "Checking for updates..."
                        is UpdateStatus.Downloading -> "Downloading... ${status.progress}%"
                        is UpdateStatus.InstallReady -> "Download ready • Tap to install"
                        is UpdateStatus.Installing -> "Installing..."
                        is UpdateStatus.UpToDate -> "Version $currentVersion • Up to date"
                        is UpdateStatus.NoRelease -> "Version $currentVersion • No release published"
                        is UpdateStatus.Error -> "Check failed • Tap to retry"
                        is UpdateStatus.InstallComplete -> "Update installed!"
                        is UpdateStatus.InstallFailed -> "Install failed • Tap to retry"
                        else -> "Version $currentVersion • " + if (settings.autoUpdateEnabled) "Auto-check active" else "Auto-check disabled"
                    }
                    DashboardCard(
                        title = "App Updates",
                        subtitle = updateSubtitle,
                        icon = Icons.Filled.SystemUpdate,
                        onClick = { showUpdatesDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }

        if (showUpdatesDialog) {
            val dialogContainer = dialogContainerColor()
            val dialogContent = dialogContentColor()
            AlertDialog(
                onDismissRequest = { showUpdatesDialog = false },
                containerColor = dialogContainer,
                titleContentColor = dialogContent,
                textContentColor = dialogContent,
                title = {
                    Text(
                        "App Updates",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Current version: v$currentVersion", fontWeight = FontWeight.SemiBold)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .toggleable(
                                    value = settings.autoUpdateEnabled,
                                    onValueChange = { viewModel.setAutoUpdateEnabled(it) },
                                    role = Role.Switch
                                )
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Auto-check for updates", fontWeight = FontWeight.Medium)
                                Text(
                                    "Queries GitHub API on app startup",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.autoUpdateEnabled,
                                onCheckedChange = null
                            )
                        }

                        Text(
                            "When auto-check is enabled, the app contacts the public GitHub Releases API on startup to see if a newer version is available.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        HorizontalDivider(color = if (isDark) GlassBorderDark else GlassBorderLight)

                        when (val status = updateStatus) {
                            is UpdateStatus.Idle -> {
                                Text("Check status: Not checked", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            is UpdateStatus.Checking -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Text("Checking for updates...", color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            is UpdateStatus.UpToDate -> {
                                Text("Your app is up to date!", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                            }
                            is UpdateStatus.NoRelease -> {
                                Text("No published release yet on GitHub.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            is UpdateStatus.UpdateAvailable -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        "New version available: ${status.latestVersion}",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (status.releaseNotes.isNotBlank()) {
                                        Text(
                                            "Release Notes:\n${status.releaseNotes}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 5
                                        )
                                    }
                                    Button(
                                        onClick = { viewModel.downloadAndInstall(status.downloadUrl) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Download & Install", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            is UpdateStatus.Downloading -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        Text(
                                            "Downloading... ${formatBytes(status.bytesDownloaded)} / ${formatBytes(status.totalBytes)}",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { status.progress / 100f },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    Text(
                                        "${status.progress}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    TextButton(
                                        onClick = { viewModel.cancelDownload() },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Cancel", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                            is UpdateStatus.InstallReady -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        "Download complete!",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Button(
                                        onClick = { viewModel.installApk(status.localApkPath) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Install Update", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            is UpdateStatus.Installing -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        Text(
                                            "Installing...",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { status.progress / 100f },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                            is UpdateStatus.InstallComplete -> {
                                Text(
                                    "Installation complete! The app will restart.",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            is UpdateStatus.InstallFailed -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        "Installation failed: ${status.error}",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Button(
                                        onClick = {
                                            val updateStatus = updateStatus
                                            if (updateStatus is UpdateStatus.UpdateAvailable) {
                                                viewModel.downloadAndInstall(updateStatus.downloadUrl)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Retry", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            is UpdateStatus.Error -> {
                                Text("Check failed: ${status.message}", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.checkForUpdates(isManual = true) }
                    ) {
                        Text("Check Now", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showUpdatesDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}


@Composable
fun DashboardCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) GlassBgDark else GlassBgLight
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isDark) GlassBorderDark else GlassBorderLight
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isDark) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        else MaterialTheme.colorScheme.primaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isDark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
        else -> String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024))
    }
}
