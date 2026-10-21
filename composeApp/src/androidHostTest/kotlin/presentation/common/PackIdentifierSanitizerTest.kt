package presentation.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PackIdentifierSanitizerTest {

    @Test
    fun sanitizeReplacesInvalidCharsAndAppendsSuffix() {
        val result = PackIdentifierSanitizer.sanitize("My Pack 😺 / v1", 1234)
        assertTrue(result.endsWith("_1234"))
        assertTrue(result.startsWith("my pack"))
        assertFalse(result.contains("😺"))
        assertFalse(result.contains("/"))
        assertTrue(result.matches(Regex("[a-z0-9_\\-.,' ]+_1234")))
    }

    @Test
    fun sanitizeCollapsesDotsUnderscoresAndFallbacksToPack() {
        val result = PackIdentifierSanitizer.sanitize("..__..", 77)
        assertEquals("pack_77", result)
    }

    @Test
    fun sanitizeLimitsBaseLength() {
        val longName = "a".repeat(200)
        val result = PackIdentifierSanitizer.sanitize(longName, 9)
        assertTrue(result.length <= 115)
    }
}
