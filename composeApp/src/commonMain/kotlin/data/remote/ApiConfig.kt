package data.remote

expect object ApiConfig {
    val baseUrl: String
    val isDebugLoggingEnabled: Boolean
}
