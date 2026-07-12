package com.hourlyvoiceclock.ui.localvoices

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.hourlyvoiceclock.tts.local.VoiceCategory
import com.hourlyvoiceclock.tts.local.VoiceModel
import com.hourlyvoiceclock.tts.local.VoiceModelRegistry
import com.hourlyvoiceclock.tts.local.VoiceSourceKind
import com.hourlyvoiceclock.ui.theme.GlassBgDark
import com.hourlyvoiceclock.ui.theme.GlassBgLight
import com.hourlyvoiceclock.ui.theme.LightBgStart
import com.hourlyvoiceclock.ui.theme.LightBgEnd
import com.hourlyvoiceclock.ui.theme.DarkBgStart
import com.hourlyvoiceclock.ui.theme.DarkBgEnd

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalVoiceSettingsScreen(
    onBack: () -> Unit,
    viewModel: LocalVoiceSettingsViewModel = viewModel(
        factory = LocalVoiceSettingsViewModelFactory(
            LocalContext.current.applicationContext as android.app.Application
        )
    )
) {
    val downloadedModels by viewModel.downloadedModels.collectAsState()
    val downloadProgressByModelId by viewModel.downloadProgressByModelId.collectAsState()
    val previewingModelId by viewModel.previewingModelId.collectAsState()
    val selectedLocalModelId by viewModel.selectedLocalModelId.collectAsState()
    val listFilter by viewModel.listFilter.collectAsState()
    val errorsByModelId by viewModel.errorsByModelId.collectAsState()
    val context = LocalContext.current

    val isDark = isSystemInDarkTheme()
    val bgGradient = Brush.verticalGradient(
        colors = listOf(
            if (isDark) DarkBgStart else LightBgStart,
            if (isDark) DarkBgEnd else LightBgEnd
        )
    )
    val downloadedIds = remember(downloadedModels) { downloadedModels.map { it.id }.toSet() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            stringResource(R.string.local_voices_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.local_voices_intro),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                item {
                    PreviewBanner()
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val chipShape = RoundedCornerShape(12.dp)
                        FilterChip(
                            selected = listFilter == LocalVoiceListFilter.ALL,
                            onClick = { viewModel.setListFilter(LocalVoiceListFilter.ALL) },
                            label = {
                                Text(stringResource(R.string.filter_all), fontWeight = FontWeight.Bold)
                            },
                            shape = chipShape
                        )
                        FilterChip(
                            selected = listFilter == LocalVoiceListFilter.INSTALLED,
                            onClick = { viewModel.setListFilter(LocalVoiceListFilter.INSTALLED) },
                            label = {
                                Text(stringResource(R.string.filter_installed), fontWeight = FontWeight.Bold)
                            },
                            shape = chipShape
                        )
                    }
                }

                val categories = listOf(
                    VoiceCategory.STANDARD to R.string.category_standard,
                    VoiceCategory.CHARACTER to R.string.category_character,
                    VoiceCategory.NARRATOR to R.string.category_narrator,
                    VoiceCategory.ACCENT to R.string.category_accents
                )

                categories.forEach { (category, labelRes) ->
                    val voices = VoiceModelRegistry.getVoicesByCategory(category)
                        .filter { model ->
                            listFilter == LocalVoiceListFilter.ALL || model.id in downloadedIds
                        }
                    if (voices.isNotEmpty()) {
                        item {
                            Text(
                                stringResource(labelRes),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        items(voices, key = { it.id }) { model ->
                            val isDownloaded = model.id in downloadedIds
                            val progress = downloadProgressByModelId[model.id]
                            val isDownloading = progress != null
                            val errorMessage = errorsByModelId[model.id]

                            VoiceModelCard(
                                model = model,
                                isDownloaded = isDownloaded,
                                isDownloading = isDownloading,
                                downloadProgress = progress ?: 0f,
                                isPreviewing = previewingModelId == model.id,
                                isActiveHourly = selectedLocalModelId == model.id,
                                errorMessage = errorMessage,
                                onDownload = { viewModel.downloadModel(model) },
                                onCancelDownload = { viewModel.cancelDownload(model.id) },
                                onDelete = { viewModel.deleteModel(model) },
                                onPreview = { viewModel.previewVoice(model) },
                                onClearError = { viewModel.clearError(model.id) },
                                isDark = isDark
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }
}

@Composable
private fun ErrorChip(message: String, onClear: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            stringResource(R.string.dismiss),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier
                .clickable(onClick = onClear, role = Role.Button)
                .padding(horizontal = 4.dp)
        )
    }
}

@Composable
private fun PreviewBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                stringResource(R.string.local_voices_preview_banner),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun VoiceModelCard(
    model: VoiceModel,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    downloadProgress: Float,
    isPreviewing: Boolean,
    isActiveHourly: Boolean,
    errorMessage: String?,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onDelete: () -> Unit,
    onPreview: () -> Unit,
    onClearError: () -> Unit,
    isDark: Boolean
) {
    val context = LocalContext.current
    val displayName = stringResource(model.displayNameRes)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) GlassBgDark else GlassBgLight
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            displayName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isActiveHourly) {
                            Text(
                                stringResource(R.string.active),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        stringResource(model.descriptionRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            VoiceModelRegistry.formatSize(context, model.sizeBytes),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        SourceBadge(model)
                    }
                }

                if (isDownloaded) {
                    Row {
                        IconButton(onClick = onPreview) {
                            Icon(
                                if (isPreviewing) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = if (isPreviewing) {
                                    stringResource(R.string.stop)
                                } else {
                                    stringResource(R.string.preview)
                                },
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        var showDeleteDialog by remember { mutableStateOf(false) }

                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.delete),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }

                        if (showDeleteDialog) {
                            androidx.compose.material3.AlertDialog(
                                onDismissRequest = { showDeleteDialog = false },
                                containerColor = com.hourlyvoiceclock.ui.theme.dialogContainerColor(),
                                titleContentColor = com.hourlyvoiceclock.ui.theme.dialogContentColor(),
                                textContentColor = com.hourlyvoiceclock.ui.theme.dialogContentColor(),
                                title = {
                                    Text(
                                        stringResource(R.string.delete_voice_model_title),
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                text = {
                                    Text(stringResource(R.string.delete_voice_model_confirm, displayName))
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            showDeleteDialog = false
                                            onDelete()
                                        }
                                    ) {
                                        Text(
                                            stringResource(R.string.delete),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteDialog = false }) {
                                        Text(stringResource(R.string.cancel))
                                    }
                                }
                            )
                        }
                    }
                }
            }

            if (isDownloading) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { downloadProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(
                            R.string.download_progress_percent,
                            (downloadProgress * 100).toInt()
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = onCancelDownload) {
                        Text(stringResource(R.string.cancel_download))
                    }
                }
            } else if (!isDownloaded) {
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ErrorChip(message = errorMessage, onClear = onClearError)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onDownload,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (errorMessage != null) {
                            stringResource(R.string.retry_download)
                        } else {
                            stringResource(R.string.download)
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ErrorChip(message = errorMessage, onClear = onClearError)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    if (isActiveHourly) {
                        stringResource(R.string.installed_selected_hourly)
                    } else {
                        stringResource(R.string.installed)
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SourceBadge(model: VoiceModel) {
    val label = when {
        model.personalTestingOnly || model.sourceKind == VoiceSourceKind.COMMUNITY ->
            stringResource(R.string.badge_community_personal_testing)
        else -> stringResource(R.string.badge_official)
    }
    val color = if (model.personalTestingOnly || model.sourceKind == VoiceSourceKind.COMMUNITY) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.primary
    }
    Text(
        label,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}
