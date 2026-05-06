package domain.actions

interface PackActions {
    suspend fun sharePack(packId: String)
    suspend fun addPackToWhatsApp(packId: String, packName: String): Boolean
    fun isWhatsAppInstalled(): Boolean
}
