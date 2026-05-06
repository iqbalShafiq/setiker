package presentation.navigation

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
object PathEncoder {
    fun encode(path: String): String =
        Base64.encode(path.encodeToByteArray())
            .replace("+", "-")
            .replace("/", "_")
            .replace("=", "")

    fun decode(encoded: String): String {
        val base64 = encoded
            .replace("-", "+")
            .replace("_", "/")
            .let { pad ->
                val padding = when (pad.length % 4) {
                    2 -> "=="
                    3 -> "="
                    else -> ""
                }
                pad + padding
            }
        return Base64.decode(base64).decodeToString()
    }
}
