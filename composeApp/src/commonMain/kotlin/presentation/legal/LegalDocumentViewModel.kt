package presentation.legal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.remote.LegalApiRepository
import domain.model.LegalDocType
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import presentation.common.toUiText
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.error_cloud_fetch_failed

class LegalDocumentViewModel(
    private val legalApiRepository: LegalApiRepository,
    docTypeParam: String
) : ViewModel() {
    private val docType = runCatching { LegalDocType.valueOf(docTypeParam) }
        .getOrDefault(LegalDocType.PRIVACY)

    private val _state = MutableStateFlow(LegalDocumentState())
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
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching {
                when (docType) {
                    LegalDocType.PRIVACY -> legalApiRepository.getPrivacyDocument()
                    LegalDocType.TERMS -> legalApiRepository.getTermsDocument()
                    LegalDocType.RETENTION -> legalApiRepository.getRetentionDocument()
                    LegalDocType.PERMISSIONS -> legalApiRepository.getPermissionsDocument()
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
}
