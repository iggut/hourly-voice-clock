package com.hourlyvoiceclock.ui.formatsettings

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.os.Build
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hourlyvoiceclock.R
import com.hourlyvoiceclock.data.AudioChannel
import com.hourlyvoiceclock.data.ChimeSound
import com.hourlyvoiceclock.data.PhraseStyle
import com.hourlyvoiceclock.data.TimeFormat
import com.hourlyvoiceclock.ui.components.GlassCard
import com.hourlyvoiceclock.ui.components.GlassScreen
import com.hourlyvoiceclock.ui.components.SectionHeader
import com.hourlyvoiceclock.ui.components.glassPagePadding
import com.hourlyvoiceclock.ui.theme.GlassShapes
import com.hourlyvoiceclock.ui.theme.GlassSpacing

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
    val useDynamicColor by viewModel.useDynamicColor.collectAsState()
    val dynamicColorSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    GlassScreen(
        title = stringResource(R.string.format_settings),
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

                SectionHeader(title = stringResource(R.string.time_format), icon = Icons.Default.Timer)
                GlassCard {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(GlassShapes.Chip).selectable(selected = timeFormat == TimeFormat.HOUR_12, onClick = { viewModel.setTimeFormat(TimeFormat.HOUR_12) }, role = Role.RadioButton)) {
                            RadioButton(
                                selected = timeFormat == TimeFormat.HOUR_12,
                                onClick = null
                            )
                            Text("12-hour (e.g. 3:00 PM)", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(GlassShapes.Chip).selectable(selected = timeFormat == TimeFormat.HOUR_24, onClick = { viewModel.setTimeFormat(TimeFormat.HOUR_24) }, role = Role.RadioButton)) {
                            RadioButton(
                                selected = timeFormat == TimeFormat.HOUR_24,
                                onClick = null
                            )
                            Text("24-hour (e.g. 15:00)", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
                        }
                }

                SectionHeader(title = stringResource(R.string.phrase_style), icon = Icons.Default.Style)
                GlassCard {
                        val optionShape = GlassShapes.Chip
                        PhraseStyle.entries.forEach { style ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(optionShape).selectable(selected = phraseStyle == style, onClick = { viewModel.setPhraseStyle(style) }, role = Role.RadioButton)) {
                                RadioButton(
                                    selected = phraseStyle == style,
                                    onClick = null
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
                                label = { Text("Prefix") },
                                placeholder = { Text("e.g., 'Hello, it is now '") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = GlassShapes.Item,
                                trailingIcon = {
                                    if (customPrefix.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.setCustomPrefix("") }) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = stringResource(R.string.clear_text)
                                            )
                                        }
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = customSuffix,
                                onValueChange = { viewModel.setCustomSuffix(it) },
                                label = { Text("Suffix") },
                                placeholder = { Text("e.g., ' master.'") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = GlassShapes.Item,
                                trailingIcon = {
                                    if (customSuffix.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.setCustomSuffix("") }) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = stringResource(R.string.clear_text)
                                            )
                                        }
                                    }
                                }
                            )
                        }
                }

                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                            modifier = Modifier.fillMaxWidth().clip(GlassShapes.Chip).toggleable(value = vibrateBefore, onValueChange = { viewModel.setVibrateBefore(it) }, role = Role.Switch),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.vibrate_before), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                            Switch(
                                checked = vibrateBefore,
                                onCheckedChange = null
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(GlassShapes.Chip).toggleable(value = announceDate, onValueChange = { viewModel.setAnnounceDate(it) }, role = Role.Switch),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.announce_date), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                            Switch(
                                checked = announceDate,
                                onCheckedChange = null
                            )
                        }
                    }
                }

                SectionHeader(title = stringResource(R.string.audio_channel), icon = Icons.AutoMirrored.Filled.VolumeUp)
                GlassCard {
                        Text(
                            stringResource(R.string.audio_channel_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        val optionShape = GlassShapes.Chip
                        AudioChannel.entries.forEach { channel ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(optionShape).selectable(selected = audioChannel == channel, onClick = { viewModel.setAudioChannel(channel) }, role = Role.RadioButton)) {
                                RadioButton(
                                    selected = audioChannel == channel,
                                    onClick = null
                                )
                                Text(
                                    text = when (channel) {
                                        AudioChannel.MEDIA -> "Media output (uses media volume)"
                                        AudioChannel.NOTIFICATION -> "Notification output (respects do-not-disturb)"
                                        AudioChannel.CALL -> "Ringer output (uses ringtone volume)"
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                }

                SectionHeader(title = stringResource(R.string.appearance_section), icon = Icons.Default.Palette)
                GlassCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(GlassShapes.Chip)
                            .toggleable(
                                value = useDynamicColor,
                                enabled = dynamicColorSupported,
                                onValueChange = { viewModel.setUseDynamicColor(it) },
                                role = Role.Switch
                            )
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                stringResource(R.string.material_you_colors),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                            )
                            Text(
                                if (dynamicColorSupported) {
                                    stringResource(R.string.material_you_colors_desc)
                                } else {
                                    stringResource(R.string.material_you_unavailable)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = useDynamicColor && dynamicColorSupported,
                            onCheckedChange = null,
                            enabled = dynamicColorSupported
                        )
                    }
                }

                Spacer(modifier = Modifier.height(GlassSpacing.BottomSpacer))
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
            shape = GlassShapes.Chip
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
                contentDescription = if (expanded) stringResource(R.string.a11y_collapse) else stringResource(R.string.a11y_expand),
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
