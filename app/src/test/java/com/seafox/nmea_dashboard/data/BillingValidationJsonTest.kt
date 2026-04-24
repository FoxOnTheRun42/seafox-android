package com.seafox.nmea_dashboard.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BillingValidationJsonTest {
    @Test
    fun parsesVerifiedResponseForKnownPurchaseToken() {
        val decision = BillingValidationJson.toDecision(
            purchaseToken = "token-123",
            json = """{"verificationStatus":"verified","validUntilEpochMs":2000}""",
        )

        assertEquals("token-123", decision.purchaseToken)
        assertEquals(PurchaseVerificationStatus.verified, decision.verificationStatus)
        assertEquals(2_000L, decision.validUntilEpochMs)
    }

    @Test
    fun unknownOrBlankStatusDefaultsToUnverified() {
        val blank = BillingValidationJson.toDecision("blank-token", """{"verificationStatus":""}""")
        val unknown = BillingValidationJson.toDecision("unknown-token", """{"verificationStatus":"accepted"}""")

        assertEquals(PurchaseVerificationStatus.unverified, blank.verificationStatus)
        assertEquals(PurchaseVerificationStatus.unverified, unknown.verificationStatus)
    }

    @Test
    fun rejectedResponseDoesNotNeedExpiry() {
        val decision = BillingValidationJson.toDecision(
            purchaseToken = "rejected-token",
            json = """{"verificationStatus":"rejected"}""",
        )

        assertEquals(PurchaseVerificationStatus.rejected, decision.verificationStatus)
        assertEquals(null, decision.validUntilEpochMs)
    }

    @Test
    fun serializesStableDecisionWithoutTokenLeak() {
        val json = BillingValidationJson.toJson(
            BillingValidationDecision(
                purchaseToken = "server-token-is-not-echoed",
                verificationStatus = PurchaseVerificationStatus.verified,
                validUntilEpochMs = 2_000L,
            )
        )

        assertTrue(json.contains("\"verificationStatus\":\"verified\""))
        assertTrue(json.contains("\"validUntilEpochMs\":2000"))
        assertFalse(json.contains("server-token-is-not-echoed"))
    }
}
