package com.seafox.nmea_dashboard.data

import org.json.JSONObject
import java.util.Locale

object BillingValidationJson {
    fun toDecision(
        purchaseToken: String,
        json: String,
    ): BillingValidationDecision {
        val root = JSONObject(json)
        return BillingValidationDecision(
            purchaseToken = purchaseToken,
            verificationStatus = parseVerificationStatus(root.optString("verificationStatus")),
            validUntilEpochMs = root.optLongOrNull("validUntilEpochMs"),
        )
    }

    fun toJson(decision: BillingValidationDecision): String {
        val root = JSONObject()
            .put("verificationStatus", decision.verificationStatus.name)
        decision.validUntilEpochMs?.let { validUntilEpochMs ->
            root.put("validUntilEpochMs", validUntilEpochMs.coerceAtLeast(0L))
        }
        return root.toString()
    }

    private fun parseVerificationStatus(value: String): PurchaseVerificationStatus {
        return when (value.trim().lowercase(Locale.ROOT)) {
            "verified" -> PurchaseVerificationStatus.verified
            "rejected" -> PurchaseVerificationStatus.rejected
            else -> PurchaseVerificationStatus.unverified
        }
    }

    private fun JSONObject.optLongOrNull(key: String): Long? {
        if (!has(key) || isNull(key)) return null
        return optLong(key).coerceAtLeast(0L)
    }
}
