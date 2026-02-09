package com.yral.shared.features.aiinfluencer.data.models

import com.yral.shared.features.aiinfluencer.domain.models.GeneratedInfluencerMetadata
import com.yral.shared.features.aiinfluencer.domain.models.GeneratedPrompt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MappersTest {
    @Test
    fun `GeneratePromptResponseDto toDomain maps systemInstructions correctly`() {
        // Given
        val dto = GeneratePromptResponseDto(systemInstructions = "Test instructions")

        // When
        val result = dto.toDomain()

        // Then
        assertEquals("Test instructions", result.systemInstructions)
    }

    @Test
    fun `GeneratePromptResponseDto toDomain handles empty systemInstructions`() {
        // Given
        val dto = GeneratePromptResponseDto(systemInstructions = "")

        // When
        val result = dto.toDomain()

        // Then
        assertEquals("", result.systemInstructions)
    }

    @Test
    fun `ValidateAndGenerateMetadataResponseDto toDomain maps all fields correctly when present`() {
        // Given
        val dto =
            ValidateAndGenerateMetadataResponseDto(
                isValid = true,
                reason = "Valid profile",
                name = "testbot",
                displayName = "Test Bot",
                description = "A test bot description",
                initialGreeting = "Hello!",
                suggestedMessages = listOf("Message 1", "Message 2"),
                personalityTraits = mapOf("friendly" to "high", "professional" to "medium"),
                category = "Entertainment",
                avatarUrl = "https://example.com/avatar.jpg",
                systemInstructions = "System instructions",
                isNsfw = false,
            )

        // When
        val result = dto.toDomain()

        // Then
        assertEquals(true, result.isValid)
        assertEquals("Valid profile", result.reason)
        assertEquals("testbot", result.name)
        assertEquals("Test Bot", result.displayName)
        assertEquals("A test bot description", result.description)
        assertEquals("Hello!", result.initialGreeting)
        assertEquals(listOf("Message 1", "Message 2"), result.suggestedMessages)
        assertEquals(mapOf("friendly" to "high", "professional" to "medium"), result.personalityTraits)
        assertEquals("Entertainment", result.category)
        assertEquals("https://example.com/avatar.jpg", result.avatarUrl)
        assertEquals("System instructions", result.systemInstructions)
        assertFalse(result.isNsfw)
    }

    @Test
    fun `ValidateAndGenerateMetadataResponseDto toDomain handles null fields with defaults`() {
        // Given - only required field is isValid
        val dto =
            ValidateAndGenerateMetadataResponseDto(
                isValid = false,
                reason = null,
                name = null,
                displayName = null,
                description = null,
                initialGreeting = null,
                suggestedMessages = null,
                personalityTraits = null,
                category = null,
                avatarUrl = null,
                systemInstructions = null,
                isNsfw = true,
            )

        // When
        val result = dto.toDomain()

        // Then
        assertFalse(result.isValid)
        assertEquals("", result.reason)
        assertEquals("", result.name)
        assertEquals("", result.displayName)
        assertEquals("", result.description)
        assertEquals("", result.initialGreeting)
        assertEquals(emptyList(), result.suggestedMessages)
        assertEquals(emptyMap(), result.personalityTraits)
        assertEquals("", result.category)
        assertEquals("", result.avatarUrl)
        assertEquals(null, result.systemInstructions)
        assertTrue(result.isNsfw)
    }

    @Test
    fun `ValidateAndGenerateMetadataResponseDto toDomain handles empty strings`() {
        // Given
        val dto =
            ValidateAndGenerateMetadataResponseDto(
                isValid = true,
                reason = "",
                name = "",
                displayName = "",
                description = "",
                initialGreeting = "",
                suggestedMessages = emptyList(),
                personalityTraits = emptyMap(),
                category = "",
                avatarUrl = "",
                systemInstructions = "",
                isNsfw = false,
            )

        // When
        val result = dto.toDomain()

        // Then
        assertTrue(result.isValid)
        assertEquals("", result.reason)
        assertEquals("", result.name)
        assertEquals("", result.displayName)
        assertEquals("", result.description)
        assertEquals("", result.initialGreeting)
        assertEquals(emptyList(), result.suggestedMessages)
        assertEquals(emptyMap(), result.personalityTraits)
        assertEquals("", result.category)
        assertEquals("", result.avatarUrl)
        assertEquals("", result.systemInstructions)
        assertFalse(result.isNsfw)
    }

    @Test
    fun `ValidateAndGenerateMetadataResponseDto toDomain handles partial null fields`() {
        // Given - mix of null and non-null fields
        val dto =
            ValidateAndGenerateMetadataResponseDto(
                isValid = true,
                reason = "Some reason",
                name = "validname",
                displayName = null,
                description = "Valid description",
                initialGreeting = null,
                suggestedMessages = listOf("Hi", "Hello"),
                personalityTraits = null,
                category = "Tech",
                avatarUrl = null,
                systemInstructions = "Instructions",
                isNsfw = false,
            )

        // When
        val result = dto.toDomain()

        // Then
        assertTrue(result.isValid)
        assertEquals("Some reason", result.reason)
        assertEquals("validname", result.name)
        assertEquals("", result.displayName) // null becomes empty
        assertEquals("Valid description", result.description)
        assertEquals("", result.initialGreeting) // null becomes empty
        assertEquals(listOf("Hi", "Hello"), result.suggestedMessages)
        assertEquals(emptyMap(), result.personalityTraits) // null becomes empty map
        assertEquals("Tech", result.category)
        assertEquals("", result.avatarUrl) // null becomes empty
        assertEquals("Instructions", result.systemInstructions)
        assertFalse(result.isNsfw)
    }

    @Test
    fun `ValidateAndGenerateMetadataResponseDto toDomain handles complex personality traits`() {
        // Given
        val traits =
            mapOf(
                "friendly" to "high",
                "professional" to "medium",
                "humorous" to "low",
                "technical" to "very high",
            )
        val dto =
            ValidateAndGenerateMetadataResponseDto(
                isValid = true,
                name = "bot",
                displayName = "Bot",
                description = "Desc",
                initialGreeting = "Hi",
                suggestedMessages = listOf("A"),
                personalityTraits = traits,
                category = "Cat",
                avatarUrl = "url",
            )

        // When
        val result = dto.toDomain()

        // Then
        assertEquals(traits, result.personalityTraits)
        assertEquals(4, result.personalityTraits.size)
    }

    @Test
    fun `ValidateAndGenerateMetadataResponseDto toDomain handles many suggested messages`() {
        // Given
        val messages = (1..10).map { "Message $it" }
        val dto =
            ValidateAndGenerateMetadataResponseDto(
                isValid = true,
                name = "bot",
                displayName = "Bot",
                description = "Desc",
                initialGreeting = "Hi",
                suggestedMessages = messages,
                personalityTraits = emptyMap(),
                category = "Cat",
                avatarUrl = "url",
            )

        // When
        val result = dto.toDomain()

        // Then
        assertEquals(messages, result.suggestedMessages)
        assertEquals(10, result.suggestedMessages.size)
    }
}
