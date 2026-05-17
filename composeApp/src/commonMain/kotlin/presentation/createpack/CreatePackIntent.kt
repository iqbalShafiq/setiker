package presentation.createpack

sealed interface CreatePackIntent {
    data class UpdateName(val name: String) : CreatePackIntent
    data class UpdatePublisher(val publisher: String) : CreatePackIntent
    data class UpdateVisibility(val visibility: String) : CreatePackIntent
    data class UpdateTrayImage(val imagePath: String) : CreatePackIntent
    data class AddSticker(val imagePath: String) : CreatePackIntent
    data class AddAnimatedDraft(val draft: DraftSticker) : CreatePackIntent
    data class RemoveSticker(val index: Int) : CreatePackIntent
    data class UpdateGeneratePrompt(val prompt: String) : CreatePackIntent
    data class UpdateGenerateInputImage(val path: String?) : CreatePackIntent
    data class UpdateGridSplitSource(val path: String) : CreatePackIntent
    data object GenerateStickers : CreatePackIntent
    data object ImprovePackStickers : CreatePackIntent
    data class ToggleGeneratedSelection(val index: Int) : CreatePackIntent
    data object AddSelectedGeneratedToPack : CreatePackIntent
    data object ReplacePackWithGenerated : CreatePackIntent
    data object CloseGeneratedSheet : CreatePackIntent
    data object RunGridSplit : CreatePackIntent
    data class ToggleSplitSelection(val index: Int) : CreatePackIntent
    data object AddSelectedSplitToPack : CreatePackIntent
    data object ClearSplitPreview : CreatePackIntent
    data object OpenAiGenerateSheet : CreatePackIntent
    data object CloseAiGenerateSheet : CreatePackIntent
    data object OpenGridConfirmSheet : CreatePackIntent
    data object CloseGridSheet : CreatePackIntent
    data object SavePack : CreatePackIntent
    data class LoadPack(val packId: String) : CreatePackIntent
    data class StageStickerGalleryPick(val path: String) : CreatePackIntent
    data object DismissStickerGalleryCropPrompt : CreatePackIntent
    data class StageTrayGalleryPick(val path: String) : CreatePackIntent
    data object DismissTrayGalleryCropPrompt : CreatePackIntent
}
