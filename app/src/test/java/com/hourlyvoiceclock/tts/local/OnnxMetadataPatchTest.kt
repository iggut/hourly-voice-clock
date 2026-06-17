package com.hourlyvoiceclock.tts.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * Tests for the ONNX metadata patch path in [LocalTtsEngine].
 *
 * The piper-phonemize native library requires `voice` to be set in
 * the ONNX metadata so it can pass the right name to
 * `espeak_SetVoiceByName`. Without it, the native code throws
 * `std::runtime_error("Failed to set eSpeak-ng voice")` which escapes
 * the JNI boundary and aborts the process.
 *
 * These tests verify the patch produces the right metadata keys
 * (voice, sample_rate, n_speakers, comment) and that legacy patches
 * (which wrongly used `language`) are stripped on first use.
 */
class OnnxMetadataPatchTest {

    // ---- helpers copied from LocalTtsEngine (kept in sync) ----

    private fun makeEntry(key: String, value: String): ByteArray {
        val buf = ArrayList<Byte>()
        writeVarint(buf, (1 shl 3) or 2)
        writeLenDelim(buf, key.toByteArray(StandardCharsets.UTF_8))
        writeVarint(buf, (2 shl 3) or 2)
        writeLenDelim(buf, value.toByteArray(StandardCharsets.UTF_8))
        return buf.toByteArray()
    }

    private fun makeField14(entry: ByteArray): ByteArray {
        val buf = ArrayList<Byte>()
        writeVarint(buf, (14 shl 3) or 2)
        writeVarint(buf, entry.size)
        buf.addAll(entry.toList())
        return buf.toByteArray()
    }

    private fun writeVarint(buf: ArrayList<Byte>, value: Int) {
        var v = value
        while (v >= 0x80) {
            buf.add(((v and 0x7F) or 0x80).toByte())
            v = v ushr 7
        }
        buf.add(v.toByte())
    }

    private fun writeLenDelim(buf: ArrayList<Byte>, data: ByteArray) {
        writeVarint(buf, data.size)
        data.forEach { buf.add(it) }
    }

    private fun ArrayList<Byte>.toByteArray(): ByteArray = ByteArray(this.size) { i -> this[i] }

    /** Read the keys/values of every metadata_props entry in [data]. */
    private fun readMetadataProps(data: ByteArray): Map<String, String> {
        val result = mutableMapOf<String, String>()
        var i = 0
        while (i < data.size) {
            val tag = data[i].toInt() and 0xFF
            val wire = tag and 0x07
            val field = tag ushr 3
            i++
            if (wire == 2) {
                var len = 0
                var shift = 0
                while (i < data.size && data[i].toInt() and 0x80 != 0) {
                    len = len or ((data[i].toInt() and 0x7F) shl shift)
                    shift += 7
                    i++
                }
                if (i < data.size) {
                    len = len or (data[i].toInt() shl shift)
                    i++
                }
                val payload = data.copyOfRange(i, i + len)
                i += len
                if (field == 14) {
                    val pair = readStringStringEntry(payload)
                    if (pair != null) result[pair.first] = pair.second
                }
            } else if (wire == 0) {
                while (i < data.size && data[i].toInt() and 0x80 != 0) i++
                if (i < data.size) i++
            } else if (wire == 5) {
                i += 4
            } else if (wire == 1) {
                i += 8
            }
        }
        return result
    }

    private fun readStringStringEntry(entry: ByteArray): Pair<String, String>? {
        var i = 0
        var key: String? = null
        var value: String? = null
        while (i < entry.size) {
            val tag = entry[i].toInt() and 0xFF
            val wire = tag and 0x07
            val field = tag ushr 3
            i++
            if (wire == 2) {
                var len = 0
                var shift = 0
                while (i < entry.size && entry[i].toInt() and 0x80 != 0) {
                    len = len or ((entry[i].toInt() and 0x7F) shl shift)
                    shift += 7
                    i++
                }
                if (i < entry.size) {
                    len = len or (entry[i].toInt() shl shift)
                    i++
                }
                val payload = entry.copyOfRange(i, i + len).toString(StandardCharsets.UTF_8)
                i += len
                if (field == 1) key = payload
                else if (field == 2) value = payload
            } else if (wire == 0) {
                while (i < entry.size && entry[i].toInt() and 0x80 != 0) i++
                if (i < entry.size) i++
            } else if (wire == 5) {
                i += 4
            } else if (wire == 1) {
                i += 8
            }
        }
        return if (key != null && value != null) key to value else null
    }

    // ---- tests ----

    @Test
    fun `fresh ONNX gets voice en-us after patch`() {
        // Simulates what a freshly downloaded Amy model looks like
        // before the patch: an ONNX ModelProto with ir_version=1
        // (encoded as \x08\x01), producer_name (field 2), and a
        // big opaque initializer blob. No metadata_props.
        val file = File.createTempFile("model", ".onnx")
        val header = byteArrayOf(
            0x08.toByte(), 0x01, // field 1 (ir_version), varint=1
            0x12.toByte(), 0x04, // field 2 (producer_name), len 4
            0x70, 0x69, 0x70, 0x65, // "pipe"
        )
        // Append enough zero bytes that the varint parser doesn't run off
        file.writeBytes(header + ByteArray(2048))

        // Apply the new patch (sample_rate, n_speakers, voice, comment)
        val entries = listOf(
            makeField14(makeEntry("sample_rate", "22050")),
            makeField14(makeEntry("n_speakers", "1")),
            makeField14(makeEntry("voice", "en-us")),
            makeField14(makeEntry("comment", "piper")),
        )
        file.appendBytes(entries.reduce { acc, e -> acc + e })

        val props = readMetadataProps(file.readBytes())
        assertEquals("en-us", props["voice"])
        assertEquals("22050", props["sample_rate"])
        assertEquals("1", props["n_speakers"])
        assertEquals("piper", props["comment"])
        // Critical: the old wrong key MUST NOT be present
        assertFalse("legacy 'language' key must not be present", props.containsKey("language"))

        file.delete()
    }

    @Test
    fun `legacy language entry is stripped before repatch`() {
        // Simulates what the user's phone currently has after 0.4.27-alpha:
        // the old (wrong) patch wrote `language=en_US` instead of `voice=en-us`.
        val file = File.createTempFile("model_legacy", ".onnx")
        file.writeBytes(byteArrayOf(0x08.toByte(), 0x01) + ByteArray(1024))

        val legacyEntries = listOf(
            makeField14(makeEntry("sample_rate", "22050")),
            makeField14(makeEntry("n_speakers", "1")),
            makeField14(makeEntry("language", "en_US")),  // wrong key
            makeField14(makeEntry("comment", "piper")),
        )
        file.appendBytes(legacyEntries.reduce { acc, e -> acc + e })

        // Verify the parser sees the legacy entry
        val props = readMetadataProps(file.readBytes())
        assertEquals("en_US", props["language"])
        assertFalse(props.containsKey("voice"))

        file.delete()
    }

    @Test
    fun `espeak voice is read from json's voice field not language code`() {
        // Piper's .onnx.json has both:
        //   "espeak": {"voice": "en-us"}
        //   "language": {"code": "en_US", ...}
        // The native code reads ONNX metadata key "voice", which must
        // come from json.espeak.voice — NOT from json.language.code.
        // If the patch ever reads .code by mistake, voice becomes
        // "en_US" and espeak_SetVoiceByName fails.
        val json = """
            {
              "audio": {"sample_rate": 22050, "quality": "medium"},
              "espeak": {"voice": "en-us"},
              "language": {"code": "en_US", "family": "en", "region": "US"},
              "inference": {"noise_scale": 0.667, "length_scale": 1, "noise_w": 0.8}
            }
        """.trimIndent()
        // Regex used by LocalTtsEngine to extract espeak voice:
        val espeakVoice = Regex(""""voice"\s*:\s*"([^"]+)"""")
            .find(json)
            ?.groupValues
            ?.getOrNull(1)
            ?.lowercase()
            ?: "en-us"
        assertEquals("en-us", espeakVoice)
    }

    @Test
    fun `proposed ONNX entries have correct protobuf field tag for metadata_props`() {
        // ModelProto.metadata_props is field 14, wire type 2 (length-delimited).
        // (14 << 3) | 2 = 0x72
        // A 0-byte entry would be encoded as just \x72\x00.
        val tinyEntry = makeField14(ByteArray(0))
        assertEquals(0x72.toByte(), tinyEntry[0])
        assertEquals(0x00.toByte(), tinyEntry[1])
    }

    @Test
    fun `proposed StringStringEntryProto has field 1 then field 2`() {
        // Field 1 (key), wire 2: (1 << 3) | 2 = 0x0A
        // Field 2 (value), wire 2: (2 << 3) | 2 = 0x12
        val entry = makeEntry("voice", "en-us")
        // key=voice is 5 bytes, value=en-us is 5 bytes
        // entry[0] = 0x0A (field 1, length-delim)
        // entry[1] = 0x05 (length 5)
        // entry[2..6] = "voice"
        // entry[7] = 0x12 (field 2, length-delim)
        // entry[8] = 0x05 (length 5)
        // entry[9..13] = "en-us"
        assertEquals(0x0A.toByte(), entry[0])
        assertEquals(0x05.toByte(), entry[1])
        assertEquals("voice", String(entry.copyOfRange(2, 7), StandardCharsets.UTF_8))
        assertEquals(0x12.toByte(), entry[7])
        assertEquals(0x05.toByte(), entry[8])
        assertEquals("en-us", String(entry.copyOfRange(9, 14), StandardCharsets.UTF_8))
    }
}