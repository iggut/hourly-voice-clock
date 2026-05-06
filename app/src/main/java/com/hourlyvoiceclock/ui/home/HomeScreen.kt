package com.hourlyvoiceclock.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToVoiceSettings: () -> Unit,
    onNavigateToFormatSettings: () -> Unit,
    onNavigateToScheduleSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(LocalContext.current.applicationContext as android.app.Application))
) {
    val currentTime by viewModel.currentTime.collectAsState()
    val nextAnnouncement by viewModel.nextAnnouncement.collectAsState()
    val quietHoursActive by viewModel.quietHoursActive.collectAsState()
    val hourlyEnabled by viewModel.hourlyEnabled.collectAsState()
    val canSpeakNow by viewModel.canSpeakNow.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onNavigateToVoiceSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Time display
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = currentTime,
                        style = MaterialTheme.typography.displayLarge
                    )
                }
            }

            // Announce now button
            Button(
                onClick = { viewModel.announceNow() },
                enabled = canSpeakNow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.announce_now))
            }

            // Hourly toggle
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.hourly_announcements),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Switch(
                            checked = hourlyEnabled,
                            onCheckedChange = { viewModel.toggleHourly(it) }
                        )
                    }
                    if (nextAnnouncement.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.next_announcement, nextAnnouncement),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Quiet hours status
            if (quietHoursActive) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(
                        text = stringResource(R.string.quiet_hours_active),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // Settings shortcuts
            Button(
                onClick = onNavigateToVoiceSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.voice_settings))
            }
            Button(
                onClick = onNavigateToFormatSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.format_settings))
            }
            Button(
                onClick = onNavigateToScheduleSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.schedule_settings))
            }
        }
    }
}
