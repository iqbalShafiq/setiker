package domain.model

data class LegalSummary(
    val privacyUrl: String,
    val termsUrl: String,
    val retentionUrl: String? = null,
    val accountDeletionUrl: String? = null,
    val version: String? = null,
    val effectiveDate: String? = null
)

data class LegalSection(
    val id: String,
    val title: String,
    val body: String
)

data class LegalDocument(
    val title: String,
    val version: String? = null,
    val effectiveDate: String? = null,
    val url: String? = null,
    val summary: String? = null,
    val sections: List<LegalSection> = emptyList()
)

enum class LegalDocType {
    PRIVACY,
    TERMS,
    RETENTION,
    PERMISSIONS,
    ACCOUNT_DELETION
}
