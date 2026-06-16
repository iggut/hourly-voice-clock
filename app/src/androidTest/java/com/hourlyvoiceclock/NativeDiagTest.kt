package com.hourlyvoiceclock

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeDiagTest {
    companion object {
        init {
            System.loadLibrary("native-tts-bridge")
        }
        @JvmStatic
        external fun testFieldIds(config: OfflineTtsConfig)
    }

    @Test
    fun testFieldIds() {
        val config = OfflineTtsConfig(
            model = OfflineTtsModelConfig(
                vits = OfflineTtsVitsModelConfig(
                    model = "/tmp/dummy.onnx",
                    tokens = "/tmp/dummy.tokens",
                    dataDir = "/tmp/dummy"
                )
            )
        )
        testFieldIds(config)
    }
}
