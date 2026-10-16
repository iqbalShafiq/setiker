package util

/**
 * Minimal multiplatform logger shared by every source set.
 *
 * Android maps to [android.util.Log], the Apple targets print to stdout. Keeping a single
 * abstraction avoids `android.util.Log` leaking into common code, which would break the KMP
 * metadata compilation and the iOS target.
 */
internal expect object AppLog {
    fun e(tag: String, message: String, throwable: Throwable? = null)
    fun i(tag: String, message: String)
    fun d(tag: String, message: String)
    fun w(tag: String, message: String, throwable: Throwable? = null)
}
