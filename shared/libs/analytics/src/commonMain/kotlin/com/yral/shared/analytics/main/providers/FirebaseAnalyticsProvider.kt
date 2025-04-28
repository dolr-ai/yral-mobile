package com.yral.shared.analytics.main.providers

import com.yral.shared.analytics.core.AnalyticsProvider
import com.yral.shared.analytics.core.Event
import com.yral.shared.analytics.core.User
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.analytics.FirebaseAnalytics
import dev.gitlive.firebase.analytics.analytics

class FirebaseAnalyticsProvider(
    private val eventFilter: (Event) -> Boolean = { true },
) : AnalyticsProvider {
    private val firebaseAnalytics: FirebaseAnalytics by lazy {
        Firebase.analytics
    }

    override val name: String = "firebase"

    override fun shouldTrackEvent(event: Event): Boolean = eventFilter(event)

    override fun trackEvent(event: Event) {
        val parameters = event.properties.mapKeys { toValidKeyName(it.key) }
        firebaseAnalytics.logEvent(toValidKeyName(event.name), parameters)
    }

    override fun flush() {
        // Firebase Analytics automatically batches and sends events
    }

    override fun setUserProperties(user: User) {
        firebaseAnalytics.setUserId(user.userId)
        firebaseAnalytics.setUserProperty("name", user.name)
        firebaseAnalytics.setUserProperty("emailId", user.emailId)
    }

    override fun reset() {
        firebaseAnalytics.resetAnalyticsData()
        firebaseAnalytics.setUserId("")
        firebaseAnalytics.setUserProperty("name", "")
        firebaseAnalytics.setUserProperty("emailId", "")
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
