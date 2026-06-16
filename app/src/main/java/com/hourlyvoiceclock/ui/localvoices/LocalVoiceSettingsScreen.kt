package com.hourlyvoiceclock.ui.localvoices

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hourlyvoiceclock.tts.local.VoiceCategory
import com.hourlyvoiceclock.tts.local.VoiceModel
import com.hourlyvoiceclock.tts.local.VoiceModelRegistry
import com.hourlyvoiceclock.ui.theme.GlassBgDark
import com.hourlyvoiceclock.ui.theme.GlassBgLight
import com.hourlyvoiceclock.ui.theme.GlassBorderDark
import com.hourlyvoiceclock.ui.theme.GlassBorderLight
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
    val downloadingModel by viewModel.downloadingModel.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val errorsByModelId by viewModel.errorsByModelId.collectAsState()

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
                    title = { Text("Local Voices", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                        "Download and use local AI voices. These run entirely on your device - no internet needed after download.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                item {
                    PreviewBanner()
                }

                val categories = listOf(
                    VoiceCategory.STANDARD to "Standard",
                    VoiceCategory.CHARACTER to "Character",
                    VoiceCategory.NARRATOR to "Narrator",
                    VoiceCategory.ACCENT to "Accents"
                )

                categories.forEach { (category, label) ->
                    val voices = VoiceModelRegistry.getVoicesByCategory(category)
                    if (voices.isNotEmpty()) {
                        item {
                            Text(
                                label,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        items(voices) { model ->
                            val isDownloaded = downloadedModels.any { it.id == model.id }
                            val isDownloading = downloadingModel?.id == model.id
                            val errorMessage = errorsByModelId[model.id]

                            VoiceModelCard(
                                model = model,
                                isDownloaded = isDownloaded,
                                isDownloading = isDownloading,
                                downloadProgress = if (isDownloading) downloadProgress else 0f,
                                isSpeaking = isSpeaking,
                                errorMessage = errorMessage,
                                onDownload = { viewModel.downloadModel(model) },
                                onDelete = { viewModel.deleteModel(model) },
                                onPreview = { viewModel.previewVoice(model) },
                                onStop = { viewModel.stopSpeaking() },
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
            "Dismiss",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier
                .clickable { onClear() }
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
                "Preview build — download works, the \"Preview\" button works once a voice is " +
                    "downloaded. Wiring this into the hourly announcement flow lands in 0.5.x.",
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
    isSpeaking: Boolean,
    errorMessage: String?,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onPreview: () -> Unit,
    onStop: () -> Unit,
    onClearError: () -> Unit,
    isDark: Boolean
) {
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
                    Text(
                        model.displayName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        model.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        VoiceModelRegistry.formatSize(model.sizeBytes),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (isDownloaded) {
                    Row {
                        IconButton(onClick = onPreview) {
                            Icon(
                                if (isSpeaking) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = if (isSpeaking) "Stop" else "Preview",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = onDelete) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
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
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Text(
                    "Downloading... ${(downloadProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                        if (errorMessage != null) "Retry download" else "Download",
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
                    "Installed",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
