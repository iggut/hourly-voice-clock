package com.hourlyvoiceclock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.hourlyvoiceclock.di.DependenciesProvider
import com.hourlyvoiceclock.ui.navigation.AppNavigation
import com.hourlyvoiceclock.ui.theme.HourlyVoiceClockTheme
import kotlinx.coroutines.flow.map

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val deps = (application as DependenciesProvider).dependencies
            val useDynamicColor by remember {
                deps.settingsRepository.settings.map { it.useDynamicColor }
            }.collectAsState(initial = false)

            HourlyVoiceClockTheme(dynamicColor = useDynamicColor) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}
