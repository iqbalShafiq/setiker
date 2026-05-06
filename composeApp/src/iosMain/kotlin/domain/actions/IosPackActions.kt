package domain.actions

class IosPackActions : PackActions {
    override suspend fun sharePack(packId: String) {
        // TODO: Implement iOS share functionality
    }

    override suspend fun addPackToWhatsApp(packId: String, packName: String): Boolean {
        // TODO: Implement iOS WhatsApp integration
        return false
    }

    override fun isWhatsAppInstalled(): Boolean {
        // TODO: Implement iOS WhatsApp check
        return false
    }
}
