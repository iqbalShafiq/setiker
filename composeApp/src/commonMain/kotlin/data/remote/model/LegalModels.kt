package data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class LegalSummaryDto(
    val privacyUrl: String,
    val termsUrl: String,
    val retentionUrl: String? = null,
    val accountDeletionUrl: String? = null,
    val version: String? = null,
    val effectiveDate: String? = null
)

@Serializable
data class LegalSectionDto(
    val id: String,
    val title: String,
    val body: String
)

@Serializable
data class LegalDocumentDto(
    val title: String,
    val version: String? = null,
    val effectiveDate: String? = null,
    val url: String? = null,
    val summary: String? = null,
    val sections: List<LegalSectionDto> = emptyList()
)

@Serializable
data class LegalRetentionDto(
    val processingHistoryDays: Int = 7,
    val deletedAccountGraceDays: Int = 30,
    val aiInputHandling: String? = null,
    val description: String? = null,
    val sections: List<LegalSectionDto> = emptyList()
)

@Serializable
data class ReportContentRequest(
    val reason: String,
    val details: String? = null
)
