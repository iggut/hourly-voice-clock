package com.hourlyvoiceclock.ui.voicesettings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.hourlyvoiceclock.tts.VoiceInfo
import com.hourlyvoiceclock.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSettingsScreen(
    onBack: () -> Unit,
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
    val selectedGender by viewModel.selectedGender.collectAsState()
    val engines by viewModel.engines.collectAsState()
    val selectedEnginePackage by viewModel.selectedEnginePackage.collectAsState()
    val context = LocalContext.current

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
                                        if (engine.isInstalled) {
                                            viewModel.switchTtsEngine(engine.packageName)
                                        } else {
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
                                            text = if (isSelected) "Active" else "Tap to Switch",
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

                // Presets Section
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Special Voice Presets",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        viewModel.specialPresets.forEach { preset ->
                            val presetGradient = when (preset.id) {
                                "preset_robot" -> Brush.horizontalGradient(listOf(Color(0xFF06B6D4), Color(0xFF0891B2)))
                                "preset_freeman" -> Brush.horizontalGradient(listOf(Color(0xFF78350F), Color(0xFFD97706)))
                                "preset_giant" -> Brush.horizontalGradient(listOf(Color(0xFF6B21A8), Color(0xFF5B21B6)))
                                "preset_chipmunk" -> Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF059669)))
                                "preset_goblin" -> Brush.horizontalGradient(listOf(Color(0xFF84CC16), Color(0xFF65A30D)))
                                "preset_redneck" -> Brush.horizontalGradient(listOf(Color(0xFFF97316), Color(0xFFEA580C)))
                                "preset_baby" -> Brush.horizontalGradient(listOf(Color(0xFFEC4899), Color(0xFFD946EF)))
                                "preset_donald" -> Brush.horizontalGradient(listOf(Color(0xFFEAB308), Color(0xFFCA8A04)))
                                "preset_nerdy" -> Brush.horizontalGradient(listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8)))
                                "preset_slowmo" -> Brush.horizontalGradient(listOf(Color(0xFFEF4444), Color(0xFFB91C1C)))
                                else -> Brush.horizontalGradient(listOf(Color(0xFF6366F1), Color(0xFF4F46E5)))
                            }

                            Card(
                                modifier = Modifier
                                    .width(130.dp)
                                    .clickable { viewModel.selectVoicePreset(preset) },
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, if (isDark) GlassBorderDark else GlassBorderLight)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(presetGradient)
                                        .padding(14.dp)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = preset.displayName,
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Pitch: %.1f".format(preset.pitch),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.85f)
                                        )
                                    }
                                }
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
                                valueRange = 0.5f..2.0f
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
                                valueRange = 0.5f..2.0f
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

                // Gender Filter Section
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Filter by Gender",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val options = listOf("All", "Female", "Male")
                        options.forEach { option ->
                            FilterChip(
                                selected = selectedGender == option,
                                onClick = { viewModel.setGenderFilter(option) },
                                label = { Text(option, fontWeight = FontWeight.Bold) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                // Voice list grouped by locale
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

                Spacer(modifier = Modifier.height(30.dp))
            }
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
        IconButton(
            onClick = { onPreviewVoice(voice.name, voice.localeTag) }
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Preview voice ${voice.description}",
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
