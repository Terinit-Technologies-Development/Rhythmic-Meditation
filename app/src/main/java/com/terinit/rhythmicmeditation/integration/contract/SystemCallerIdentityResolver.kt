package com.terinit.rhythmicmeditation.integration.contract

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import java.security.MessageDigest

/**
 * Resolves the identity (package + signing certificate digests) of an IPC
 * caller so [CallerVerifier] can make its deny-by-default decision.
 *
 * Digests are SHA-256 hex of the APK signing certificates.
 */
class SystemCallerIdentityResolver(private val context: Context) {

    fun identityOf(packageName: String?): CallerIdentity {
        if (packageName.isNullOrBlank()) return CallerIdentity.UNKNOWN
        return CallerIdentity(
            packageName = packageName,
            signingCertificateDigests = signingDigestsOf(packageName)
        )
    }

    @Suppress("DEPRECATION")
    private fun signingDigestsOf(packageName: String): Set<String> = try {
        val signatures: Array<out Signature>? = if (Build.VERSION.SDK_INT >= 28) {
            val info = context.packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_SIGNING_CERTIFICATES
            )
            val signingInfo = info.signingInfo ?: return emptySet()
            if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners
            } else {
                signingInfo.signingCertificateHistory
            }
        } else {
            context.packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures
        }
        signatures?.mapNotNull { signature ->
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(signature.toByteArray())
            digest.joinToString("") { "%02X".format(it) }
        }?.toSet() ?: emptySet()
    } catch (_: Throwable) {
        emptySet()
    }
}
