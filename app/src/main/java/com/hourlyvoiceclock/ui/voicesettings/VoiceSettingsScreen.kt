package com.hourlyvoiceclock.ui.voicesettings

import android.content.Intent
import com.hourlyvoiceclock.util.openTtsEngineSettings
import com.hourlyvoiceclock.util.openTtsSettings
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.hourlyvoiceclock.ui.components.DashboardCard
import com.hourlyvoiceclock.ui.components.GlassCard
import com.hourlyvoiceclock.ui.components.GlassFilterChip
import com.hourlyvoiceclock.ui.components.GlassFilterChipRow
import com.hourlyvoiceclock.ui.components.GlassInfoBanner
import com.hourlyvoiceclock.ui.components.GlassScreen
import com.hourlyvoiceclock.ui.components.SectionHeader
import com.hourlyvoiceclock.ui.components.SoftStatusBadge
import com.hourlyvoiceclock.ui.components.StatusBadge
import com.hourlyvoiceclock.ui.components.glassPagePadding
import com.hourlyvoiceclock.ui.theme.AccentCloud
import com.hourlyvoiceclock.ui.theme.AccentFemale
import com.hourlyvoiceclock.ui.theme.AccentMale
import com.hourlyvoiceclock.ui.theme.AccentOffline
import com.hourlyvoiceclock.ui.theme.GlassBgDark
import com.hourlyvoiceclock.ui.theme.GlassBgLight
import com.hourlyvoiceclock.ui.theme.GlassBorderDark
import com.hourlyvoiceclock.ui.theme.GlassBorderLight
import com.hourlyvoiceclock.ui.theme.GlassShapes
import com.hourlyvoiceclock.ui.theme.GlassSpacing
import com.hourlyvoiceclock.ui.theme.GlassTypography
import com.hourlyvoiceclock.ui.theme.glassBorderColor
import com.hourlyvoiceclock.ui.theme.glassContainerColor
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween

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
    val specialTagFilter by viewModel.specialTagFilter.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val localVoiceSelected = selectedLocalModelId != null

    val isDark = isSystemInDarkTheme()

    // Refresh the list of on-device voices whenever the screen
    // becomes active. The user can navigate to the Local Voices
    // screen, download or delete a model, then return here — we
    // need the new list to be visible on the way back.
    LaunchedEffect(Unit) {
        viewModel.refreshDownloadedLocalModels()
    }

    LaunchedEffect(userMessage) {
        val message = userMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeUserMessage()
    }

    GlassScreen(
        title = stringResource(R.string.voice_settings),
        onBack = onBack,
        snackbarHostState = snackbarHostState
    ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .glassPagePadding(padding)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(GlassSpacing.SectionGap)
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                // Speech Engines Section
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(title = stringResource(R.string.preferred_speech_engine))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // ⚡ Bolt: Hoist shared styles outside the high-frequency iteration loop to prevent redundant object allocations
                        val selectedEngineBg = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                        val unselectedEngineBg = if (isDark) GlassBgDark else GlassBgLight
                        val selectedBorderTint = MaterialTheme.colorScheme.primary
                        val unselectedBorderTint = if (isDark) GlassBorderDark else GlassBorderLight
                        val selectedTextTint = MaterialTheme.colorScheme.onPrimaryContainer
                        val unselectedTextTint = MaterialTheme.colorScheme.onBackground

                        engines.forEach { engine ->
                            val isSelected = selectedEnginePackage == engine.packageName
                            val engineBg = if (isSelected) selectedEngineBg else unselectedEngineBg
                            val borderTint = if (isSelected) selectedBorderTint else unselectedBorderTint
                            val textTint = if (isSelected) selectedTextTint else unselectedTextTint

                            Card(
                                modifier = Modifier
                                    .width(160.dp)
                                    .selectable(
                                        selected = isSelected,
                                        onClick = {
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
                                        role = Role.RadioButton
                                    ),
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
                                                isSelected -> stringResource(R.string.engine_active_tap_settings)
                                                else -> stringResource(R.string.engine_tap_to_switch)
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = textTint.copy(alpha = 0.7f)
                                        )
                                    } else {
                                        Text(
                                            text = stringResource(R.string.engine_tap_to_install),
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
                                    text = stringResource(R.string.single_voice_engine_detected),
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
                                shape = GlassShapes.Chip
                            ) {
                                Text(stringResource(R.string.open_tts_settings))
                            }
                        }
                    }
                }

                // Sliders Section
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (localVoiceSelected) {
                            Text(
                                text = stringResource(R.string.pitch_rate_system_tts_only),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Pitch slider
                        Column(modifier = Modifier.alpha(if (localVoiceSelected) 0.45f else 1f)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(stringResource(R.string.pitch), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Text("%.1f".format(pitch), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                            }
                            val pitchA11y = stringResource(R.string.voice_pitch_a11y)
                            Slider(
                                value = pitch,
                                onValueChange = { viewModel.setPitch(it) },
                                enabled = !localVoiceSelected,
                                valueRange = 0.5f..2.0f,
                                modifier = Modifier.semantics { contentDescription = pitchA11y }
                            )
                        }

                        // Speech rate slider
                        Column(modifier = Modifier.alpha(if (localVoiceSelected) 0.45f else 1f)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(stringResource(R.string.speech_rate), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Text("%.1f".format(speechRate), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                            }
                            val rateA11y = stringResource(R.string.speech_rate_a11y)
                            Slider(
                                value = speechRate,
                                onValueChange = { viewModel.setSpeechRate(it) },
                                enabled = !localVoiceSelected,
                                valueRange = 0.5f..2.0f,
                                modifier = Modifier.semantics { contentDescription = rateA11y }
                            )
                        }

                        Button(
                            onClick = { viewModel.previewVoice() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = GlassShapes.Item
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.preview_voice), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }

                // Voice filter: All / Male / Female / Special
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(title = stringResource(R.string.voices_section_title))
                    GlassFilterChipRow {
                        VoiceListFilter.entries.forEach { filter ->
                            GlassFilterChip(
                                selected = selectedFilter == filter,
                                onClick = { viewModel.setVoiceFilter(filter) },
                                label = when (filter) {
                                    VoiceListFilter.ALL -> stringResource(R.string.filter_all)
                                    VoiceListFilter.MALE -> stringResource(R.string.filter_male)
                                    VoiceListFilter.FEMALE -> stringResource(R.string.filter_female)
                                    VoiceListFilter.SPECIAL -> stringResource(R.string.filter_special)
                                }
                            )
                        }
                    }
                }

                val showSpecialSection = selectedFilter == VoiceListFilter.ALL ||
                    selectedFilter == VoiceListFilter.SPECIAL ||
                    selectedFilter == VoiceListFilter.MALE ||
                    selectedFilter == VoiceListFilter.FEMALE
                val showNormalVoices = selectedFilter != VoiceListFilter.SPECIAL
                val specialPresetsForFilter = viewModel.activeSpecialPresets
                val showSpecialCard = showSpecialSection && (
                    selectedFilter == VoiceListFilter.ALL ||
                        selectedFilter == VoiceListFilter.SPECIAL ||
                        specialPresetsForFilter.isNotEmpty()
                    )

                if (showSpecialCard) {
                    GlassCard(contentPadding = 0.dp) {
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
                                        stringResource(R.string.special_and_espeak)
                                    } else {
                                        stringResource(R.string.special_voices)
                                    },
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            GlassFilterChipRow(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                            ) {
                                GlassFilterChip(
                                    selected = specialTagFilter == null,
                                    onClick = { viewModel.setSpecialTagFilter(null) },
                                    label = stringResource(R.string.filter_all)
                                )
                                listOf(
                                    SpecialVoiceTag.FUN,
                                    SpecialVoiceTag.CHARACTER,
                                    SpecialVoiceTag.ACCENT
                                ).forEach { tag ->
                                    GlassFilterChip(
                                        selected = specialTagFilter == tag,
                                        onClick = { viewModel.setSpecialTagFilter(tag) },
                                        label = stringResource(specialVoiceTagLabelRes(tag))
                                    )
                                }
                                if (isEspeakNgSelected) {
                                    GlassFilterChip(
                                        selected = specialTagFilter == SpecialVoiceTag.ESPEAK,
                                        onClick = { viewModel.setSpecialTagFilter(SpecialVoiceTag.ESPEAK) },
                                        label = stringResource(specialVoiceTagLabelRes(SpecialVoiceTag.ESPEAK))
                                    )
                                }
                            }

                            viewModel.activeSpecialPresets.forEach { preset ->
                                SpecialPresetItem(
                                    preset = preset,
                                    isSelected = selectedPresetId == preset.id && !localVoiceSelected,
                                    matchedVoiceName = if (selectedPresetId == preset.id) selectedVoice else null,
                                    onSelectPreset = { viewModel.selectVoicePreset(preset) },
                                    onPreviewPreset = { viewModel.selectAndPreviewPreset(preset) }
                                )
                            }
                        }
                    }
                }

                // Local AI voices — downloaded Piper / Sherpa-ONNX models.
                // Hidden on the Special filter so that tab stays focused on presets.
                if (selectedFilter != VoiceListFilter.SPECIAL) {
                    GlassCard(contentPadding = 0.dp) {
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
                                    text = stringResource(R.string.local_ai_voices),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }

                            if (downloadedLocalModels.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.local_ai_empty_in_voice),
                                    style = GlassTypography.cardSubtitle,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                                )
                            } else {
                                downloadedLocalModels.forEach { model ->
                                    LocalVoiceItem(
                                        model = model,
                                        isSelected = selectedLocalModelId == model.id,
                                        onSelect = { viewModel.selectLocalModel(model) },
                                        onPreview = { viewModel.previewLocalModel(model) }
                                    )
                                }
                                if (selectedLocalModelId != null) {
                                    LocalVoiceClearRow(
                                        onClear = { viewModel.clearLocalModelSelection() }
                                    )
                                }
                            }

                            DashboardCard(
                                title = stringResource(R.string.browse_and_download),
                                subtitle = stringResource(R.string.on_device_voices_subtitle),
                                icon = Icons.Default.CloudSync,
                                onClick = onNavigateToLocalVoices,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                // System voices grouped by locale
                if (showNormalVoices) {
                val localeTitleStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                val localeTitleColor = MaterialTheme.colorScheme.primary

                normalVoicesByLocale.forEach { (localeName, voices) ->
                    GlassCard(contentPadding = 0.dp) {
                        Column(modifier = Modifier.padding(vertical = 10.dp)) {
                            Text(
                                text = localeName,
                                style = localeTitleStyle,
                                color = localeTitleColor,
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
                GlassCard(shape = GlassShapes.Dashboard) {
                    Column(
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
                                text = stringResource(R.string.get_more_accents_voices),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = stringResource(R.string.get_more_accents_body),
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
                            shape = GlassShapes.Chip,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            )
                        ) {
                            Text(stringResource(R.string.open_tts_settings_short))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(GlassSpacing.BottomSpacer))
            }
    }
}

@Composable
fun SpecialPresetItem(
    preset: SpecialVoicePreset,
    isSelected: Boolean,
    matchedVoiceName: String? = null,
    onSelectPreset: () -> Unit,
    onPreviewPreset: () -> Unit
) {
    val targetBg = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
    } else {
        Color.Transparent
    }
    val backgroundColor by animateColorAsState(targetBg, tween(180), label = "specialSel")
    val displayName = stringResource(preset.nameRes)
    val description = stringResource(preset.descriptionRes)
    val genderLabel = when (preset.preferredGender) {
        "Female" -> stringResource(R.string.gender_female)
        else -> stringResource(R.string.gender_male)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GlassShapes.Chip)
            .selectable(selected = isSelected, onClick = onSelectPreset, role = Role.RadioButton)
            .background(backgroundColor)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = null
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 52.dp)
                .clip(GlassShapes.Badge)
                .background(presetGradientFor(preset.id))
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                    ),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.weight(1f, fill = false)
                )
                SoftStatusBadge(
                    text = stringResource(specialVoiceTagLabelRes(preset.tag)),
                    accent = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = description,
                style = GlassTypography.cardSubtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            val meta = if (isSelected && !matchedVoiceName.isNullOrBlank()) {
                stringResource(
                    R.string.special_voice_meta_matched,
                    preset.pitch,
                    preset.speechRate,
                    genderLabel,
                    matchedVoiceName
                )
            } else {
                stringResource(
                    R.string.special_voice_meta,
                    preset.pitch,
                    preset.speechRate,
                    genderLabel
                )
            }
            Text(
                text = meta,
                style = GlassTypography.badgeLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
            )
        }
        IconButton(onClick = onPreviewPreset) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = stringResource(R.string.preview_named, displayName),
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
    val targetBg = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else Color.Transparent
    val backgroundColor by animateColorAsState(targetBg, tween(180), label = "voiceSel")
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GlassShapes.Chip)
            .selectable(selected = isSelected, onClick = { onSelectVoice(voice.name, voice.localeTag) }, role = Role.RadioButton)
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
                    SoftStatusBadge(
                        text = gender,
                        accent = if (gender == "Female") AccentFemale else AccentMale
                    )
                }

                SoftStatusBadge(
                    text = if (voice.requiresNetwork) {
                        stringResource(R.string.badge_cloud_sync)
                    } else {
                        stringResource(R.string.badge_offline)
                    },
                    accent = if (voice.requiresNetwork) AccentCloud else AccentOffline
                )
            }
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        val title = voice.description.ifBlank { voice.name }
        IconButton(
            onClick = { onPreviewVoice(voice.name, voice.localeTag) }
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = stringResource(R.string.preview_voice_named, title),
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
    val targetBg = if (isSelected) {
        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f)
    } else {
        Color.Transparent
    }
    val backgroundColor by animateColorAsState(targetBg, tween(180), label = "localSel")
    val displayName = stringResource(model.displayNameRes)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GlassShapes.Chip)
            .selectable(selected = isSelected, onClick = onSelect, role = Role.RadioButton)
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
                text = displayName,
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
                            stringResource(R.string.badge_on_device),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
                Text(
                    text = stringResource(model.descriptionRes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        IconButton(onClick = onPreview) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = stringResource(R.string.preview_named, displayName),
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
            .clip(GlassShapes.Chip)
            .selectable(
                selected = false,
                onClick = onClear,
                role = Role.RadioButton,
                onClickLabel = stringResource(R.string.use_system_voice),
            )
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = false, onClick = null)
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = stringResource(R.string.use_system_voice),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.use_system_voice_subtitle),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
