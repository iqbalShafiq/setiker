package domain.model

enum class SyncState {
    LOCAL_ONLY,
    SYNCED,
    PENDING,
    FAILED
}

enum class SyncOperationType {
    CREATE_PACK,
    UPDATE_PACK,
    DELETE_PACK,
    ADD_STICKER,
    UPDATE_STICKER,
    DELETE_STICKER,
    UPLOAD_IMAGE,
    REORDER_STICKERS
}

enum class SyncOperationStatus {
    PENDING,
    IN_PROGRESS,
    SUCCESS,
    FAILED,
    CANCELLED
}
