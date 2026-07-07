package presentation.packdetail

sealed interface PackDetailIntent {
    /**
     * @param silentRefresh When true and this pack is already in state, skips the full-screen blocking loader
     * (smooth return e.g. from sticker editor).
     */
    data class LoadPack(val packId: String, val silentRefresh: Boolean = false) : PackDetailIntent
    data class AddToWhatsApp(val packId: String) : PackDetailIntent
    data class DeletePack(val packId: String) : PackDetailIntent
    data object EditPack : PackDetailIntent
    data class DeleteSticker(val index: Int) : PackDetailIntent
    data object AddSticker : PackDetailIntent
    data class StageStickerImports(val imagePaths: List<String>) : PackDetailIntent
    data object DismissStickerImportSheet : PackDetailIntent
    data class ApplyCroppedStickerImport(val croppedPath: String) : PackDetailIntent
    data class EditSticker(val index: Int) : PackDetailIntent
    data object OpenCloudShareSheet : PackDetailIntent
    data object DismissCloudShareSheet : PackDetailIntent
    data object RefreshCloudShareLinks : PackDetailIntent
    data object CreateCloudShareLink : PackDetailIntent
    data class RevokeCloudShareLink(val linkId: String) : PackDetailIntent
    data object RequestDuplicate : PackDetailIntent
    data object ConfirmDuplicate : PackDetailIntent
    data object DismissDuplicateDialog : PackDetailIntent
    data object OpenCollaboratorsSheet : PackDetailIntent
    data object DismissCollaboratorsSheet : PackDetailIntent
    data object RefreshCollaborators : PackDetailIntent
    data class CollaboratorSearchChanged(val query: String) : PackDetailIntent
    data class InviteCollaborator(val userId: String) : PackDetailIntent
    data class RemoveCollaborator(val userId: String) : PackDetailIntent
    data class CollaboratorPermissionChanged(val permission: String) : PackDetailIntent
    data object RequestMakePublic : PackDetailIntent
    data object RequestUnpublish : PackDetailIntent
    data object AcceptContentPolicy : PackDetailIntent
    data object DismissContentPolicy : PackDetailIntent
    data object ConfirmVisibilityChange : PackDetailIntent
    data object DismissVisibilityDialog : PackDetailIntent
}
