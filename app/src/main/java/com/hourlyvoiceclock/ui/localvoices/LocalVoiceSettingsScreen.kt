package com.hourlyvoiceclock.ui.localvoices

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hourlyvoiceclock.R
import com.hourlyvoiceclock.tts.local.VoiceCategory
import com.hourlyvoiceclock.tts.local.VoiceModel
import com.hourlyvoiceclock.tts.local.VoiceModelRegistry
import com.hourlyvoiceclock.tts.local.VoiceSourceKind
import com.hourlyvoiceclock.ui.components.GlassCard
import com.hourlyvoiceclock.ui.components.GlassFilterChip
import com.hourlyvoiceclock.ui.components.GlassFilterChipRow
import com.hourlyvoiceclock.ui.components.GlassInfoBanner
import com.hourlyvoiceclock.ui.components.GlassScreen
import com.hourlyvoiceclock.ui.components.OpaqueAlertDialog
import com.hourlyvoiceclock.ui.components.SectionHeader
import com.hourlyvoiceclock.ui.components.SoftStatusBadge
import com.hourlyvoiceclock.ui.components.glassPagePadding
import com.hourlyvoiceclock.ui.theme.GlassShapes
import com.hourlyvoiceclock.ui.theme.GlassSpacing
import com.hourlyvoiceclock.ui.theme.GlassTypography

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

    val downloadedIds = remember(downloadedModels) { downloadedModels.map { it.id }.toSet() }

    val categories = listOf(
        VoiceCategory.STANDARD to R.string.category_standard,
        VoiceCategory.CHARACTER to R.string.category_character,
        VoiceCategory.NARRATOR to R.string.category_narrator,
        VoiceCategory.ACCENT to R.string.category_accents
    )

    val visibleByCategory = categories.map { (category, labelRes) ->
        val voices = VoiceModelRegistry.getVoicesByCategory(category)
            .filter { model ->
                listFilter == LocalVoiceListFilter.ALL || model.id in downloadedIds
            }
        Triple(category, labelRes, voices)
    }
    val hasVisibleVoices = visibleByCategory.any { it.third.isNotEmpty() }

    GlassScreen(
        title = stringResource(R.string.local_voices_title),
        onBack = onBack
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .glassPagePadding(padding),
            verticalArrangement = Arrangement.spacedBy(GlassSpacing.ListGap)
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
                GlassInfoBanner(
                    text = stringResource(R.string.local_voices_preview_banner),
                    icon = Icons.Default.Info,
                    iconContentDescription = stringResource(R.string.a11y_info_icon)
                )
            }

            item {
                GlassFilterChipRow {
                    GlassFilterChip(
                        selected = listFilter == LocalVoiceListFilter.ALL,
                        onClick = { viewModel.setListFilter(LocalVoiceListFilter.ALL) },
                        label = stringResource(R.string.filter_all)
                    )
                    GlassFilterChip(
                        selected = listFilter == LocalVoiceListFilter.INSTALLED,
                        onClick = { viewModel.setListFilter(LocalVoiceListFilter.INSTALLED) },
                        label = stringResource(R.string.filter_installed)
                    )
                }
            }

            if (!hasVisibleVoices && listFilter == LocalVoiceListFilter.INSTALLED) {
                item {
                    GlassCard(shape = GlassShapes.Item) {
                        Text(
                            stringResource(R.string.local_voices_installed_empty),
                            style = GlassTypography.cardSubtitle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            visibleByCategory.forEach { (_, labelRes, voices) ->
                if (voices.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = stringResource(labelRes),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(voices, key = { it.id }) { model ->
                        val progress = downloadProgressByModelId[model.id]
                        VoiceModelCard(
                            model = model,
                            isDownloaded = model.id in downloadedIds,
                            isDownloading = progress != null,
                            downloadProgress = progress ?: 0f,
                            isPreviewing = previewingModelId == model.id,
                            isActiveHourly = selectedLocalModelId == model.id,
                            errorMessage = errorsByModelId[model.id],
                            onDownload = { viewModel.downloadModel(model) },
                            onCancelDownload = { viewModel.cancelDownload(model.id) },
                            onDelete = { viewModel.deleteModel(model) },
                            onPreview = { viewModel.previewVoice(model) },
                            onClearError = { viewModel.clearError(model.id) }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(GlassSpacing.BottomSpacer))
            }
        }
    }
}

@Composable
private fun ErrorChip(message: String, onClear: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GlassShapes.Chip)
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
        val dismissLabel = stringResource(R.string.dismiss)
        Text(
            dismissLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier
                .clickable(onClick = onClear, role = Role.Button, onClickLabel = dismissLabel)
                .padding(horizontal = 4.dp)
        )
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
    onClearError: () -> Unit
) {
    val context = LocalContext.current
    val displayName = stringResource(model.displayNameRes)
    var showDeleteDialog by remember { mutableStateOf(false) }

    GlassCard(shape = GlassShapes.Item, contentPadding = 16.dp) {
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
                        SoftStatusBadge(
                            text = stringResource(R.string.active),
                            accent = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
                Text(
                    stringResource(model.descriptionRes),
                    style = GlassTypography.cardSubtitle,
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
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.error
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
                    .clip(GlassShapes.Badge),
                color = MaterialTheme.colorScheme.tertiary,
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
                shape = GlassShapes.Chip
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

    if (showDeleteDialog) {
        OpaqueAlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = stringResource(R.string.delete_voice_model_title),
            text = {
                Text(stringResource(R.string.delete_voice_model_confirm, displayName))
            },
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                showDeleteDialog = false
                onDelete()
            },
            dismissLabel = stringResource(R.string.cancel)
        )
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
    SoftStatusBadge(text = label, accent = color)
}
