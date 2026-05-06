package com.hourlyvoiceclock.ui.voicesettings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
    val voicesByLocale by viewModel.voicesByLocale.collectAsState()
    val selectedVoice by viewModel.selectedVoiceName.collectAsState()
    val pitch by viewModel.pitch.collectAsState()
    val speechRate by viewModel.speechRate.collectAsState()
    val hasMultiple by viewModel.hasMultipleVoices.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.voice_settings)) },
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
            if (!hasMultiple) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.no_voices_found),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            val intent = Intent("com.android.settings.TTS_SETTINGS").apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            }
                        }) {
                            Text(stringResource(R.string.open_tts_settings))
                        }
                    }
                }
            }

            // Pitch slider
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.pitch), style = MaterialTheme.typography.bodyLarge)
                    Slider(
                        value = pitch,
                        onValueChange = { viewModel.setPitch(it) },
                        valueRange = 0.5f..2.0f
                    )
                    Text("%.1f".format(pitch), style = MaterialTheme.typography.bodySmall)
                }
            }

            // Speech rate slider
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.speech_rate), style = MaterialTheme.typography.bodyLarge)
                    Slider(
                        value = speechRate,
                        onValueChange = { viewModel.setSpeechRate(it) },
                        valueRange = 0.5f..2.0f
                    )
                    Text("%.1f".format(speechRate), style = MaterialTheme.typography.bodySmall)
                }
            }

            Button(
                onClick = { viewModel.previewVoice() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.preview_voice))
            }

            // Voice list grouped by locale
            voicesByLocale.forEach { (localeName, voices) ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = localeName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        voices.forEachIndexed { index, voice ->
                            val displayLabel = voice.genderLabel
                                ?: stringResource(R.string.voice_n, index + 1)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedVoice == voice.name,
                                    onClick = {
                                        viewModel.selectVoice(voice.name, voice.localeTag)
                                    }
                                )
                                Column {
                                    Text(
                                        text = "${voice.name} ($displayLabel)",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "${stringResource(R.string.quality, voice.quality)}, " +
                                                "${stringResource(R.string.latency, voice.latency)}" +
                                                if (voice.requiresNetwork) ", ${stringResource(R.string.requires_network)}" else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
