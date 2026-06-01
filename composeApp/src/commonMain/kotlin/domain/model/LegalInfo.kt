package domain.model

data class LegalSummary(
    val privacyUrl: String,
    val termsUrl: String,
    val retentionUrl: String? = null,
    val version: String? = null,
    val effectiveDate: String? = null
)
