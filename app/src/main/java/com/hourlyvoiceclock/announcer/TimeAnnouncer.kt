package com.hourlyvoiceclock.announcer

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import com.hourlyvoiceclock.data.AppSettings
import com.hourlyvoiceclock.tts.TtsVoiceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

class TimeAnnouncer(
    private val context: Context,
    private val ttsRepository: TtsVoiceRepository
) {

    suspend fun announce(settings: AppSettings, force: Boolean = false, includeDate: Boolean = false): Boolean {
        val now = LocalDateTime.now()

        if (!force) {
            val inQuiet = QuietHoursPolicy.isQuietTime(
                now.toLocalTime(),
                settings.quietHoursEnabled,
                settings.quietHoursStart,
                settings.quietHoursEnd
            )
            if (inQuiet) return false
        }

        if (settings.vibrateBefore) {
            vibrate()
        }

        ttsRepository.initialize()
        settings.selectedVoiceName?.let { voiceName ->
            val success = ttsRepository.selectVoice(voiceName, settings.selectedLocale ?: "")
            if (!success) {
                settings.selectedLocale?.let { locale ->
                    ttsRepository.selectLanguage(locale)
                }
            }
        }
        ttsRepository.setPitch(settings.pitch)
        ttsRepository.setSpeechRate(settings.speechRate)

        val text = AnnouncementFormatter.format(
            now,
            settings.timeFormat,
            settings.phraseStyle,
            includeDate && settings.announceDateOnDemand
        )

        return withContext(Dispatchers.Main) {
            ttsRepository.previewVoice(text)
        }
    }

    private fun vibrate() {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(200)
        }
    }
}
