package com.hourlyvoiceclock.data

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import java.io.File
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

/**
 * Utility for verifying APK signatures and detecting certificate mismatches.
 * This helps diagnose INSTALL_PARSE_FAILED_INCONSISTENT_CERTIFICATES errors.
 */
object SignatureVerifier {
    private const val TAG = "SignatureVerifier"

    /**
     * Result of signature verification
     */
    sealed class VerifyResult {
        data class SignatureMismatch(
            val localFingerprint: String,
            val apkFingerprint: String,
            val message: String
        ) : VerifyResult()
        
        data class Error(val message: String) : VerifyResult()
        object SignaturesMatch : VerifyResult()
        object Verified : VerifyResult()
    }

    /**
     * Get the signature fingerprint of the currently installed app.
     * Returns null if the app is not installed.
     */
    fun getInstalledAppSignatureFingerprint(context: Context): String? {
        return try {
            val packageName = context.packageName
            val packageInfo = context.packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_SIGNATURES
            )
            
            val signatures = packageInfo.signatures
            if (signatures.isNullOrEmpty()) {
                Log.e(TAG, "No signatures found for installed app")
                return null
            }

            // Get the first signature's SHA-256 fingerprint
            val signature = signatures[0]
            val cert = signature.toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val publicKeyHash = md.digest(cert)
            bytesToHex(publicKeyHash)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get installed app signature", e)
            null
        }
    }

    /**
     * Get the signature fingerprint of an APK file.
     * Returns null if the APK cannot be parsed.
     */
    fun getApkSignatureFingerprint(apkPath: String): String? {
        return try {
            val file = File(apkPath)
            if (!file.exists()) {
                Log.e(TAG, "APK file does not exist: $apkPath")
                return null
            }

            val cf = CertificateFactory.getInstance("X.509")
            val fis = java.io.FileInputStream(file)
            val bis = java.io.BufferedInputStream(fis)
            
            var cert: X509Certificate? = null
            while (bis.available() > 0) {
                try {
                    cert = cf.generateCertificate(bis) as? X509Certificate
                    if (cert != null) break
                } catch (e: Exception) {
                    // Continue reading - APK may have multiple certificates
                }
            }
            bis.close()
            fis.close()

            if (cert == null) {
                Log.e(TAG, "No certificate found in APK")
                return null
            }

            val publicKey = cert.publicKey
            val md = MessageDigest.getInstance("SHA-256")
            val keyBytes = publicKey.encoded ?: cert.encoded
            val fingerprint = md.digest(keyBytes)
            bytesToHex(fingerprint)
        } catch (e: NoSuchAlgorithmException) {
            Log.e(TAG, "SHA-256 algorithm not available", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get APK signature", e)
            null
        }
    }

    /**
     * Verify if the APK can be installed over the currently installed app.
     * Returns detailed result about signature compatibility.
     */
    fun verifyUpdateCompatibility(context: Context, apkPath: String): VerifyResult {
        val localFingerprint = getInstalledAppSignatureFingerprint(context)
        val apkFingerprint = getApkSignatureFingerprint(apkPath)

        if (localFingerprint == null) {
            return VerifyResult.Error("Could not read local app signature")
        }

        if (apkFingerprint == null) {
            return VerifyResult.Error("Could not read APK signature - file may be corrupted or not a valid APK")
        }

        return if (localFingerprint == apkFingerprint) {
            VerifyResult.SignaturesMatch
        } else {
            VerifyResult.SignatureMismatch(
                localFingerprint = localFingerprint,
                apkFingerprint = apkFingerprint,
                message = "Signature mismatch: Local app and APK were signed with different keys. " +
                        "This typically happens when installing a development build and trying to update " +
                        "with a release build, or vice versa. Solution: Uninstall the current app " +
                        "before installing the new version."
            )
        }
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
}