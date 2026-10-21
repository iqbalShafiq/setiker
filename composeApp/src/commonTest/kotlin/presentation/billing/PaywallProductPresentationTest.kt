package presentation.billing

import domain.billing.BillingProductUiModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * F5: the paywall must never offer a "Buy" button without a price, and must not repeat the same
 * token amount twice on the same card.
 */
class PaywallProductPresentationTest {

    @Test
    fun `a product without a store price is not purchasable`() {
        val presentation = paywallProductPresentation(tokenPack(tokens = 100), tokenLabel = "100 tokens")

        assertNull(presentation.price)
        assertFalse(presentation.isPurchasable)
    }

    @Test
    fun `a product with a store price is purchasable and shows the price`() {
        val presentation = paywallProductPresentation(
            tokenPack(tokens = 100).copy(formattedPrice = "IDR 25.000"),
            tokenLabel = "100 tokens"
        )

        assertEquals("IDR 25.000", presentation.price)
        assertTrue(presentation.isPurchasable)
    }

    @Test
    fun `a blank price counts as no price`() {
        val presentation = paywallProductPresentation(
            tokenPack(tokens = 100).copy(formattedPrice = "   "),
            tokenLabel = "100 tokens"
        )

        assertNull(presentation.price)
        assertFalse(presentation.isPurchasable)
    }

    @Test
    fun `the token line is not repeated when the description already states the amount`() {
        val presentation = paywallProductPresentation(
            tokenPack(tokens = 100).copy(description = "100 AI tokens (non-expiring)"),
            tokenLabel = "100 tokens (non-expiring)"
        )

        assertEquals("100 AI tokens (non-expiring)", presentation.description)
        assertNull(presentation.tokenLabel)
    }

    @Test
    fun `the token line is used when there is no description`() {
        val presentation = paywallProductPresentation(
            tokenPack(tokens = 300).copy(description = null),
            tokenLabel = "300 tokens (non-expiring)"
        )

        assertNull(presentation.description)
        assertEquals("300 tokens (non-expiring)", presentation.tokenLabel)
    }

    @Test
    fun `a subscription shows its description and no token line`() {
        val presentation = paywallProductPresentation(
            BillingProductUiModel(
                code = "premium_monthly",
                type = "subscription",
                name = "Premium Monthly",
                description = "500 daily AI points + Premium badge",
                tokenAmount = null,
                tierCode = "premium",
                dailyPointLimit = 500,
                storeProductId = "premium_monthly",
                formattedPrice = "IDR 45.000"
            ),
            tokenLabel = null
        )

        assertEquals("500 daily AI points + Premium badge", presentation.description)
        assertNull(presentation.tokenLabel)
        assertEquals("IDR 45.000", presentation.price)
        assertTrue(presentation.isPurchasable)
    }

    private fun tokenPack(tokens: Int) = BillingProductUiModel(
        code = "token_pack_s",
        type = "consumable_token_pack",
        name = "Token Pack S",
        description = "100 AI tokens (non-expiring)",
        tokenAmount = tokens,
        tierCode = null,
        dailyPointLimit = null,
        storeProductId = "token_pack_s",
        formattedPrice = null
    )
}
