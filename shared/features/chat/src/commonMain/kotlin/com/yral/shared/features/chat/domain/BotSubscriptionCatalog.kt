package com.yral.shared.features.chat.domain

import com.yral.shared.iap.core.model.ProductId

/**
 * Client-side source of truth for which bots use the per-bot auto-renewing
 * subscription flow instead of the one-time [ProductId.DAILY_CHAT] product.
 * The billing backend has no per-bot catalog endpoint — it only verifies
 * whatever product the client sends (routing on the "bot_sub" id prefix) —
 * so the bot→product mapping lives here.
 *
 * Subscription bots are also the only ones with the Request Image (photo
 * collage) feature.
 */
object BotSubscriptionCatalog {
    // Tara. Observed on the agent.rishi.yral.com chat backend — confirm the
    // prod environment serves the same principal before a prod release.
    private const val TARA_BOT_ID = "qi6gd-esmrx-v2oyd-7fwhm-ibfs5-trflm-xm3iy-xq6d3-3hmwu-jb7tk-5qe"

    fun usesBotSubscription(botId: String?): Boolean = botId == TARA_BOT_ID

    fun chatProductFor(botId: String?): ProductId {
        val product = if (usesBotSubscription(botId)) ProductId.BOT_SUB_TARA else ProductId.DAILY_CHAT
        return product
    }
}
