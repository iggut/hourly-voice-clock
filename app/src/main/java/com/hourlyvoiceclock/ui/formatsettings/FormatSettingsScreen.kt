package com.hourlyvoiceclock.ui.formatsettings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hourlyvoiceclock.R
import com.hourlyvoiceclock.data.AudioChannel
import com.hourlyvoiceclock.data.PhraseStyle
import com.hourlyvoiceclock.data.TimeFormat

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
    val chimeBefore by viewModel.chimeBefore.collectAsState()
    val vibrateBefore by viewModel.vibrateBefore.collectAsState()
    val announceDate by viewModel.announceDate.collectAsState()
    val audioChannel by viewModel.audioChannel.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.format_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Time format
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.time_format),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = timeFormat == TimeFormat.HOUR_12,
                            onClick = { viewModel.setTimeFormat(TimeFormat.HOUR_12) }
                        )
                        Text("12-hour")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = timeFormat == TimeFormat.HOUR_24,
                            onClick = { viewModel.setTimeFormat(TimeFormat.HOUR_24) }
                        )
                        Text("24-hour")
                    }
                }
            }

            // Phrase style
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.phrase_style),
                        style = MaterialTheme.typography.titleMedium
                    )
                    PhraseStyle.values().forEach { style ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = phraseStyle == style,
                                onClick = { viewModel.setPhraseStyle(style) }
                            )
                            Text(
                                when (style) {
                                    PhraseStyle.SIMPLE -> "Simple: \"It is 3 PM.\""
                                    PhraseStyle.DETAILED -> "Detailed: \"The time is 3:00 PM.\""
                                    PhraseStyle.FRIENDLY -> "Friendly: \"Good afternoon. It is 3 PM.\""
                                }
                            )
                        }
                    }
                }
            }

            // Toggles
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.chime_before))
                        Switch(
                            checked = chimeBefore,
                            onCheckedChange = { viewModel.setChimeBefore(it) }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.vibrate_before))
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
                        Text(stringResource(R.string.announce_date))
                        Switch(
                            checked = announceDate,
                            onCheckedChange = { viewModel.setAnnounceDate(it) }
                        )
                    }
                }
            }

            // Audio channel
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.audio_channel),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        stringResource(R.string.audio_channel_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AudioChannel.values().forEach { channel ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = audioChannel == channel,
                                onClick = { viewModel.setAudioChannel(channel) }
                            )
                            Text(
                                when (channel) {
                                    AudioChannel.MEDIA -> "Media"
                                    AudioChannel.NOTIFICATION -> "Notification"
                                    AudioChannel.CALL -> "Call"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
