package data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class LegalSummaryDto(
    val privacyUrl: String,
    val termsUrl: String,
    val retentionUrl: String? = null,
    val version: String? = null,
    val effectiveDate: String? = null
)

@Serializable
data class LegalDocumentDto(
    val title: String,
    val version: String? = null,
    val effectiveDate: String? = null,
    val url: String? = null,
    val summary: String? = null
)

@Serializable
data class LegalRetentionDto(
    val processingHistoryDays: Int = 7,
    val deletedAccountGraceDays: Int = 30,
    val aiInputHandling: String? = null,
    val description: String? = null
)
