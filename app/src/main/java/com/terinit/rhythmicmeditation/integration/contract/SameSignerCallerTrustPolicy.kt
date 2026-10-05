package com.terinit.rhythmicmeditation.integration.contract

/**
 * Final-equivalent peer trust for Routine (Pass 3 handoff item).
 *
 * The paired package is trusted ONLY with signing certificates identical to
 * Meditation's own signer — the same-signer model as the signature permission
 * guarding the status provider. Certificates are read from the platform at
 * runtime (via [SystemCallerIdentityResolver]); nothing is hardcoded and there
 * is no debug bypass and no package-name-only trust.
 *
 * Deny-by-default is preserved: an unlisted package, a wrong package name, or
 * the right name with a different signer is always rejected.
 */
class SameSignerCallerTrustPolicy(
    private val ownPackageName: String,
    private val identityResolver: SystemCallerIdentityResolver,
    private val acceptedPackages: Set<String> = DEFAULT_ACCEPTED_PACKAGES
) : CallerTrustPolicy {

    /** Multi-package peers are validated through [isAcceptedPackage]. */
    override fun expectedPackageName(): String? = acceptedPackages.firstOrNull()

    override fun expectedSigningCertificateDigests(): Set<String> =
        identityResolver.identityOf(ownPackageName).signingCertificateDigests

    /** Package must be an accepted companion name (signatures verified separately). */
    override fun isAcceptedPackage(packageName: String?): Boolean =
        packageName != null && packageName in acceptedPackages

    companion object {
        val DEFAULT_ACCEPTED_PACKAGES: Set<String> = setOf(
            "com.terinit.rhythmicroutine",
            "com.terinit.rhythmicroutine.qa"
        )
    }
}
