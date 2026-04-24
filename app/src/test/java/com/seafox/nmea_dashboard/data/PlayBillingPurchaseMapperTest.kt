package com.seafox.nmea_dashboard.data

import com.android.billingclient.api.Purchase
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayBillingPurchaseMapperTest {
    @Test
    fun mapsPurchasedPlayPurchaseToUnverifiedRecordByDefault() {
        val purchase = purchase(
            productIds = listOf(" seafox.navigator.yearly "),
            token = "nav-token",
            purchaseState = Purchase.PurchaseState.PURCHASED,
            acknowledged = false,
            purchaseTime = 1_234L,
        )

        val record = PlayBillingPurchaseMapper.toPurchaseRecord(purchase)

        assertEquals(setOf("seafox.navigator.yearly"), record.productIds)
        assertEquals("nav-token", record.purchaseToken)
        assertEquals(BillingPurchaseState.purchased, record.purchaseState)
        assertEquals(PurchaseVerificationStatus.unverified, record.verificationStatus)
        assertFalse(record.acknowledged)
        assertEquals(1_234L, record.purchasedAtEpochMs)
    }

    @Test
    fun mapsPendingAndAcknowledgedPurchaseState() {
        val record = PlayBillingPurchaseMapper.toPurchaseRecord(
            purchase(
                productIds = listOf("seafox.pro.monthly"),
                token = "pending-token",
                purchaseState = PLAY_JSON_PENDING_PURCHASE_STATE,
                acknowledged = true,
            )
        )

        assertEquals(BillingPurchaseState.pending, record.purchaseState)
        assertTrue(record.acknowledged)
    }

    @Test
    fun trimsBlankProductsAndAllowsExplicitVerificationOverride() {
        val records = PlayBillingPurchaseMapper.toPurchaseRecords(
            purchases = listOf(
                purchase(
                    productIds = listOf("", " seafox.chartpack.de_coast "),
                    token = "pack-token",
                    purchaseState = Purchase.PurchaseState.PURCHASED,
                )
            ),
            verificationStatus = PurchaseVerificationStatus.verified,
        )

        assertEquals(1, records.size)
        assertEquals(setOf(BillingCatalog.SEAFOX_PREMIUM_DE_COAST_PRODUCT_ID), records.single().productIds)
        assertEquals(PurchaseVerificationStatus.verified, records.single().verificationStatus)
    }

    @Test
    fun mapsUnknownPlayStateToUnspecified() {
        assertEquals(
            BillingPurchaseState.unspecified,
            PlayBillingPurchaseMapper.toBillingPurchaseState(Purchase.PurchaseState.UNSPECIFIED_STATE),
        )
    }

    private fun purchase(
        productIds: List<String>,
        token: String,
        purchaseState: Int,
        acknowledged: Boolean = false,
        purchaseTime: Long = 1_000L,
    ): Purchase {
        val json = JSONObject()
            .put("orderId", "GPA.1234-5678-9012-34567")
            .put("packageName", "com.seafox.nmea_dashboard")
            .put("productIds", JSONArray(productIds))
            .put("purchaseToken", token)
            .put("purchaseState", purchaseState)
            .put("acknowledged", acknowledged)
            .put("purchaseTime", purchaseTime)
            .toString()
        return Purchase(json, "signature")
    }

    private companion object {
        private const val PLAY_JSON_PENDING_PURCHASE_STATE = 4
    }
}
