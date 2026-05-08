package data.storage

expect class StickerFileStorage {
    suspend fun saveImage(sourcePath: String, fileName: String): String
    suspend fun saveBytes(bytes: ByteArray, fileName: String): String
    suspend fun loadImage(fileName: String): ByteArray?
    suspend fun deleteImage(fileName: String): Boolean
    suspend fun getImagePath(fileName: String): String
    suspend fun imageExists(fileName: String): Boolean
    suspend fun convertToWebP(sourcePath: String, outputFileName: String): String
    suspend fun saveTrayImage(sourcePath: String, fileName: String): String
    suspend fun saveStickerImage(sourcePath: String, fileName: String): String
}