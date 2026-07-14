package presentation.legal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.auth.AuthManager
import data.remote.ApiException
import data.remote.LegalApiRepository
import domain.model.LegalDocType
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import presentation.common.UiText
import presentation.common.toUiText
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.account_deletion_confirm_required
import setiker.composeapp.generated.resources.account_deletion_email_required
import setiker.composeapp.generated.resources.account_deletion_request_failed
import setiker.composeapp.generated.resources.account_deletion_request_success
import setiker.composeapp.generated.resources.error_cloud_fetch_failed

class LegalDocumentViewModel(
    private val legalApiRepository: LegalApiRepository,
    private val authManager: AuthManager,
    docTypeParam: String
) : ViewModel() {
    private val docType = runCatching { LegalDocType.valueOf(docTypeParam) }
        .getOrDefault(LegalDocType.PRIVACY)

    private val _state = MutableStateFlow(
        LegalDocumentState(showDeletionForm = docType == LegalDocType.ACCOUNT_DELETION)
    )
    val state: StateFlow<LegalDocumentState> = _state.asStateFlow()

    private val _effect = Channel<LegalDocumentEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        onIntent(LegalDocumentIntent.Load)
    }

    fun onIntent(intent: LegalDocumentIntent) {
        when (intent) {
            LegalDocumentIntent.Load -> load()
            LegalDocumentIntent.NavigateBack -> viewModelScope.launch {
                _effect.send(LegalDocumentEffect.NavigateBack)
            }
            is LegalDocumentIntent.UpdateDeletionEmail -> _state.update {
                it.copy(
                    deletionEmail = intent.email,
                    deletionSubmitError = null,
                    deletionSubmitSuccessMessage = null
                )
            }
            is LegalDocumentIntent.UpdateDeletionReason -> _state.update {
                it.copy(deletionReason = intent.reason)
            }
            is LegalDocumentIntent.SetDeletionConfirmed -> _state.update {
                it.copy(
                    deletionConfirmed = intent.confirmed,
                    deletionSubmitError = null
                )
            }
            LegalDocumentIntent.SubmitDeletionRequest -> submitDeletionRequest()
        }
    }

    private fun load() {
        viewModelScope.launch {
            val prefilledEmail = if (docType == LegalDocType.ACCOUNT_DELETION) {
                authManager.getUser()?.email.orEmpty()
            } else {
                ""
            }
            _state.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    deletionEmail = prefilledEmail.ifBlank { it.deletionEmail }
                )
            }
            runCatching {
                when (docType) {
                    LegalDocType.PRIVACY -> legalApiRepository.getPrivacyDocument()
                    LegalDocType.TERMS -> legalApiRepository.getTermsDocument()
                    LegalDocType.RETENTION -> legalApiRepository.getRetentionDocument()
                    LegalDocType.PERMISSIONS -> legalApiRepository.getPermissionsDocument()
                    LegalDocType.ACCOUNT_DELETION -> legalApiRepository.getAccountDeletionDocument()
                }
            }.onSuccess { document ->
                _state.update { it.copy(isLoading = false, document = document) }
            }.onFailure { error ->
                _state.update {
                    it.copy(isLoading = false, error = error.toUiText(Res.string.error_cloud_fetch_failed))
                }
            }
        }
    }

    private fun submitDeletionRequest() {
        if (docType != LegalDocType.ACCOUNT_DELETION) return
        val current = _state.value
        if (current.isSubmittingDeletion) return

        val email = current.deletionEmail.trim()
        if (email.isBlank() || !email.contains('@')) {
            _state.update {
                it.copy(deletionSubmitError = UiText.StringRes(Res.string.account_deletion_email_required))
            }
            return
        }
        if (!current.deletionConfirmed) {
            _state.update {
                it.copy(deletionSubmitError = UiText.StringRes(Res.string.account_deletion_confirm_required))
            }
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isSubmittingDeletion = true,
                    deletionSubmitError = null,
                    deletionSubmitSuccessMessage = null
                )
            }
            runCatching {
                legalApiRepository.requestAccountDeletion(
                    email = email,
                    reason = current.deletionReason.takeIf { it.isNotBlank() },
                    confirmed = true
                )
            }.onSuccess { message ->
                _state.update {
                    it.copy(
                        isSubmittingDeletion = false,
                        deletionSubmitSuccessMessage = if (message.isNotBlank()) {
                            UiText.DynamicString(message)
                        } else {
                            UiText.StringRes(Res.string.account_deletion_request_success)
                        },
                        deletionReason = "",
                        deletionConfirmed = false
                    )
                }
            }.onFailure { error ->
                val apiMessage = (error as? ApiException)?.message?.takeIf { it.isNotBlank() }
                _state.update {
                    it.copy(
                        isSubmittingDeletion = false,
                        deletionSubmitError = apiMessage?.let { msg -> UiText.DynamicString(msg) }
                            ?: error.toUiText(Res.string.account_deletion_request_failed)
                    )
                }
            }
        }
    }
}
