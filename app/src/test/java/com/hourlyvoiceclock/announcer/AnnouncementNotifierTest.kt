package com.hourlyvoiceclock.announcer

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AnnouncementNotifierTest {

    @Test
    fun `post on Robolectric runtime does not throw`() {
        val context = RuntimeEnvironment.getApplication()
        val notifier = AnnouncementNotifier(
            context = context,
            channelId = "test_channel"
        )
        notifier.post("It's 3 PM.")
    }

    @Test
    fun `default notification id is stable`() {
        assertEquals(2001, AnnouncementNotifier.DEFAULT_NOTIFICATION_ID)
    }
}
