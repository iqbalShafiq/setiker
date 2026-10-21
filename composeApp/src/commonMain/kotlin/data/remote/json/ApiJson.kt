package data.remote.json

import kotlinx.serialization.json.Json

/**
 * The one codec for every Setiker API payload, AI job payload and locally-persisted DTO.
 *
 * Config is the (audit-approved) superset used today: unknown keys are ignored defensively
 * (`ignoreUnknownKeys`) and defaults are always written so backend contracts keep seeing
 * explicit values (`encodeDefaults`). Strictness (`isLenient`) is intentionally OFF for API
 * bodies; `data.remote.ApiErrorParser` keeps its own lenient Json because it parses *error*
 * bodies of arbitrary shape (documented exception in `docs/specs/p3b-data-layer-consolidation.md`
 * D2).
 */
object ApiJson {
    val codec: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
}
