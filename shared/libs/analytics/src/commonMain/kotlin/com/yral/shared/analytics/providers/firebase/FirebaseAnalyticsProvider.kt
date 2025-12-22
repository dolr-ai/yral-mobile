package com.yral.shared.analytics.providers.firebase

import com.yral.shared.analytics.AnalyticsProvider
import com.yral.shared.analytics.EventToMapConverter
import com.yral.shared.analytics.User
import com.yral.shared.analytics.events.EventData
import dev.gitlive.firebase.analytics.FirebaseAnalytics

class FirebaseAnalyticsProvider(
    private val firebaseAnalytics: FirebaseAnalytics,
    private val eventFilter: (EventData) -> Boolean = { true },
    private val mapConverter: EventToMapConverter,
) : AnalyticsProvider {
    override val name: String = "firebase"

    override fun shouldTrackEvent(event: EventData): Boolean = eventFilter(event)

    override fun trackEvent(event: EventData) {
        firebaseAnalytics.logEvent(
            name = toValidKeyName(event.event),
            parameters = mapConverter.toMap(event),
        )
    }

    override fun setUserProperties(user: User) {
        firebaseAnalytics.setUserId(user.userId)
    }

    override fun reset() {
        firebaseAnalytics.resetAnalyticsData()
        firebaseAnalytics.setUserId("")
        firebaseAnalytics.setUserProperty("name", "")
        firebaseAnalytics.setUserProperty("emailId", "")
    }

    override fun applyCommonContext(common: Map<String, Any?>) {
        common.forEach { (key, value) ->
            value?.toString()?.let {
                firebaseAnalytics.setUserProperty(key, it)
            }
        }
    }

    override fun toValidKeyName(key: String): String {
        if (isEventKeyValid(key)) return key

        val keyNameBuilder = StringBuilder(key.length + EXTRA_SB_CAPACITY)
        key.forEachIndexed { index, ch ->
            if (keyNameBuilder.isNotBlank()) {
                val appendUnderscore =
                    when {
                        ch.replaceWithUnderscore() -> true
                        ch.isUpperCase() -> key[index - 1].isUpperCase().not()
                        else -> false
                    }
                if (appendUnderscore) {
                    keyNameBuilder.appendUnderscoreIfNotPresent()
                }
            }
            if (!ch.replaceWithUnderscore()) {
                if (ch.isUnderscore()) {
                    keyNameBuilder.appendUnderscoreIfNotPresent()
                } else {
                    keyNameBuilder.append(ch.lowercaseChar())
                }
            }
        }

        val lastCorrectIndex = keyNameBuilder.indexOfLast { it != '_' }
        if (lastCorrectIndex != keyNameBuilder.lastIndex) {
            keyNameBuilder.deleteRange(lastCorrectIndex + 1, keyNameBuilder.length)
        }

        check(keyNameBuilder.first().isLetter()) { "Event key must start with an alphabetic character $keyNameBuilder" }
        check(isEventKeyValid(keyNameBuilder)) { "Event key does not confirm to valid string $keyNameBuilder" }

        return keyNameBuilder.toString()
    }

    private fun isEventKeyValid(charSequence: CharSequence): Boolean = validityCheckRegex.matches(charSequence)

    @Suppress("MaxLineLength")
    private fun StringBuilder.appendUnderscoreIfNotPresent(): StringBuilder = if (last().isUnderscore().not()) append('_') else this

    private fun Char.replaceWithUnderscore(): Boolean = isWhitespace() || isSpecialChar()

    private fun Char.isSpecialChar(): Boolean = (isLetter() || isDigit() || isUnderscore()).not()

    private fun Char.isUnderscore(): Boolean = this == '_'

    companion object {
        private const val EXTRA_SB_CAPACITY = 16
        private val validityCheckRegex = Regex("^[a-z]+[a-z0-9_]*\$")
    }
}
