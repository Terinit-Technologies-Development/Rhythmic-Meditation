package com.terinit.rhythmicmeditation.integration.contract

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CallerVerifierTest {

    private class FixedTrustPolicy(
        private val packageName: String?,
        private val digests: Set<String>
    ) : CallerTrustPolicy {
        override fun expectedPackageName(): String? = packageName
        override fun expectedSigningCertificateDigests(): Set<String> = digests
    }

    @Test
    fun `unconfigured peer rejects every caller`() {
        val verifier = CallerVerifier(FixedTrustPolicy(null, emptySet()))
        val result = verifier.verifyCaller("com.terinit.rhythmicroutine", setOf("AB:CD"))
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }

    @Test
    fun `unknown caller package is rejected`() {
        val verifier = CallerVerifier(FixedTrustPolicy("com.terinit.rhythmicroutine", setOf("AB:CD")))
        assertTrue(verifier.verifyCaller(null, setOf("AB:CD")).isFailure)
        assertTrue(verifier.verifyCaller(" ", setOf("AB:CD")).isFailure)
    }

    @Test
    fun `wrong caller package is rejected`() {
        val verifier = CallerVerifier(FixedTrustPolicy("com.terinit.rhythmicroutine", setOf("AB:CD")))
        assertTrue(
            verifier.verifyCaller("com.evil.app", setOf("AB:CD")).isFailure
        )
    }

    @Test
    fun `matching package without configured digests is rejected`() {
        // Deny-by-default: until peer signing certificates are configured,
        // even the right package name must not pass.
        val verifier = CallerVerifier(FixedTrustPolicy("com.terinit.rhythmicroutine", emptySet()))
        assertTrue(
            verifier.verifyCaller("com.terinit.rhythmicroutine", setOf("AB:CD")).isFailure
        )
    }

    @Test
    fun `matching package with wrong signature is rejected`() {
        val verifier = CallerVerifier(FixedTrustPolicy("com.terinit.rhythmicroutine", setOf("AB:CD")))
        assertTrue(
            verifier.verifyCaller("com.terinit.rhythmicroutine", setOf("FF:00")).isFailure
        )
    }

    @Test
    fun `matching package and signature is accepted`() {
        val verifier = CallerVerifier(FixedTrustPolicy("com.terinit.rhythmicroutine", setOf("AB:CD")))
        val result = verifier.verifyCaller("com.terinit.rhythmicroutine", setOf("FF:00", "AB:CD"))
        assertTrue(result.isSuccess)
        assertEquals(Unit, result.getOrNull())
    }
}
