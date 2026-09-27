package com.terinit.rhythmicmeditation.testutil

import com.terinit.rhythmicmeditation.integration.contract.CallerTrustPolicy

/**
 * Configurable [CallerTrustPolicy] for tests. Production stays deny-by-default
 * (unconfigured peer); tests inject this to exercise verified flows — there is
 * no debug bypass in production security logic.
 */
class FakeCallerTrustPolicy(
    private val packageName: String? = null,
    private val digests: Set<String> = emptySet()
) : CallerTrustPolicy {
    override fun expectedPackageName(): String? = packageName
    override fun expectedSigningCertificateDigests(): Set<String> = digests
}
