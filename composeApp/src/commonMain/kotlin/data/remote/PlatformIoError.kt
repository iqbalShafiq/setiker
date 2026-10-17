package data.remote

/**
 * Platform-specific recognition of "connection-level" IO failures (DNS, socket reset, network
 * dropped). JVM/Android surfaces `java.io.IOException`; Kotlin/Native surfaces Ktor's
 * `io.ktor.utils.io.errors.IOException`.
 */
expect fun Throwable.isPlatformIoException(): Boolean
