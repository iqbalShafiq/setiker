package data.billing

enum class BillingStorePlatform {
    GOOGLE_PLAY,
    APPLE_APP_STORE
}

expect object PlatformBillingConfig {
    val platform: BillingStorePlatform
}
