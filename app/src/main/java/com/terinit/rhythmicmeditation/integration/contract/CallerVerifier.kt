package com.terinit.rhythmicmeditation.integration.contract

/**
 * Supplies the identity of the trusted peer (Rhythmic Routine) for caller
 * verification. Backed by app preferences once pairing is configured.
 */
interface CallerTrustPolicy {
    /** Expected calling package name, or null when no peer is configured. */
    fun expectedPackageName(): String?

    /** Expected signing certificate digests of the peer (empty until configured). */
    fun expectedSigningCertificateDigests(): Set<String>
}

/**
 * Verifies that IPC callers are the genuine, same-signature companion app.
 *
 * Deny-by-default: an unconfigured peer rejects every caller. Signature
 * comparison is a placeholder hook — the digest source (PackageManager
 * signing info) is supplied by the caller of [verifyCaller], so this class
 * stays unit-testable without the Android framework.
 */
class CallerVerifier(private val trustPolicy: CallerTrustPolicy) {

    fun verifyCaller(
        callingPackage: String?,
        callingSigningCertificateDigests: Set<String>
    ): Result<Unit> {
        val expectedPackage = trustPolicy.expectedPackageName()
            ?: return Result.failure(
                SecurityException("No paired peer configured; rejecting caller")
            )
        if (callingPackage.isNullOrBlank()) {
            return Result.failure(SecurityException("Caller package is unknown"))
        }
        if (callingPackage != expectedPackage) {
            return Result.failure(
                SecurityException("Caller '$callingPackage' is not the paired peer")
            )
        }
        val expectedDigests = trustPolicy.expectedSigningCertificateDigests()
        if (expectedDigests.isEmpty()) {
            return Result.failure(
                SecurityException("Peer signing certificates are not configured yet")
            )
        }
        if (callingSigningCertificateDigests.none { it in expectedDigests }) {
            return Result.failure(
                SecurityException("Caller signature does not match the paired peer")
            )
        }
        return Result.success(Unit)
    }
}
