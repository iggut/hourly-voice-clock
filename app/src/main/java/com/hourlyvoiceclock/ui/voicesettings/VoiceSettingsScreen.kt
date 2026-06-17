package com.hourlyvoiceclock.ui.voicesettings

import android.content.Intent
import com.hourlyvoiceclock.util.openTtsEngineSettings
import com.hourlyvoiceclock.util.openTtsSettings
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hourlyvoiceclock.R
import com.hourlyvoiceclock.tts.VoiceInfo
import com.hourlyvoiceclock.tts.local.VoiceModel
import com.hourlyvoiceclock.ui.theme.GlassBgLight
import com.hourlyvoiceclock.ui.theme.GlassBgDark
import com.hourlyvoiceclock.ui.theme.GlassBorderLight
import com.hourlyvoiceclock.ui.theme.GlassBorderDark
import com.hourlyvoiceclock.ui.theme.LightBgStart
import com.hourlyvoiceclock.ui.theme.LightBgEnd
import com.hourlyvoiceclock.ui.theme.DarkBgStart
import com.hourlyvoiceclock.ui.theme.DarkBgEnd

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSettingsScreen(
    onBack: () -> Unit,
    onNavigateToLocalVoices: () -> Unit = {},
    viewModel: VoiceSettingsViewModel = viewModel(
        factory = VoiceSettingsViewModelFactory(
            LocalContext.current.applicationContext as android.app.Application
        )
    )
) {
    val normalVoicesByLocale by viewModel.normalVoicesByLocale.collectAsState()
    val selectedVoice by viewModel.selectedVoiceName.collectAsState()
    val pitch by viewModel.pitch.collectAsState()
    val speechRate by viewModel.speechRate.collectAsState()
    val hasMultiple by viewModel.hasMultipleVoices.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val selectedPresetId by viewModel.selectedPresetId.collectAsState()
    val engines by viewModel.engines.collectAsState()
    val selectedEnginePackage by viewModel.selectedEnginePackage.collectAsState()
    val isEspeakNgSelected by viewModel.isEspeakNgSelected.collectAsState()
    val downloadedLocalModels by viewModel.downloadedLocalModels.collectAsState()
    val selectedLocalModelId by viewModel.selectedLocalModelId.collectAsState()
    val context = LocalContext.current

    val isDark = isSystemInDarkTheme()
    val bgGradient = Brush.verticalGradient(
        colors = listOf(
            if (isDark) DarkBgStart else LightBgStart,
            if (isDark) DarkBgEnd else LightBgEnd
        )
    )

    // Refresh the list of on-device voices whenever the screen
    // becomes active. The user can navigate to the Local Voices
    // screen, download or delete a model, then return here — we
    // need the new list to be visible on the way back.
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.refreshDownloadedLocalModels()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(R.string.voice_settings), fontWeight = FontWeight.Bold, fontSize = 20.sp) },
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
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                // Speech Engines Section
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Preferred Speech Engine",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.ExtraBold
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        engines.forEach { engine ->
                            val isSelected = selectedEnginePackage == engine.packageName
                            val engineBg = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                            } else {
                                if (isDark) GlassBgDark else GlassBgLight
                            }
                            val borderTint = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                if (isDark) GlassBorderDark else GlassBorderLight
                            }
                            val textTint = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onBackground
                            }

                            Card(
                                modifier = Modifier
                                    .width(160.dp)
                                    .clickable {
                                        when {
                                            engine.isInstalled && isSelected -> {
                                                context.openTtsEngineSettings(engine.packageName)
                                            }
                                            engine.isInstalled -> {
                                                viewModel.switchTtsEngine(engine.packageName)
                                            }
                                            else -> {
                                                val playStoreUri = Uri.parse("market://details?id=${engine.packageName}")
                                                val playStoreIntent = Intent(Intent.ACTION_VIEW, playStoreUri).apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                try {
                                                    context.startActivity(playStoreIntent)
                                                } catch (e: Exception) {
                                                    val webUri = Uri.parse("https://play.google.com/store/apps/details?id=${engine.packageName}")
                                                    context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                                                }
                                            }
                                        }
                                    },
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, borderTint),
                                colors = CardDefaults.cardColors(containerColor = engineBg)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = engine.label,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = textTint,
                                        maxLines = 1
                                    )
                                    
                                    if (engine.isInstalled) {
                                        Text(
                                            text = when {
                                                isSelected -> "Active — Tap for Settings"
                                                else -> "Tap to Switch"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = textTint.copy(alpha = 0.7f)
                                        )
                                    } else {
                                        Text(
                                            text = "Tap to Install",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (!hasMultiple) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                                Text(
                                    text = "Single Voice Engine Detected",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.no_voices_found),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    val intent = Intent("com.android.settings.TTS_SETTINGS").apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    try {
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                    }
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(stringResource(R.string.open_tts_settings))
                            }
                        }
                    }
                }

                // Sliders Section
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
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Pitch slider
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(stringResource(R.string.pitch), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Text("%.1f".format(pitch), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(
                                value = pitch,
                                onValueChange = { viewModel.setPitch(it) },
                                valueRange = 0.5f..2.0f,
                                modifier = Modifier.semantics { contentDescription = "Voice Pitch" }
                            )
                        }

                        // Speech rate slider
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(stringResource(R.string.speech_rate), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Text("%.1f".format(speechRate), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(
                                value = speechRate,
                                onValueChange = { viewModel.setSpeechRate(it) },
                                valueRange = 0.5f..2.0f,
                                modifier = Modifier.semantics { contentDescription = "Speech Rate" }
                            )
                        }

                        Button(
                            onClick = { viewModel.previewVoice() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.preview_voice), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }

                // Voice filter: All / Male / Female / Special
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Voices",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        VoiceListFilter.entries.forEach { filter ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { viewModel.setVoiceFilter(filter) },
                                label = {
                                    Text(
                                        text = when (filter) {
                                            VoiceListFilter.ALL -> "All"
                                            VoiceListFilter.MALE -> "Male"
                                            VoiceListFilter.FEMALE -> "Female"
                                            VoiceListFilter.SPECIAL -> "Special"
                                        },
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                val showSpecialSection = selectedFilter == VoiceListFilter.ALL ||
                    selectedFilter == VoiceListFilter.SPECIAL
                val showNormalVoices = selectedFilter != VoiceListFilter.SPECIAL

                if (showSpecialSection) {
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
                        Column(modifier = Modifier.padding(vertical = 10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isEspeakNgSelected) {
                                        "Special & eSpeak NG"
                                    } else {
                                        "Special Voices"
                                    },
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            viewModel.activeSpecialPresets.forEach { preset ->
                                SpecialPresetItem(
                                    preset = preset,
                                    isSelected = selectedPresetId == preset.id,
                                    onSelectPreset = { viewModel.selectVoicePreset(preset) },
                                    onPreviewPreset = { viewModel.selectAndPreviewPreset(preset) }
                                )
                            }
                        }
                    }
                }

                // Local AI voices — downloaded Piper / Sherpa-ONNX models.
                // These appear as first-class voice options so the user can
                // pick one as the active hourly-announcement voice. The
                // announcer routes through LocalTtsEngine when a local
                // model is selected, bypassing the system TTS path.
                if (downloadedLocalModels.isNotEmpty()) {
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
                        Column(modifier = Modifier.padding(vertical = 10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    Icons.Filled.OfflineBolt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Local AI Voices",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            downloadedLocalModels.forEach { model ->
                                LocalVoiceItem(
                                    model = model,
                                    isSelected = selectedLocalModelId == model.id,
                                    onSelect = { viewModel.selectLocalModel(model) },
                                    onPreview = { viewModel.previewLocalModel(model) }
                                )
                            }
                            // A row to clear the local voice selection and
                            // fall back to whichever system voice is set.
                            if (selectedLocalModelId != null) {
                                LocalVoiceClearRow(
                                    onClear = { viewModel.clearLocalModelSelection() }
                                )
                            }
                        }
                    }
                }

                // System voices grouped by locale
                if (showNormalVoices) {
                normalVoicesByLocale.forEach { (localeName, voices) ->
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
                        Column(modifier = Modifier.padding(vertical = 10.dp)) {
                            Text(
                                text = localeName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                            )
                            voices.forEach { voice ->
                                VoiceItem(
                                    voice = voice,
                                    selectedVoice = selectedVoice,
                                    onSelectVoice = viewModel::selectVoice,
                                    onPreviewVoice = viewModel::selectAndPreviewVoice
                                )
                            }
                        }
                    }
                }
                }

                // Help/Info Card for more accents and high-quality voices
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) 
                                         else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                    border = BorderStroke(1.dp, if (isDark) GlassBorderDark else GlassBorderLight)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Get More Accents & Voices",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "To download additional high-quality voices with accents (e.g. UK, Australia, India, Ireland, Canada), open your Android Text-to-Speech settings and select 'Install voice data' under your preferred speech engine.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = {
                                val intent = Intent("com.android.settings.TTS_SETTINGS").apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    try {
                                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                    } catch (_: Exception) {}
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            )
                        ) {
                            Text("Open TTS Settings")
                        }
                    }
                }

                // ── Local AI Voices Card ─────────────────────────────────────────────
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
                                .clickable { onNavigateToLocalVoices() }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudSync,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                                Column {
                                    Text(
                                        "Local AI Voices",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        "On-device voices, no internet needed",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
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
fun SpecialPresetItem(
    preset: SpecialVoicePreset,
    isSelected: Boolean,
    onSelectPreset: () -> Unit,
    onPreviewPreset: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelectPreset)
            .background(backgroundColor)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = null
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 44.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(presetGradientFor(preset.id))
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = preset.displayName,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                ),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Pitch %.1f · Rate %.1f · ${preset.preferredGender}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onPreviewPreset) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Preview ${preset.displayName}",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun VoiceItem(
    voice: VoiceInfo,
    selectedVoice: String?,
    onSelectVoice: (String, String) -> Unit,
    onPreviewVoice: (String, String) -> Unit
) {
    val isSelected = selectedVoice == voice.name
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else Color.Transparent
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectVoice(voice.name, voice.localeTag) }
            .background(backgroundColor)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = null // handled by row click
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            val title = voice.description.ifBlank { voice.name }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Subtitle badges row
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gender badge
                voice.genderLabel?.let { gender ->
                    val badgeColor = if (gender == "Female") Color(0xFFEC4899) else Color(0xFF3B82F6)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = gender,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = badgeColor
                        )
                    }
                }

                // Cloud vs Offline badge
                val (badgeBg, badgeText, badgeIcon) = if (voice.requiresNetwork) {
                    Triple(Color(0xFFEAB308).copy(alpha = 0.15f), Color(0xFFCA8A04), Icons.Default.CloudSync)
                } else {
                    Triple(Color(0xFF10B981).copy(alpha = 0.15f), Color(0xFF059669), Icons.Default.OfflineBolt)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeBg)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(badgeIcon, contentDescription = null, size = 12.dp, tint = badgeText)
                        Text(
                            text = if (voice.requiresNetwork) "Cloud Sync" else "Offline",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = badgeText
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        val title = voice.description.ifBlank { voice.name }
        IconButton(
            onClick = { onPreviewVoice(voice.name, voice.localeTag) }
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Preview voice $title",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun Icon(imageVector: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String?, size: androidx.compose.ui.unit.Dp, tint: Color) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = tint,
        modifier = Modifier.size(size)
    )
}

/**
 * Row in the voice list representing a downloaded on-device Piper
 * voice. Selecting it routes the hourly announcer through
 * [com.hourlyvoiceclock.tts.local.LocalTtsEngine] for this model.
 */
@Composable
fun LocalVoiceItem(
    model: VoiceModel,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onPreview: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f)
    } else {
        Color.Transparent
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .background(backgroundColor)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = null
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = model.displayName,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                ),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // "On-device" badge so the user can tell this voice
                // does not hit a network at announcement time.
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.OfflineBolt,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "On-device",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
                Text(
                    text = model.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        IconButton(onClick = onPreview) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Preview ${model.displayName}",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * "Use system voice" row that appears at the bottom of the Local AI
 * Voices list when a local model is the current selection. Clears
 * [com.hourlyvoiceclock.data.AppSettings.selectedLocalModelId] and
 * hands control back to the system TTS path.
 */
@Composable
private fun LocalVoiceClearRow(onClear: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClear)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = false, onClick = null)
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = "Use system voice",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Fall back to the selected Android TTS engine and voice",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
