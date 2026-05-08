package domain.error

enum class AppErrorCode {
    BackgroundRemoveRequestFailed,
    InvalidBackgroundRemoveResponse,
    GenerateRequestFailed,
    InvalidGenerateResponse,
    GridSplitRequestFailed,
    InvalidGridSplitResponse,
    ImageDownloadFailed,
    PackNotFound,
    StickerNotFound
}
