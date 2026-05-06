package data.storage

expect class StickerFileStorage {
    suspend fun saveImage(sourcePath: String, fileName: String): String
    suspend fun loadImage(fileName: String): ByteArray?
    suspend fun deleteImage(fileName: String): Boolean
    suspend fun getImagePath(fileName: String): String
    suspend fun imageExists(fileName: String): Boolean
}