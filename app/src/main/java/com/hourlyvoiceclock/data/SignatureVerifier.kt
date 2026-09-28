package com.hourlyvoiceclock.data

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import android.util.Log
import java.security.MessageDigest

/**
 * Port for verifying APK signatures and detecting certificate mismatches.
 */
interface SignatureVerifier {

    sealed class VerifyResult {
        data class SignatureMismatch(
            val localFingerprint: String,
            val apkFingerprint: String,
            val message: String
        ) : VerifyResult()

        data class Error(val message: String) : VerifyResult()
        object SignaturesMatch : VerifyResult()
    }

    fun verifyUpdateCompatibility(localApkPath: String): VerifyResult
}

/**
 * Production implementation using [PackageManager].
 */
class AndroidSignatureVerifier(context: Context) : SignatureVerifier {

    private val appContext = context.applicationContext

    override fun verifyUpdateCompatibility(localApkPath: String): SignatureVerifier.VerifyResult {
        val localFingerprint = getInstalledAppSignatureFingerprint()
        val apkFingerprint = getApkSignatureFingerprint(localApkPath)

        if (localFingerprint == null) {
            return SignatureVerifier.VerifyResult.Error("Could not read local app signature")
        }

        if (apkFingerprint == null) {
            return SignatureVerifier.VerifyResult.Error("Could not read APK signature - file may be corrupted or not a valid APK")
        }

        return if (localFingerprint == apkFingerprint) {
            SignatureVerifier.VerifyResult.SignaturesMatch
        } else {
            SignatureVerifier.VerifyResult.SignatureMismatch(
                localFingerprint = localFingerprint,
                apkFingerprint = apkFingerprint,
                message = "Signature mismatch: Local app and APK were signed with different keys. " +
                        "This typically happens when installing a development build and trying to update " +
                        "with a release build, or vice versa. Solution: Uninstall the current app " +
                        "before installing the new version."
            )
        }
    }

    private fun getInstalledAppSignatureFingerprint(): String? {
        return try {
            val flags = signingFlags()
            val packageInfo = appContext.packageManager.getPackageInfo(appContext.packageName, flags)
            fingerprintOf(packageInfo) ?: run {
                Log.e(TAG, "No signatures found for installed app")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get installed app signature", e)
            null
        }
    }

    private fun getApkSignatureFingerprint(apkPath: String): String? {
        return try {
            val packageInfo = appContext.packageManager.getPackageArchiveInfo(apkPath, signingFlags())
            if (packageInfo == null) {
                Log.e(TAG, "Could not parse APK: $apkPath")
                return null
            }
            fingerprintOf(packageInfo) ?: run {
                Log.e(TAG, "No signatures found in APK")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get APK signature", e)
            null
        }
    }

    /**
     * Installed app and downloaded APK must be hashed the same way.
     * Mixing [PackageManager.GET_SIGNATURES] with signing-info certificates
     * can reject a valid same-key update on API 28+.
     */
    private fun signingFlags(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }
    }

    private fun fingerprintOf(packageInfo: PackageInfo): String? {
        val signature = signingCertificates(packageInfo).firstOrNull() ?: return null
        val digest = MessageDigest.getInstance("SHA-256").digest(signature.toByteArray())
        return bytesToHex(digest)
    }

    private fun signingCertificates(packageInfo: PackageInfo): List<Signature> {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.signingInfo?.apkContentsSigners
        } else {
            @Suppress("DEPRECATION")
            packageInfo.signatures
        }
        return signatures?.toList().orEmpty()
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = "0123456789ABCDEF"
        val result = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val i = b.toInt()
            result.append(hexChars[i shr 4 and 0x0f])
            result.append(hexChars[i and 0x0f])
        }
        return result.toString()
    }

    companion object {
        private const val TAG = "SignatureVerifier"
    }
}
