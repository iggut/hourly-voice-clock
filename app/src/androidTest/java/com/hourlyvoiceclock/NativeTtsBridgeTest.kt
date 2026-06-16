package com.hourlyvoiceclock

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hourlyvoiceclock.tts.local.NativeTtsBridge
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeTtsBridgeTest {
    @Test
    fun testNativeTtsBridgeCreateWithDummyPaths() {
        val ptr = NativeTtsBridge.nativeCreate("/data/local/tmp/dummy.onnx", "/data/local/tmp/dummy.tokens", "/data/local/tmp/dummy")
        android.util.Log.i("NativeTtsTest", "ptr = $ptr")
        if (ptr != 0L) {
            val sr = NativeTtsBridge.nativeSampleRate(ptr)
            android.util.Log.i("NativeTtsTest", "sampleRate = $sr")
            NativeTtsBridge.nativeDestroy(ptr)
        }
    }
}
