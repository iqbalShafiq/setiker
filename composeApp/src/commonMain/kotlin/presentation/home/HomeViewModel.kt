package presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.auth.AuthManager
import data.sync.SyncManager
import domain.model.StickerPack
import domain.model.SyncOperationStatus
import domain.repository.StickerRepository
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
import setiker.composeapp.generated.resources.error_failed_add_pack
import setiker.composeapp.generated.resources.error_failed_delete_pack
import setiker.composeapp.generated.resources.error_pack_min_stickers_whatsapp
import setiker.composeapp.generated.resources.success_pack_added_whatsapp

class HomeViewModel(
    private val repository: StickerRepository,
    private val authManager: AuthManager,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val _effect = Channel<HomeEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        observeAuthState()
        observeSyncState()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            authManager.currentUser.collect { user ->
                _state.update { it.copy(currentUser = user) }
            }
        }
    }

    private fun observeSyncState() {
        viewModelScope.launch {
            syncManager.isSyncing.collect { isSyncing ->
                _state.update { it.copy(isSyncing = isSyncing) }
            }
        }
        viewModelScope.launch {
            syncManager.operationsFlow.collect { operations ->
                val pendingCount = operations.count { it.status == SyncOperationStatus.PENDING }
                _state.update { it.copy(pendingSyncCount = pendingCount) }
            }
        }
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.LoadPacks -> loadPacks()
            is HomeIntent.DeletePack -> deletePack(intent.packId)
            is HomeIntent.AddToWhatsApp -> addToWhatsApp(intent.packId)
            is HomeIntent.CreateNewPack -> {
                viewModelScope.launch {
                    _effect.send(HomeEffect.NavigateToCreatePack)
                }
            }
            is HomeIntent.NavigateToProfile -> {
                viewModelScope.launch {
                    _effect.send(HomeEffect.NavigateToProfile)
                }
            }
            is HomeIntent.NavigateToSync -> {
                viewModelScope.launch {
                    _effect.send(HomeEffect.NavigateToSync)
                }
            }
            is HomeIntent.NavigateToLogin -> {
                viewModelScope.launch {
                    _effect.send(HomeEffect.NavigateToLogin)
                }
            }
            is HomeIntent.RefreshSync -> {
                viewModelScope.launch {
                    syncManager.sync()
                }
            }
        }
    }

    private fun loadPacks() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val packs = repository.getAllPacks()
                _state.update { it.copy(isLoading = false, packs = packs) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun deletePack(packId: String) {
        viewModelScope.launch {
            try {
                repository.deletePack(packId)
                loadPacks()
            } catch (e: Exception) {
                _effect.send(
                    HomeEffect.ShowError(
                        e.toUiText(Res.string.error_failed_delete_pack)
                    )
                )
            }
        }
    }

    private fun addToWhatsApp(packId: String) {
        viewModelScope.launch {
            try {
                val pack = repository.getPack(packId)
                if (pack.stickers.size < StickerPack.MIN_STICKERS) {
                    _effect.send(
                        HomeEffect.ShowError(
                            UiText.StringRes(
                                Res.string.error_pack_min_stickers_whatsapp,
                                listOf(StickerPack.MIN_STICKERS)
                            )
                        )
                    )
                    return@launch
                }
                _effect.send(HomeEffect.ShowSuccess(UiText.StringRes(Res.string.success_pack_added_whatsapp)))
            } catch (e: Exception) {
                _effect.send(
                    HomeEffect.ShowError(
                        e.toUiText(Res.string.error_failed_add_pack)
                    )
                )
            }
        }
    }
}
