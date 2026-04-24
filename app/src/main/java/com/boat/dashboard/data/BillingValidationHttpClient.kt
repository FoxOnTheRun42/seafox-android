package com.seafox.nmea_dashboard.data

import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

object BillingValidationHttpClient {
    fun requestBody(
        packageName: String,
        request: BillingValidationRequest,
    ): String {
        return JSONObject()
            .put("packageName", packageName)
            .put("productIds", JSONArray(request.productIds.toList()))
            .put("purchaseToken", request.purchaseToken)
            .put("purchaseState", request.purchaseState.name)
            .put("acknowledged", request.acknowledged)
            .put("purchasedAtEpochMs", request.purchasedAtEpochMs.coerceAtLeast(0L))
            .toString()
    }

    fun validate(
        endpointUrl: String,
        packageName: String,
        request: BillingValidationRequest,
        connectTimeoutMs: Int = 5_000,
        readTimeoutMs: Int = 8_000,
    ): BillingValidationDecision {
        val trimmedUrl = endpointUrl.trim()
        if (trimmedUrl.isBlank()) {
            return BillingValidationDecision(
                purchaseToken = request.purchaseToken,
                verificationStatus = PurchaseVerificationStatus.unverified,
            )
        }

        val connection = (URL(trimmedUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
        }

        return try {
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(requestBody(packageName = packageName, request = request))
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                BillingValidationDecision(
                    purchaseToken = request.purchaseToken,
                    verificationStatus = PurchaseVerificationStatus.unverified,
                )
            } else {
                val response = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                BillingValidationJson.toDecision(
                    purchaseToken = request.purchaseToken,
                    json = response,
                )
            }
        } finally {
            connection.disconnect()
        }
    }
}
