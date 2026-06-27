package com.hourlyvoiceclock.announcer

import android.content.Context
import android.widget.Toast

/**
 * Port for user-facing feedback that originates from the announcement
 * pipeline (e.g. "your stream is muted").
 *
 * Keeping this behind an interface removes the direct [Toast] dependency
 * from [TimeAnnouncer] and makes the feedback path testable.
 */
interface UserFeedback {
    fun showMutedStreamMessage(channelLabel: String)
}

/**
 * Production implementation that shows a short [Toast].
 */
class ToastUserFeedback(private val context: Context) : UserFeedback {
    override fun showMutedStreamMessage(channelLabel: String) {
        Toast.makeText(
            context,
            "$channelLabel volume is muted. Turn up volume to hear announcements.",
            Toast.LENGTH_LONG
        ).show()
    }
}
