package com.hourlyvoiceclock.ui.formatsettings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hourlyvoiceclock.R
import com.hourlyvoiceclock.data.AudioChannel
import com.hourlyvoiceclock.data.ChimeSound
import com.hourlyvoiceclock.data.PhraseStyle
import com.hourlyvoiceclock.data.TimeFormat
import com.hourlyvoiceclock.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormatSettingsScreen(
    onBack: () -> Unit,
    viewModel: FormatSettingsViewModel = viewModel(
        factory = FormatSettingsViewModelFactory(
            LocalContext.current.applicationContext as android.app.Application
        )
    )
) {
    val timeFormat by viewModel.timeFormat.collectAsState()
    val phraseStyle by viewModel.phraseStyle.collectAsState()
    val customPrefix by viewModel.customPrefix.collectAsState()
    val customSuffix by viewModel.customSuffix.collectAsState()
    val chimeSound by viewModel.chimeSound.collectAsState()
    val vibrateBefore by viewModel.vibrateBefore.collectAsState()
    val announceDate by viewModel.announceDate.collectAsState()
    val audioChannel by viewModel.audioChannel.collectAsState()

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
                    title = { Text(stringResource(R.string.format_settings), fontWeight = FontWeight.Bold, fontSize = 20.sp) },
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

                // Time format
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) GlassBgDark else GlassBgLight),
                    border = BorderStroke(1.dp, if (isDark) GlassBorderDark else GlassBorderLight)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Text(
                                stringResource(R.string.time_format),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            RadioButton(
                                selected = timeFormat == TimeFormat.HOUR_12,
                                onClick = { viewModel.setTimeFormat(TimeFormat.HOUR_12) }
                            )
                            Text("12-hour (e.g. 3:00 PM)", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            RadioButton(
                                selected = timeFormat == TimeFormat.HOUR_24,
                                onClick = { viewModel.setTimeFormat(TimeFormat.HOUR_24) }
                            )
                            Text("24-hour (e.g. 15:00)", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }

                // Phrase style
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) GlassBgDark else GlassBgLight),
                    border = BorderStroke(1.dp, if (isDark) GlassBorderDark else GlassBorderLight)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Style, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Text(
                                stringResource(R.string.phrase_style),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        PhraseStyle.values().forEach { style ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                RadioButton(
                                    selected = phraseStyle == style,
                                    onClick = { viewModel.setPhraseStyle(style) }
                                )
                                Text(
                                    text = when (style) {
                                        PhraseStyle.SIMPLE -> "Simple: \"It is 3 PM.\""
                                        PhraseStyle.DETAILED -> "Detailed: \"The time is 3:00 PM.\""
                                        PhraseStyle.FRIENDLY -> "Friendly: \"Good afternoon. It is 3 PM.\""
                                        PhraseStyle.CUSTOM -> "Custom phrase format"
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                        if (phraseStyle == PhraseStyle.CUSTOM) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = customPrefix,
                                onValueChange = { viewModel.setCustomPrefix(it) },
                                label = { Text("Prefix (e.g., 'Hello, it is now ')") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = customSuffix,
                                onValueChange = { viewModel.setCustomSuffix(it) },
                                label = { Text("Suffix (e.g., ' master.')") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            )
                        }
                    }
                }

                // Toggles
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) GlassBgDark else GlassBgLight),
                    border = BorderStroke(1.dp, if (isDark) GlassBorderDark else GlassBorderLight)
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.chime_before), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                            ChimeSoundSelector(
                                selectedSound = chimeSound,
                                onSoundSelected = { viewModel.setChimeSound(it) }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.vibrate_before), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                            Switch(
                                checked = vibrateBefore,
                                onCheckedChange = { viewModel.setVibrateBefore(it) }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.announce_date), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                            Switch(
                                checked = announceDate,
                                onCheckedChange = { viewModel.setAnnounceDate(it) }
                            )
                        }
                    }
                }

                // Audio channel
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) GlassBgDark else GlassBgLight),
                    border = BorderStroke(1.dp, if (isDark) GlassBorderDark else GlassBorderLight)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Text(
                                stringResource(R.string.audio_channel),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.audio_channel_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        AudioChannel.values().forEach { channel ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                RadioButton(
                                    selected = audioChannel == channel,
                                    onClick = { viewModel.setAudioChannel(channel) }
                                )
                                Text(
                                    text = when (channel) {
                                        AudioChannel.MEDIA -> "Media player (uses media volume)"
                                        AudioChannel.NOTIFICATION -> "Notification stream (respects do-not-disturb)"
                                        AudioChannel.CALL -> "Voice call stream"
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = 8.dp)
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
fun ChimeSoundSelector(
    selectedSound: ChimeSound,
    onSoundSelected: (ChimeSound) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = when (selectedSound) {
                    ChimeSound.NONE -> stringResource(R.string.chime_none)
                    ChimeSound.CLASSIC_CHIME -> stringResource(R.string.chime_classic_chime)
                    ChimeSound.BELL -> stringResource(R.string.chime_bell)
                    ChimeSound.GONG -> stringResource(R.string.chime_gong)
                    ChimeSound.CYMBALS -> stringResource(R.string.chime_cymbals)
                    ChimeSound.DIGITAL_BEEP -> stringResource(R.string.chime_digital_beep)
                    ChimeSound.BIRD_CHIRP -> stringResource(R.string.chime_bird_chirp)
                    ChimeSound.HONK -> stringResource(R.string.chime_honk)
                },
                style = MaterialTheme.typography.bodyMedium
            )
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ChimeSound.entries.forEach { sound ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = when (sound) {
                                ChimeSound.NONE -> stringResource(R.string.chime_none)
                                ChimeSound.CLASSIC_CHIME -> stringResource(R.string.chime_classic_chime)
                                ChimeSound.BELL -> stringResource(R.string.chime_bell)
                                ChimeSound.GONG -> stringResource(R.string.chime_gong)
                                ChimeSound.CYMBALS -> stringResource(R.string.chime_cymbals)
                                ChimeSound.DIGITAL_BEEP -> stringResource(R.string.chime_digital_beep)
                                ChimeSound.BIRD_CHIRP -> stringResource(R.string.chime_bird_chirp)
                                ChimeSound.HONK -> stringResource(R.string.chime_honk)
                            }
                        )
                    },
                    onClick = {
                        onSoundSelected(sound)
                        expanded = false
                    }
                )
            }
        }
    }
}
