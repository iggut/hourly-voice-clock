# ProGuard / R8 rules for Hourly Voice Clock
#
# R8 is enabled for release builds. These rules keep the few classes
# that R8's static analysis cannot see are needed, and silence warnings
# for framework classes that ship their own consumer rules.
#
# Notes:
#  * The Kotlin/Compose/DataStore/AndroidX libraries ship their own
#    consumer-rules.pro via AAR. Most reflection-based code (Moshi,
#    kotlinx-serialization, Retrofit, etc.) is auto-handled.
#  * We use kotlinx.coroutines by direct call, not by reflection —
#    nothing extra is needed for the bridge.
#  * OkHttp is on the classpath; rules ship in its AAR.
#  * No JSON serialisation in production code — UpdateChecker parses
#    org.json manually and does not reflect on a model class.
# * No JNI / native methods.
#
# NOTE: The above was written before Sherpa-ONNX was integrated.
# Sherpa-ONNX uses JNI reflection to access Java fields from native
# code. R8 MUST NOT obfuscate or remove any class in the
# com.k2fsa.sherpa.onnx package, or the native layer will crash
# with NoSuchFieldError / ClassNotFoundException at runtime.

# ── Our code: keep things that R8 might over-prune ───────────────────────

# UpdateStatus is a sealed class used in `when` blocks (instanceof + smart
# cast). R8 keeps the subclasses by default, but be explicit so a future
# refactor that adds reflectively-accessed subclasses does not silently
# break the build.
-keep class com.hourlyvoiceclock.data.UpdateStatus { *; }
-keep class com.hourlyvoiceclock.data.UpdateStatus$* { *; }

# SettingsRepository uses helper static methods (safeEnumValueOf,
# parseTime, formatTime) that are called via FQ name from the migration
# code. R8 inlines these into the only caller in practice; pinning them
# keeps a stable surface for tests.
-keepclassmembers class com.hourlyvoiceclock.data.SettingsRepository {
    public static <methods>;
}

# The TtsEngine wrapper around android.speech.tts.TextToSpeech stores
# a UtteranceProgressListener. The progress listener is an anonymous
# class and would normally be kept by R8 because it is referenced, but
# the UtteranceProgressListener API itself uses default-method dispatch
# from the Android framework; keep the listener hook.
-keepclassmembers class * extends android.speech.tts.UtteranceProgressListener {
    public <init>(...);
}

# AndroidManifest-declared components (HourlyVoiceClockApp, MainActivity,
# AlarmReceiver, BootReceiver, TimeChangedReceiver) are referenced from
# the manifest and kept by R8 automatically, but pin the names so a
# typo during refactoring fails the build instead of silently dropping
# the class.
-keep class com.hourlyvoiceclock.HourlyVoiceClockApp
-keep class com.hourlyvoiceclock.MainActivity
-keep class com.hourlyvoiceclock.receiver.AlarmReceiver
-keep class com.hourlyvoiceclock.receiver.BootReceiver
-keep class com.hourlyvoiceclock.receiver.TimeChangedReceiver

# ViewModels are constructed via AndroidViewModelFactory through the
# ViewModelProvider; the factory looks them up by FQ name.
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class * extends androidx.lifecycle.AndroidViewModel {
    <init>(android.app.Application);
}

# ── Library compatibility ────────────────────────────────────────────────

# Suppress R8 warnings for missing classes referenced by libraries we
# depend on but do not use at runtime (e.g. javax.annotation.*).
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Sherpa-ONNX JNI — native code accesses these classes/fields by
# name through JNI FindClass / GetFieldID. R8 obfuscation breaks
# the native→Java binding and produces NoSuchFieldError crashes.
-keep class com.k2fsa.sherpa.onnx.** { *; }

# kotlinx-coroutines debug agent (only used in -coroutines-core debug
# builds, not in release, but R8 may still warn about missing classes
# from the optional `kotlinx-coroutines-debug` artifact).
-dontwarn kotlinx.coroutines.debug.**

# OkHttp's optional Conscrypt / BouncyCastle providers. We do not link
# these in release; silence the warnings.
-dontwarn okhttp3.internal.platform.**
