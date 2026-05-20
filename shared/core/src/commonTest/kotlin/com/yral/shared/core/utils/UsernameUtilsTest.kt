package com.yral.shared.core.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UsernameUtilsTest {
    @Test
    fun `word pools have expected sizes`() {
        assertEquals(EXPECTED_MODIFIER_COUNT, USERNAME_MODIFIERS.size)
        assertEquals(EXPECTED_MODIFIER_COUNT, USERNAME_MODIFIERS.toSet().size)
        assertEquals(EXPECTED_NOUN_COUNT, USERNAME_NOUNS.size)
        assertEquals(EXPECTED_NOUN_COUNT, USERNAME_NOUNS.toSet().size)
    }

    @Test
    fun `username generation is deterministic for the same principal`() {
        val principal = "test-principal"

        assertEquals(
            generateUsernameFromPrincipal(principal),
            generateUsernameFromPrincipal(principal),
        )
    }

    @Test
    fun `generated usernames are word only and valid length`() {
        repeat(SAMPLE_SIZE) { index ->
            val username = generateUsernameFromPrincipal("principal-$index")

            assertTrue(username.length in MIN_USERNAME_LENGTH..MAX_USERNAME_LENGTH)
            assertTrue(username.all { it.isLetter() })
        }
    }

    @Test
    fun `generated usernames are modifier modifier animal names`() {
        repeat(SAMPLE_SIZE) { index ->
            val username = generateUsernameFromPrincipal("principal-$index")
            val composition = username.findUsernameComposition()

            assertNotNull(composition, "Expected $username to match modifier modifier animal")
            assertFalse(composition.firstModifier == composition.secondModifier)
        }
    }

    @Test
    fun `unsafe words are not present in username pools`() {
        val words = USERNAME_MODIFIERS + USERNAME_NOUNS

        UNSAFE_WORDS.forEach { unsafeWord ->
            assertFalse(unsafeWord in words)
        }
    }

    private fun String.findUsernameComposition(): UsernameComposition? {
        USERNAME_MODIFIERS.forEach { firstModifier ->
            if (!startsWith(firstModifier)) return@forEach
            val afterFirstModifier = removePrefix(firstModifier)
            USERNAME_MODIFIERS.forEach { secondModifier ->
                if (secondModifier == firstModifier || !afterFirstModifier.startsWith(secondModifier)) {
                    return@forEach
                }
                val noun = afterFirstModifier.removePrefix(secondModifier)
                if (noun in USERNAME_NOUNS) {
                    return UsernameComposition(firstModifier, secondModifier, noun)
                }
            }
        }
        return null
    }

    private data class UsernameComposition(
        val firstModifier: String,
        val secondModifier: String,
        val noun: String,
    )

    private companion object {
        const val EXPECTED_MODIFIER_COUNT = 200
        const val EXPECTED_NOUN_COUNT = 150
        const val MIN_USERNAME_LENGTH = 3
        const val MAX_USERNAME_LENGTH = 15
        const val SAMPLE_SIZE = 1_000
        val UNSAFE_WORDS =
            setOf(
                "sex",
                "sexy",
                "thong",
                "pimp",
                "racist",
                "machete",
                "nude",
                "naked",
                "violent",
                "terrorist",
                "obscene",
            )
    }
}
