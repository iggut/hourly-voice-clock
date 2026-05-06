package com.hourlyvoiceclock.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hourlyvoiceclock.ui.home.HomeScreen
import com.hourlyvoiceclock.ui.voicesettings.VoiceSettingsScreen
import com.hourlyvoiceclock.ui.formatsettings.FormatSettingsScreen
import com.hourlyvoiceclock.ui.schedulesettings.ScheduleSettingsScreen

object Routes {
    const val HOME = "home"
    const val VOICE_SETTINGS = "voice_settings"
    const val FORMAT_SETTINGS = "format_settings"
    const val SCHEDULE_SETTINGS = "schedule_settings"
}

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToVoiceSettings = { navController.navigate(Routes.VOICE_SETTINGS) },
                onNavigateToFormatSettings = { navController.navigate(Routes.FORMAT_SETTINGS) },
                onNavigateToScheduleSettings = { navController.navigate(Routes.SCHEDULE_SETTINGS) }
            )
        }
        composable(Routes.VOICE_SETTINGS) {
            VoiceSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.FORMAT_SETTINGS) {
            FormatSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SCHEDULE_SETTINGS) {
            ScheduleSettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
