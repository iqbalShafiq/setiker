package presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.auth.AuthManager
import data.remote.StickerApiRepository
import data.repository.StickerPackDraftSaver
import data.sync.SyncManager
import domain.model.StickerDraftInput
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
import presentation.common.PackIdentifierSanitizer
import presentation.common.toUiText
import kotlin.random.Random
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.error_failed_add_pack
import setiker.composeapp.generated.resources.error_failed_delete_pack
import setiker.composeapp.generated.resources.error_failed_generate_sticker_pack
import setiker.composeapp.generated.resources.error_pack_min_stickers_whatsapp
import setiker.composeapp.generated.resources.error_pack_name_required
import setiker.composeapp.generated.resources.error_prompt_required
import setiker.composeapp.generated.resources.error_publisher_required
import setiker.composeapp.generated.resources.success_pack_added_whatsapp

class HomeViewModel(
    private val repository: StickerRepository,
    private val authManager: AuthManager,
    private val syncManager: SyncManager,
    private val apiRepository: StickerApiRepository,
    private val draftSaver: StickerPackDraftSaver
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
            is HomeIntent.NavigateToExplore -> {
                viewModelScope.launch {
                    _effect.send(HomeEffect.NavigateToExplore)
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
            is HomeIntent.SearchQueryChanged -> {
                _state.update { it.copy(searchQuery = intent.query) }
            }
            is HomeIntent.SortOrderChanged -> {
                _state.update { it.copy(sortOrder = intent.order) }
            }
            is HomeIntent.OpenGeneratePackSheet -> {
                _state.update { it.copy(isGeneratePackSheetOpen = true) }
            }
            is HomeIntent.CloseGeneratePackSheet -> {
                _state.update { it.copy(isGeneratePackSheetOpen = false) }
            }
            is HomeIntent.UpdateGeneratePackPrompt -> {
                _state.update { it.copy(generatePackPrompt = intent.prompt) }
            }
            is HomeIntent.UpdateGeneratePackName -> {
                _state.update { it.copy(generatePackName = intent.name) }
            }
            is HomeIntent.UpdateGeneratePackPublisher -> {
                _state.update { it.copy(generatePackPublisher = intent.publisher) }
            }
            is HomeIntent.UpdateGeneratePackLayout -> {
                _state.update { it.copy(generatePackLayout = intent.layout) }
            }
            is HomeIntent.UpdateGeneratePackInputImage -> {
                _state.update { it.copy(generatePackInputImagePath = intent.path) }
            }
            is HomeIntent.GenerateStickerPack -> generateStickerPack()
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

    private fun generateStickerPack() {
        viewModelScope.launch {
            val current = _state.value
            if (current.isGeneratePackLoading) return@launch
            if (current.generatePackName.isBlank()) {
                _effect.send(HomeEffect.ShowError(UiText.StringRes(Res.string.error_pack_name_required)))
                return@launch
            }
            if (current.generatePackPublisher.isBlank()) {
                _effect.send(HomeEffect.ShowError(UiText.StringRes(Res.string.error_publisher_required)))
                return@launch
            }
            if (current.generatePackPrompt.isBlank()) {
                _effect.send(HomeEffect.ShowError(UiText.StringRes(Res.string.error_prompt_required)))
                return@launch
            }

            _state.update { it.copy(isGeneratePackLoading = true, error = null) }
            try {
                val generated = apiRepository.generateStickerPack(
                    prompt = current.generatePackPrompt,
                    layout = current.generatePackLayout,
                    inputImagePath = current.generatePackInputImagePath
                )
                val trayImagePath = generated.firstOrNull()?.localPath
                if (trayImagePath.isNullOrBlank()) {
                    throw IllegalStateException("No generated sticker returned")
                }
                val identifier = PackIdentifierSanitizer.sanitize(
                    rawName = current.generatePackName,
                    suffix = Random.nextInt(1000, 9999)
                )
                val pack = draftSaver.buildDraftPack(
                    StickerDraftInput(
                        identifier = identifier,
                        name = current.generatePackName,
                        publisher = current.generatePackPublisher,
                        visibility = "PRIVATE",
                        trayImagePath = trayImagePath,
                        stickers = generated.map { file ->
                            StickerDraftInput.StickerInput(
                                imagePath = file.localPath,
                                decorations = file.decorations
                            )
                        }
                    )
                )
                repository.savePack(pack)
                val packs = repository.getAllPacks()
                _state.update {
                    it.copy(
                        packs = packs,
                        isGeneratePackLoading = false,
                        isGeneratePackSheetOpen = false,
                        generatePackPrompt = "",
                        generatePackName = "",
                        generatePackPublisher = "",
                        generatePackLayout = "4x4",
                        generatePackInputImagePath = null
                    )
                }
                _effect.send(HomeEffect.NavigateToPackDetail(pack.identifier))
            } catch (e: Exception) {
                _state.update { it.copy(isGeneratePackLoading = false, error = e.message) }
                _effect.send(HomeEffect.ShowError(e.toUiText(Res.string.error_failed_generate_sticker_pack)))
            }
        }
    }
}
