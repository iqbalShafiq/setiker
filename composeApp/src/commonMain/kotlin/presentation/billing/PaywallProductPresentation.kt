package presentation.billing

import domain.billing.BillingProductUiModel

/**
 * The resolved content of one paywall product card.
 *
 * @property price the store-formatted price, or `null` when the store did not return one.
 * @property isPurchasable whether the Buy action may be triggered. A product without a price must
 *   never be purchasable: the user would have no idea what they are paying.
 */
internal data class PaywallProductPresentation(
    val title: String,
    val description: String?,
    val tokenLabel: String?,
    val price: String?,
    val isPurchasable: Boolean
)

/**
 * Builds the card content for a billing product.
 *
 * @param tokenLabel the pre-rendered "N tokens (non-expiring)" line, or `null` when the product has
 *   no token amount.
 */
internal fun paywallProductPresentation(
    product: BillingProductUiModel,
    tokenLabel: String?
): PaywallProductPresentation {
    val price = product.formattedPrice?.trim()?.takeIf { it.isNotEmpty() }
    val description = product.description?.trim()?.takeIf { it.isNotEmpty() }
    return PaywallProductPresentation(
        title = product.name,
        description = description,
        // The catalogue description already spells the amount out for token packs, so the extra
        // token line would just repeat it.
        tokenLabel = if (description == null) tokenLabel else null,
        price = price,
        isPurchasable = price != null
    )
}
