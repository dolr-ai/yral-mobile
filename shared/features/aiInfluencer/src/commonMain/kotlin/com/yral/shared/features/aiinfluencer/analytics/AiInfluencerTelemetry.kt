package com.yral.shared.features.aiinfluencer.analytics

import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.analytics.events.BotCreationErrorStage
import com.yral.shared.analytics.events.BotCreationFailedEventData
import com.yral.shared.analytics.events.BotCreationSource
import com.yral.shared.analytics.events.BotCreationStartedEventData
import com.yral.shared.analytics.events.BotCreationSuccessEventData
import com.yral.shared.analytics.events.BotDescriptionAcceptedEventData
import com.yral.shared.analytics.events.BotDescriptionSubmittedEventData
import com.yral.shared.analytics.events.BotProfileGeneratedEventData
import com.yral.shared.analytics.events.CreateBotClickedEventData

class AiInfluencerTelemetry(
    private val analyticsManager: AnalyticsManager,
) {
    fun botCreationStarted(entryPoint: BotCreationSource) {
        analyticsManager.trackEvent(BotCreationStartedEventData(entryPoint = entryPoint))
    }

    fun botDescriptionSubmitted(
        descriptionText: String,
        entryPoint: BotCreationSource,
    ) {
        analyticsManager.trackEvent(
            BotDescriptionSubmittedEventData(
                descriptionText = descriptionText,
                entryPoint = entryPoint,
            ),
        )
    }

    fun botDescriptionAccepted(
        descriptionEdited: Boolean,
        descriptionText: String,
        entryPoint: BotCreationSource,
    ) {
        analyticsManager.trackEvent(
            BotDescriptionAcceptedEventData(
                descriptionEdited = descriptionEdited,
                descriptionText = descriptionText,
                entryPoint = entryPoint,
            ),
        )
    }

    fun botProfileGenerated(
        nameGenerated: String,
        avatarGenerated: Boolean,
        entryPoint: BotCreationSource,
    ) {
        analyticsManager.trackEvent(
            BotProfileGeneratedEventData(
                nameGenerated = nameGenerated,
                avatarGenerated = avatarGenerated,
                entryPoint = entryPoint,
            ),
        )
    }

    fun createBotClicked(
        retries: Int,
        entryPoint: BotCreationSource,
    ) {
        analyticsManager.trackEvent(CreateBotClickedEventData(retries = retries, entryPoint = entryPoint))
    }

    fun botCreationSuccess(
        botId: String,
        entryPoint: BotCreationSource,
    ) {
        analyticsManager.trackEvent(
            BotCreationSuccessEventData(
                botId = botId,
                entryPoint = entryPoint,
            ),
        )
    }

    fun botCreationFailed(
        errorCode: String,
        errorStage: BotCreationErrorStage,
        retryAvailable: Boolean,
        entryPoint: BotCreationSource,
    ) {
        analyticsManager.trackEvent(
            BotCreationFailedEventData(
                errorCode = errorCode,
                errorStage = errorStage,
                retryAvailable = retryAvailable,
                entryPoint = entryPoint,
            ),
        )
    }

    fun flush() {
        analyticsManager.flush()
    }
}
