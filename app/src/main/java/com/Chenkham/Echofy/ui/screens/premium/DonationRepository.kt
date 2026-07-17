package com.Chenkham.Echofy.ui.screens.premium

import android.content.Context
import com.Chenkham.Echofy.appwrite.AppwriteClientProvider
import com.Chenkham.Echofy.appwrite.AppwriteConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import io.appwrite.Query
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DonationRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun loadTopSupporters(): List<DonationSupporter> =
        loadSupporters(
            listOf(
                Query.equal("verified", true),
                Query.orderDesc("amount"),
                Query.limit(10),
            ),
        )

    suspend fun loadLatestSupporters(): List<DonationSupporter> =
        loadSupporters(
            listOf(
                Query.equal("verified", true),
                Query.orderDesc("createdAtEpochMs"),
                Query.limit(10),
            ),
        )

    private suspend fun loadSupporters(queries: List<String>): List<DonationSupporter> {
        val result = AppwriteClientProvider.databases(context).listDocuments(
            databaseId = AppwriteConfig.DATABASE_ID,
            collectionId = AppwriteConfig.COL_DONATIONS,
            queries = queries,
        )
        return result.documents.mapNotNull { document ->
            DonationSupporter.fromMap(document.data)
        }
    }
}

data class DonationSupporter(
    val name: String,
    val amount: Double,
    val currency: String,
    val amountText: String,
    val instagram: String?,
    val message: String?,
    val createdAtEpochMs: Long,
) {
    companion object {
        fun fromMap(data: Map<String, Any>): DonationSupporter? {
            val amount = data.doubleValue("amount") ?: return null
            val currency = data.stringValue("currency")?.ifBlank { "USD" } ?: "USD"
            return DonationSupporter(
                name = data.stringValue("name")?.ifBlank { "Anonymous" } ?: "Anonymous",
                amount = amount,
                currency = currency,
                amountText = data.stringValue("amountText")?.ifBlank { formatAmount(amount, currency) } ?: formatAmount(amount, currency),
                instagram = data.stringValue("instagram")?.ifBlank { null },
                message = data.stringValue("message")?.ifBlank { null },
                createdAtEpochMs = data.longValue("createdAtEpochMs") ?: 0L,
            )
        }

        private fun formatAmount(amount: Double, currency: String): String {
            val value = if (amount % 1.0 == 0.0) amount.toInt().toString() else String.format("%.2f", amount)
            return when (currency.uppercase()) {
                "INR" -> "₹$value"
                "USD" -> "$$value"
                "EUR" -> "€$value"
                "GBP" -> "£$value"
                else -> "$value ${currency.uppercase()}"
            }
        }
    }
}

private fun Map<String, Any>.stringValue(key: String): String? = this[key] as? String

private fun Map<String, Any>.doubleValue(key: String): Double? = when (val value = this[key]) {
    is Double -> value
    is Float -> value.toDouble()
    is Int -> value.toDouble()
    is Long -> value.toDouble()
    is String -> value.toDoubleOrNull()
    else -> null
}

private fun Map<String, Any>.longValue(key: String): Long? = when (val value = this[key]) {
    is Long -> value
    is Int -> value.toLong()
    is Double -> value.toLong()
    is Float -> value.toLong()
    is String -> value.toLongOrNull()
    else -> null
}
