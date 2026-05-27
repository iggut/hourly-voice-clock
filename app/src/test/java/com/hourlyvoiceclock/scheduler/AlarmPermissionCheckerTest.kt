package com.hourlyvoiceclock.scheduler

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class AlarmPermissionCheckerTest {

    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    fun testIsExactAlarmSupported_M() {
        assertTrue(AlarmPermissionChecker.isExactAlarmSupported())
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.LOLLIPOP])
    fun testIsExactAlarmSupported_L() {
        assertFalse(AlarmPermissionChecker.isExactAlarmSupported())
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S])
    fun testRequiresUserGrant_S() {
        assertTrue(AlarmPermissionChecker.requiresUserGrant())
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun testRequiresUserGrant_R() {
        assertFalse(AlarmPermissionChecker.requiresUserGrant())
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S])
    fun testExactAlarmSettingIntentAvailable_S() {
        assertTrue(AlarmPermissionChecker.exactAlarmSettingIntentAvailable())
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun testExactAlarmSettingIntentAvailable_R() {
        assertFalse(AlarmPermissionChecker.exactAlarmSettingIntentAvailable())
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S])
    fun testBuildExactAlarmSettingsIntent_S() {
        val context = RuntimeEnvironment.getApplication()
        val intent = AlarmPermissionChecker.buildExactAlarmSettingsIntent(context)

        assertEquals(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, intent.action)
        assertEquals("package:${context.packageName}", intent.dataString)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun testBuildExactAlarmSettingsIntent_R() {
        val context = RuntimeEnvironment.getApplication()
        val intent = AlarmPermissionChecker.buildExactAlarmSettingsIntent(context)

        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, intent.action)
        assertEquals("package:${context.packageName}", intent.dataString)
    }

    @Test
    fun testGetDeviceGuidance_Samsung() {
        ReflectionHelpers.setStaticField(Build::class.java, "MANUFACTURER", "Samsung")
        val guidance = AlarmPermissionChecker.getDeviceGuidance()
        assertEquals("Samsung", guidance.manufacturerLabel)
        assertTrue(guidance.permissionPath.contains("Alarms & reminders"))
        assertTrue(guidance.extraNote?.contains("Background usage limits") == true)
    }

    @Test
    fun testGetDeviceGuidance_Xiaomi() {
        ReflectionHelpers.setStaticField(Build::class.java, "MANUFACTURER", "xiaomi")
        val guidance = AlarmPermissionChecker.getDeviceGuidance()
        assertEquals("Xiaomi", guidance.manufacturerLabel)
        assertTrue(guidance.permissionPath.contains("Other Permissions"))
    }

    @Test
    fun testGetDeviceGuidance_Redmi() {
        ReflectionHelpers.setStaticField(Build::class.java, "MANUFACTURER", "redmi")
        val guidance = AlarmPermissionChecker.getDeviceGuidance()
        assertEquals("Xiaomi", guidance.manufacturerLabel)
    }

    @Test
    fun testGetDeviceGuidance_Poco() {
        ReflectionHelpers.setStaticField(Build::class.java, "MANUFACTURER", "poco")
        val guidance = AlarmPermissionChecker.getDeviceGuidance()
        assertEquals("Xiaomi", guidance.manufacturerLabel)
    }

    @Test
    fun testGetDeviceGuidance_Huawei() {
        ReflectionHelpers.setStaticField(Build::class.java, "MANUFACTURER", "huawei")
        val guidance = AlarmPermissionChecker.getDeviceGuidance()
        assertEquals("Huawei/Honor", guidance.manufacturerLabel)
    }

    @Test
    fun testGetDeviceGuidance_Honor() {
        ReflectionHelpers.setStaticField(Build::class.java, "MANUFACTURER", "HONOR")
        val guidance = AlarmPermissionChecker.getDeviceGuidance()
        assertEquals("Huawei/Honor", guidance.manufacturerLabel)
    }

    @Test
    fun testGetDeviceGuidance_Oppo() {
        ReflectionHelpers.setStaticField(Build::class.java, "MANUFACTURER", "Oppo")
        val guidance = AlarmPermissionChecker.getDeviceGuidance()
        assertEquals("Oppo/OnePlus", guidance.manufacturerLabel)
    }

    @Test
    fun testGetDeviceGuidance_Realme() {
        ReflectionHelpers.setStaticField(Build::class.java, "MANUFACTURER", "realme")
        val guidance = AlarmPermissionChecker.getDeviceGuidance()
        assertEquals("Oppo/OnePlus", guidance.manufacturerLabel)
    }

    @Test
    fun testGetDeviceGuidance_OnePlus() {
        ReflectionHelpers.setStaticField(Build::class.java, "MANUFACTURER", "OnePlus")
        val guidance = AlarmPermissionChecker.getDeviceGuidance()
        assertEquals("Oppo/OnePlus", guidance.manufacturerLabel)
    }

    @Test
    fun testGetDeviceGuidance_Vivo() {
        ReflectionHelpers.setStaticField(Build::class.java, "MANUFACTURER", "vivo")
        val guidance = AlarmPermissionChecker.getDeviceGuidance()
        assertEquals("Vivo", guidance.manufacturerLabel)
    }

    @Test
    fun testGetDeviceGuidance_Default() {
        ReflectionHelpers.setStaticField(Build::class.java, "MANUFACTURER", "Google")
        val guidance = AlarmPermissionChecker.getDeviceGuidance()
        assertEquals("Android", guidance.manufacturerLabel)
        assertTrue(guidance.permissionPath.contains("Hourly Voice Clock"))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun testCanScheduleExactAlarms_R() {
        val context = mock(Context::class.java)
        // On R (< S), it should just return true without checking AlarmManager
        assertTrue(AlarmPermissionChecker.canScheduleExactAlarms(context))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S])
    fun testCanScheduleExactAlarms_S_True() {
        val context = mock(Context::class.java)
        val alarmManager = mock(AlarmManager::class.java)

        `when`(context.getSystemService(Context.ALARM_SERVICE)).thenReturn(alarmManager)
        `when`(alarmManager.canScheduleExactAlarms()).thenReturn(true)

        assertTrue(AlarmPermissionChecker.canScheduleExactAlarms(context))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S])
    fun testCanScheduleExactAlarms_S_False() {
        val context = mock(Context::class.java)
        val alarmManager = mock(AlarmManager::class.java)

        `when`(context.getSystemService(Context.ALARM_SERVICE)).thenReturn(alarmManager)
        `when`(alarmManager.canScheduleExactAlarms()).thenReturn(false)

        assertFalse(AlarmPermissionChecker.canScheduleExactAlarms(context))
    }
}
