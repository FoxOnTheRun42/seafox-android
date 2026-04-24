package com.seafox.nmea_dashboard.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BillingValidationHttpClientTest {
    @Test
    fun requestBodyContainsOnlyValidationInputs() {
        val body = BillingValidationHttpClient.requestBody(
            packageName = "com.seafox.nmea_dashboard",
            request = BillingValidationRequest(
                productIds = setOf("seafox.pro.monthly"),
                purchaseToken = "purchase-token",
                purchaseState = BillingPurchaseState.purchased,
                acknowledged = false,
                purchasedAtEpochMs = 1_234L,
            ),
        )

        val json = JSONObject(body)
        assertEquals("com.seafox.nmea_dashboard", json.getString("packageName"))
        assertEquals("purchase-token", json.getString("purchaseToken"))
        assertEquals("purchased", json.getString("purchaseState"))
        assertFalse(json.getBoolean("acknowledged"))
        assertEquals(1_234L, json.getLong("purchasedAtEpochMs"))
        assertEquals("seafox.pro.monthly", json.getJSONArray("productIds").getString(0))
    }

    @Test
    fun blankEndpointReturnsUnverifiedDecision() {
        val decision = BillingValidationHttpClient.validate(
            endpointUrl = " ",
            packageName = "com.seafox.nmea_dashboard",
            request = BillingValidationRequest(
                productIds = setOf("seafox.pro.monthly"),
                purchaseToken = "purchase-token",
                purchaseState = BillingPurchaseState.purchased,
                acknowledged = false,
                purchasedAtEpochMs = 1_234L,
            ),
        )

        assertEquals("purchase-token", decision.purchaseToken)
        assertEquals(PurchaseVerificationStatus.unverified, decision.verificationStatus)
    }
}
