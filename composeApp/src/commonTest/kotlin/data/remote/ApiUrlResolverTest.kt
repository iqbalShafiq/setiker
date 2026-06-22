package data.remote

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ApiUrlResolverTest {

    private val baseUrl = "http://13.251.98.162:8080"

    @Test
    fun `returns null for blank url`() {
        assertNull(resolveApiUrl(null, baseUrl))
        assertNull(resolveApiUrl("   ", baseUrl))
    }

    @Test
    fun `prefixes relative upload path`() {
        val url = "/uploads/users/u1/stickers/sticker-0.png"
        assertEquals(
            "http://13.251.98.162:8080/uploads/users/u1/stickers/sticker-0.png",
            resolveApiUrl(url, baseUrl)
        )
    }

    @Test
    fun `prefixes path without leading slash`() {
        assertEquals(
            "http://13.251.98.162:8080/uploads/a.png",
            resolveApiUrl("uploads/a.png", baseUrl)
        )
    }

    @Test
    fun `keeps absolute remote url`() {
        val absolute = "https://cdn.example.com/sticker.png"
        assertEquals(absolute, resolveApiUrl(absolute, baseUrl))
    }

    @Test
    fun `rewrites localhost absolute url to api base`() {
        assertEquals(
            "http://13.251.98.162:8080/uploads/a.png",
            resolveApiUrl("http://localhost:8080/uploads/a.png", baseUrl)
        )
    }
}
