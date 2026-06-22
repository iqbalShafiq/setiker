package data.aijob

import kotlinx.serialization.json.Json

object AiJobJson {
    val codec: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
}
