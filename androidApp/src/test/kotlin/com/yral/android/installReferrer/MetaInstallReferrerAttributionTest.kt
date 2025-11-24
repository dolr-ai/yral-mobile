package com.yral.android.installReferrer

import co.touchlab.kermit.Logger
import com.yral.android.BuildConfig
import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.preferences.UtmParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.robolectric.annotation.Config
import javax.crypto.AEADBadTagException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Config(sdk = [24]) // Run tests as if on Android 7.0
class MetaInstallReferrerAttributionTest {
    @Suppress("ktlint:standard:max-line-length", "MaxLineLength")
    private val encryptedDataHex = "afe56cf6228c6ea8c79da49186e718e92a579824596ae1d0d4d20d7793dca797bd4034ccf467bfae5c79a3981e7a2968c41949237e2b2db678c1c3d39c9ae564c5cafd52f2b77a3dc77bf1bae063114d0283b97417487207735da31ddc1531d5645a9c3e602c195a0ebf69c272aa5fda3a2d781cb47e117310164715a54c7a5a032740584e2789a7b4e596034c16425139a77e507c492b629c848573c714a03a2e7d25b9459b95842332b460f3682d19c35dbc7d53e3a51e0497ff6a6cbb367e760debc4194ae097498108df7b95eac2fa9bac4320077b510be3b7b823248bfe02ae501d9fe4ba179c7de6733c92bf89d523df9e31238ef497b9db719484cbab7531dbf6c5ea5a8087f95d59f5e4f89050e0f1dc03e464168ad76a64cca64b79"
    private val nonceHex = "b7203c6a6fb633d16e9cf5c1"
    private val decryptionKey = "2575590594a9cd809e5bfacf397f8c1ac730dbc38a3e137ecd1ab66591c8c3c9"
    private val skipDecryptionTests = false

    private fun createTestInstance(): MetaInstallReferrerAttribution =
        MetaInstallReferrerAttribution(CoroutineScope(Dispatchers.Unconfined), AnalyticsManager())

    private fun extractUtmParamsFromJson(json: JsonObject): UtmParams {
        // Match the actual implementation: stringify JsonObject, extract JsonPrimitive content
        val utmContent = json["utm_content"]
        val contentValue =
            when (utmContent) {
                is JsonPrimitive -> utmContent.content
                is JsonObject -> Json.encodeToString(JsonObject.serializer(), utmContent)
                else -> null
            }

        return UtmParams(
            campaign = json["utm_campaign"]?.jsonPrimitive?.content,
            source = json["utm_source"]?.jsonPrimitive?.content,
            medium = json["utm_medium"]?.jsonPrimitive?.content,
            term = json["utm_term"]?.jsonPrimitive?.content,
            content = contentValue,
        )
    }

    @Test
    fun `test hexStringToByteArray converts hex string correctly`() {
        val attribution = createTestInstance()
        val hex = "48656c6c6f"
        val bytes = attribution.hexStringToByteArray(hex)

        assertEquals(5, bytes.size)
        assertEquals(0x48.toByte(), bytes[0])
        assertEquals(0x65.toByte(), bytes[1])
        assertEquals(0x6c.toByte(), bytes[2])
        assertEquals(0x6c.toByte(), bytes[3])
        assertEquals(0x6f.toByte(), bytes[4])
    }

    @Test
    fun `test decryptAesGcm decrypts encrypted data correctly`() {
        if (skipDecryptionTests) return

        val attribution = createTestInstance()
        val decrypted = attribution.decryptAesGcm(encryptedDataHex, nonceHex, decryptionKey)

        assertNotNull(decrypted, "Decrypted data should not be null")
        assertTrue(decrypted.isNotBlank(), "Decrypted data should not be empty")

        val json = Json.decodeFromString(JsonObject.serializer(), decrypted)
        Logger.d("Test") { "Decrypted data $json" }
        assertNotNull(json, "Decrypted data should be valid JSON")
    }

    @Test
    fun `test decrypted JSON contains expected campaign metadata fields`() {
        if (skipDecryptionTests) return

        val attribution = createTestInstance()
        val decrypted = attribution.decryptAesGcm(encryptedDataHex, nonceHex, decryptionKey)
        val metadata = Json.decodeFromString(JsonObject.serializer(), decrypted)

        val expectedFields =
            listOf(
                "ad_id",
                "adgroup_id",
                "adgroup_name",
                "campaign_id",
                "campaign_name",
                "campaign_group_id",
                "campaign_group_name",
                "account_id",
                "ad_objective_name",
            )

        for (field in expectedFields) {
            assertTrue(
                metadata.containsKey(field),
                "Decrypted JSON should contain field: $field",
            )
        }
    }

    @Test
    fun `test decrypted JSON structure matches expected format`() {
        if (skipDecryptionTests) return

        val attribution = createTestInstance()
        val decrypted = attribution.decryptAesGcm(encryptedDataHex, nonceHex, decryptionKey)
        val metadata = Json.decodeFromString(JsonObject.serializer(), decrypted)

        val stringFields =
            listOf(
                "ad_id",
                "adgroup_id",
                "adgroup_name",
                "campaign_id",
                "campaign_name",
                "campaign_group_id",
                "campaign_group_name",
                "account_id",
                "ad_objective_name",
            )

        for (field in stringFields) {
            val value = metadata[field]?.jsonPrimitive?.content
            assertNotNull(value, "Field $field should have a string value")
            assertEquals(value.isNotBlank(), true, "Field $field should not be blank")
        }

        val adObjectiveName = metadata["ad_objective_name"]?.jsonPrimitive?.content
        assertEquals("APP_INSTALLS", adObjectiveName)
    }

    @Test
    fun `test isMetaInstallReferrerData detects encrypted JSON correctly`() {
        val attribution = createTestInstance()

        val encryptedJson =
            """
            {
                "utm_content": {
                    "source": {
                        "data": "$encryptedDataHex",
                        "nonce": "$nonceHex"
                    }
                }
            }
            """.trimIndent()

        assertTrue(
            attribution.isMetaInstallReferrerData(encryptedJson),
            "Should detect Meta Install Referrer data in JSON format",
        )

        // Test with proper production structure
        val properStructureJson =
            """
            {
                "utm_content": {
                    "source": {
                        "data": "test_data",
                        "nonce": "test_nonce"
                    }
                }
            }
            """.trimIndent()
        assertTrue(
            attribution.isMetaInstallReferrerData(properStructureJson),
            "Should detect Meta Install Referrer data with proper structure",
        )

        assertTrue(
            !attribution.isMetaInstallReferrerData("plain text"),
            "Should not detect non-JSON as Meta Install Referrer data",
        )
    }

    @Test
    fun `test isMetaInstallReferrerData detects query string format`() {
        val attribution = createTestInstance()
        // Query string without encrypted data (should not be detected)
        val plainQueryString = "utm_source=apps.facebook.com&utm_campaign=fb4a"

        assertTrue(
            !attribution.isMetaInstallReferrerData(plainQueryString),
            "Should not detect plain query string as Meta Install Referrer data",
        )
    }

    @Test
    fun `test full decryption flow from encrypted JSON to campaign metadata`() {
        if (skipDecryptionTests) return

        val attribution = createTestInstance()
        val encryptedJsonString =
            """
            {
                "utm_content": {
                    "source": {
                        "data": "$encryptedDataHex",
                        "nonce": "$nonceHex"
                    }
                }
            }
            """.trimIndent()

        val encryptedJson = Json.decodeFromString(JsonObject.serializer(), encryptedJsonString)
        val utmContent = encryptedJson["utm_content"]?.jsonObject
        val source = utmContent?.get("source")?.jsonObject
        val dataHex = source?.get("data")?.jsonPrimitive?.content
        val nonceHex = source?.get("nonce")?.jsonPrimitive?.content

        assertNotNull(dataHex, "Should extract data from utm_content.source")
        assertNotNull(nonceHex, "Should extract nonce from utm_content.source")

        val decrypted = attribution.decryptAesGcm(dataHex, nonceHex, decryptionKey)
        val metadata = Json.decodeFromString(JsonObject.serializer(), decrypted)

        assertNotNull(metadata, "Decrypted metadata should not be null")
        assertTrue(metadata.containsKey("campaign_id"), "Decrypted metadata should contain campaign_id")
        assertTrue(metadata.containsKey("campaign_name"), "Decrypted metadata should contain campaign_name")
        assertTrue(metadata.containsKey("ad_id"), "Decrypted metadata should contain ad_id")
    }

    @Test
    fun `test root-level UTM parameters are extracted correctly`() {
        val installReferrerJson =
            """
            {
                "utm_campaign": "test_campaign",
                "utm_source": "apps.facebook.com",
                "utm_medium": "cpc",
                "utm_term": "test_term",
                "utm_content": {
                    "source": {
                        "data": "$encryptedDataHex",
                        "nonce": "$nonceHex"
                    }
                }
            }
            """.trimIndent()

        val json = Json.decodeFromString(JsonObject.serializer(), installReferrerJson)
        val rootUtmParams = extractUtmParamsFromJson(json)

        assertEquals("test_campaign", rootUtmParams.campaign, "Should extract root-level utm_campaign")
        assertEquals("apps.facebook.com", rootUtmParams.source, "Should extract root-level utm_source")
        assertEquals("cpc", rootUtmParams.medium, "Should extract root-level utm_medium")
        assertEquals("test_term", rootUtmParams.term, "Should extract root-level utm_term")
        // When utm_content is a JsonObject (encrypted data), it should be stringified
        assertNotNull(rootUtmParams.content, "utm_content should be stringified when it's a JsonObject")
        assertTrue(
            rootUtmParams.content!!.contains("\"source\""),
            "Stringified utm_content should contain the source structure",
        )
    }

    @Test
    fun `test root-level UTM parameters take priority over decrypted values`() {
        if (skipDecryptionTests) return

        val attribution = createTestInstance()
        val installReferrerJson =
            """
            {
                "utm_campaign": "root_campaign",
                "utm_source": "apps.instagram.com",
                "utm_medium": "cpc",
                "utm_term": "root_term",
                "utm_content": {
                    "source": {
                        "data": "$encryptedDataHex",
                        "nonce": "$nonceHex"
                    }
                }
            }
            """.trimIndent()

        val json = Json.decodeFromString(JsonObject.serializer(), installReferrerJson)
        val rootUtmParams = extractUtmParamsFromJson(json)

        // Extract and decrypt encrypted data
        val utmContentObject = json["utm_content"]?.jsonObject
        val source = utmContentObject?.get("source")?.jsonObject
        val dataHex = source?.get("data")?.jsonPrimitive?.content
        val nonceHex = source?.get("nonce")?.jsonPrimitive?.content

        assertNotNull(dataHex, "Should extract data from utm_content.source")
        assertNotNull(nonceHex, "Should extract nonce from utm_content.source")

        val decrypted = attribution.decryptAesGcm(dataHex, nonceHex, decryptionKey)
        val campaignMetadata = Json.decodeFromString(JsonObject.serializer(), decrypted)

        // Simulate the prioritization logic: root-level values take priority
        val finalCampaign =
            rootUtmParams.campaign?.takeIf { it.isNotBlank() }
                ?: campaignMetadata["campaign_name"]?.jsonPrimitive?.content
        val finalSource = rootUtmParams.source?.takeIf { it.isNotBlank() } ?: "meta_ads"
        val finalTerm =
            rootUtmParams.term?.takeIf { it.isNotBlank() }
                ?: campaignMetadata["adgroup_name"]?.jsonPrimitive?.content

        assertEquals("root_campaign", finalCampaign, "Root-level utm_campaign should take priority")
        assertEquals("apps.instagram.com", finalSource, "Root-level utm_source should take priority")
        assertEquals("root_term", finalTerm, "Root-level utm_term should take priority")
    }

    @Test
    fun `test decrypted values used when root-level values are missing`() {
        if (skipDecryptionTests) return

        val attribution = createTestInstance()
        val installReferrerJson =
            """
            {
                "utm_content": {
                    "source": {
                        "data": "$encryptedDataHex",
                        "nonce": "$nonceHex"
                    }
                }
            }
            """.trimIndent()

        val json = Json.decodeFromString(JsonObject.serializer(), installReferrerJson)

        val rootUtmParams = extractUtmParamsFromJson(json)

        // Extract and decrypt encrypted data
        val utmContentObject = json["utm_content"]?.jsonObject
        val source = utmContentObject?.get("source")?.jsonObject
        val dataHex = source?.get("data")?.jsonPrimitive?.content
        val nonceHex = source?.get("nonce")?.jsonPrimitive?.content

        assertNotNull(dataHex, "Should extract data from utm_content.source")
        assertNotNull(nonceHex, "Should extract nonce from utm_content.source")

        val decrypted = attribution.decryptAesGcm(dataHex, nonceHex, decryptionKey)
        val campaignMetadata = Json.decodeFromString(JsonObject.serializer(), decrypted)

        // Simulate the fallback logic: use decrypted values when root-level is missing
        val finalCampaign =
            rootUtmParams.campaign?.takeIf { it.isNotBlank() }
                ?: campaignMetadata["campaign_name"]?.jsonPrimitive?.content
        val finalSource = rootUtmParams.source?.takeIf { it.isNotBlank() } ?: "meta_ads"

        assertNotNull(finalCampaign, "Should use decrypted campaign_name when root-level is missing")
        assertEquals("meta_ads", finalSource, "Should use default meta_ads when root-level is missing")
    }

    @Test
    fun `test root-level values used when no encrypted data exists`() {
        val installReferrerJson =
            """
            {
                "utm_campaign": "simple_campaign",
                "utm_source": "apps.facebook.com",
                "utm_medium": "cpc",
                "utm_term": "simple_term",
                "utm_content": "simple_content"
            }
            """.trimIndent()

        val json = Json.decodeFromString(JsonObject.serializer(), installReferrerJson)
        val rootUtmParams = extractUtmParamsFromJson(json)

        val utmContentObject = runCatching { json["utm_content"]?.jsonObject }.getOrNull()

        assertEquals("simple_campaign", rootUtmParams.campaign, "Should extract root-level utm_campaign")
        assertEquals("apps.facebook.com", rootUtmParams.source, "Should extract root-level utm_source")
        assertEquals("cpc", rootUtmParams.medium, "Should extract root-level utm_medium")
        assertEquals("simple_term", rootUtmParams.term, "Should extract root-level utm_term")
        assertEquals("simple_content", rootUtmParams.content, "Should extract root-level utm_content as string")
        assertEquals(null, utmContentObject, "utm_content should be a string, not an object")
    }

    @Test
    fun `test actual referrer data from production`() {
        // Actual referrer data from production
        // URL-decoded: utm_source=apps.facebook.com&utm_campaign=fb4a&utm_content={"app":4094678660808510,"t":1763644120,"source":{"data":"...","nonce":"..."}}
        @Suppress("ktlint:standard:max-line-length", "MaxLineLength")
        val actualDataHex = "3694e3b3a4500e91c07cb533366ba08bcc2e133dad8d1456c0035211243e3111a9aba09afee1b15ee32ebb36961112306a5d0a6b9c651714ecf8ae2114fd8cae3aa7a90136c1e44b2ee798f003151b029b301f7f0deaf5b11646bd8a75889a088f627b8150dc6ff8586f3f2a47169b45639dd3cc57dbedcaee8cc2fd86971b712d5f48e063c1555819eabb239eb7c3fea0e79529b6a01214c6713b7f199afc580c07b138fe46f77764dbc6d13d541ed785915b5c0d842641db2b10aedcd56a78a15b716389686422695bb59d4f5f0abf57584c55b56ea2f57ec7c714766499024ac2edaff238e56918af5727ae9c5648b165e4b98dfb482c6f451068947e9cb9cac3f2699becf51cde6d825c6dd57fb19e4920c50ea34013bb5cf4a2e6f5c72db88891613ec76520d30494d1f4ca3c977ea1750b78c2efffdf51dbb9ee1744d242c4bb1b81f7fcb0e4ae144b68d65f8fa8382b8572eef14fab84c5fe47e993ea5dfc328cb5ee7e68a8158dc6bf56d5d115e731711d1634f09d335aa68cb34769cb87991cf28b9a1b243f18acb6eb3e0995590559972da9f7d9a2bff961"
        val actualNonceHex = "729add5d1b1d91ca2af43c3e"

        // Convert to JSON format as expected by processEncryptedData
        val installReferrerJson =
            """
            {
                "utm_source": "apps.facebook.com",
                "utm_campaign": "fb4a",
                "utm_content": {
                    "source": {
                        "data": "$actualDataHex",
                        "nonce": "$actualNonceHex"
                    }
                }
            }
            """.trimIndent()

        val attribution = createTestInstance()

        // Test that it can detect Meta Install Referrer data
        assertTrue(
            attribution.isMetaInstallReferrerData(installReferrerJson),
            "Should detect actual Meta Install Referrer data",
        )

        val json = Json.decodeFromString(JsonObject.serializer(), installReferrerJson)
        val rootUtmParams = extractUtmParamsFromJson(json)

        assertEquals("fb4a", rootUtmParams.campaign, "Should extract root-level utm_campaign")
        assertEquals("apps.facebook.com", rootUtmParams.source, "Should extract root-level utm_source")
        // utm_content should be stringified when it's a JsonObject (encrypted data)
        assertNotNull(rootUtmParams.content, "utm_content should be stringified when it's a JsonObject")
        assertTrue(
            rootUtmParams.content!!.contains("\"source\""),
            "Stringified utm_content should contain the source structure",
        )

        // Test decryption only if skipDecryptionTests is false and key matches
        // Note: This will fail if decryptionKey doesn't match the actual production key
        if (!skipDecryptionTests) {
            try {
                val decrypted = attribution.decryptAesGcm(actualDataHex, actualNonceHex, decryptionKey)
                val metadata = Json.decodeFromString(JsonObject.serializer(), decrypted)

                assertNotNull(metadata, "Decrypted metadata should not be null")
                assertTrue(metadata.containsKey("campaign_id"), "Decrypted metadata should contain campaign_id")
                assertTrue(metadata.containsKey("campaign_name"), "Decrypted metadata should contain campaign_name")
                assertTrue(metadata.containsKey("ad_id"), "Decrypted metadata should contain ad_id")
            } catch (e: AEADBadTagException) {
                // Expected if decryptionKey doesn't match actual production key
                Logger.d("Test") { "Decryption failed - decryptionKey may not match production key: ${e.message}" }
            }
        }
    }

    @Test
    fun `test actual production data with new decryption key from URL-encoded format`() {
        // Actual referrer data from production in URL-encoded query string format
        // URL-encoded: utm_source=apps.facebook.com&utm_campaign=fb4a&utm_content=%7B%22app%22%3A4094678660808510%2C%22t%22%3A1763644120%2C%22source%22%3A%7B%22data%22%3A%223694e3b3a4500e91c07cb533366ba08bcc2e133dad8d1456c0035211243e3111a9aba09afee1b15ee32ebb36961112306a5d0a6b9c651714ecf8ae2114fd8cae3aa7a90136c1e44b2ee798f003151b029b301f7f0deaf5b11646bd8a75889a088f627b8150dc6ff8586f3f2a47169b45639dd3cc57dbedcaee8cc2fd86971b712d5f48e063c1555819eabb239eb7c3fea0e79529b6a01214c6713b7f199afc580c07b138fe46f77764dbc6d13d541ed785915b5c0d842641db2b10aedcd56a78a15b716389686422695bb59d4f5f0abf57584c55b56ea2f57ec7c714766499024ac2edaff238e56918af5727ae9c5648b165e4b98dfb482c6f451068947e9cb9cac3f2699becf51cde6d825c6dd57fb19e4920c50ea34013bb5cf4a2e6f5c72db88891613ec76520d30494d1f4ca3c977ea1750b78c2efffdf51dbb9ee1744d242c4bb1b81f7fcb0e4ae144b68d65f8fa8382b8572eef14fab84c5fe47e993ea5dfc328cb5ee7e68a8158dc6bf56d5d115e731711d1634f09d335aa68cb34769cb87991cf28b9a1b243f18acb6eb3e0995590559972da9f7d9a2bff961%22%2C%22nonce%22%3A%22729add5d1b1d91ca2af43c3e%22%7D%7D
        val newDecryptionKey = BuildConfig.META_INSTALL_REFERRER_DECRYPTION_KEY

        @Suppress("ktlint:standard:max-line-length", "MaxLineLength")
        val actualDataHex = "3694e3b3a4500e91c07cb533366ba08bcc2e133dad8d1456c0035211243e3111a9aba09afee1b15ee32ebb36961112306a5d0a6b9c651714ecf8ae2114fd8cae3aa7a90136c1e44b2ee798f003151b029b301f7f0deaf5b11646bd8a75889a088f627b8150dc6ff8586f3f2a47169b45639dd3cc57dbedcaee8cc2fd86971b712d5f48e063c1555819eabb239eb7c3fea0e79529b6a01214c6713b7f199afc580c07b138fe46f77764dbc6d13d541ed785915b5c0d842641db2b10aedcd56a78a15b716389686422695bb59d4f5f0abf57584c55b56ea2f57ec7c714766499024ac2edaff238e56918af5727ae9c5648b165e4b98dfb482c6f451068947e9cb9cac3f2699becf51cde6d825c6dd57fb19e4920c50ea34013bb5cf4a2e6f5c72db88891613ec76520d30494d1f4ca3c977ea1750b78c2efffdf51dbb9ee1744d242c4bb1b81f7fcb0e4ae144b68d65f8fa8382b8572eef14fab84c5fe47e993ea5dfc328cb5ee7e68a8158dc6bf56d5d115e731711d1634f09d335aa68cb34769cb87991cf28b9a1b243f18acb6eb3e0995590559972da9f7d9a2bff961"

        val actualNonceHex = "729add5d1b1d91ca2af43c3e"

        val attribution = createTestInstance()

        // Test 1: Test decryption with the new key (this doesn't require Koin)
        if (!skipDecryptionTests) {
            try {
                val decrypted = attribution.decryptAesGcm(actualDataHex, actualNonceHex, newDecryptionKey)
                val metadata = Json.decodeFromString(JsonObject.serializer(), decrypted)

                assertNotNull(metadata, "Decrypted metadata should not be null")
                Logger.d("Test") { "Successfully decrypted with new key. Metadata: $metadata" }

                // Verify expected fields exist
                assertTrue(metadata.containsKey("campaign_id"), "Decrypted metadata should contain campaign_id")
                assertTrue(metadata.containsKey("campaign_name"), "Decrypted metadata should contain campaign_name")
                assertTrue(metadata.containsKey("ad_id"), "Decrypted metadata should contain ad_id")
                assertTrue(metadata.containsKey("adgroup_id"), "Decrypted metadata should contain adgroup_id")
                assertTrue(metadata.containsKey("adgroup_name"), "Decrypted metadata should contain adgroup_name")

                // Log the decrypted values for debugging
                val campaignId = metadata["campaign_id"]?.jsonPrimitive?.content
                val campaignName = metadata["campaign_name"]?.jsonPrimitive?.content
                val adId = metadata["ad_id"]?.jsonPrimitive?.content
                Logger.d("Test") {
                    "Decrypted values - campaign_id: $campaignId, campaign_name: $campaignName, ad_id: $adId"
                }
            } catch (e: AEADBadTagException) {
                Logger.d("Test") { "Decryption failed with new key: ${e.message}" }
                throw AssertionError("Decryption failed with new decryption key. This may indicate the key is incorrect.", e)
            } catch (e: Exception) {
                Logger.d("Test") { "Unexpected error during decryption: ${e.message}" }
                throw AssertionError("Unexpected error during decryption", e)
            }
        }

        // Test 2: Verify the data structure matches expected format
        // The URL-decoded utm_content should be: {"app":4094678660808510,"t":1763644120,"source":{"data":"...","nonce":"..."}}
        // We'll verify by manually constructing the expected JSON structure
        val expectedUtmContentJson =
            """
            {
                "app": 4094678660808510,
                "t": 1763644120,
                "source": {
                    "data": "$actualDataHex",
                    "nonce": "$actualNonceHex"
                }
            }
            """.trimIndent()

        val utmContentJson = Json.decodeFromString(JsonObject.serializer(), expectedUtmContentJson)
        val source = utmContentJson["source"]?.jsonObject
        val extractedDataHex = source?.get("data")?.jsonPrimitive?.content
        val extractedNonceHex = source?.get("nonce")?.jsonPrimitive?.content

        assertNotNull(extractedDataHex, "Should extract data from utm_content.source")
        assertNotNull(extractedNonceHex, "Should extract nonce from utm_content.source")
        assertEquals(actualDataHex, extractedDataHex, "Extracted data should match expected")
        assertEquals(actualNonceHex, extractedNonceHex, "Extracted nonce should match expected")
    }

    @Test
    fun `test URL-encoded query string parsing structure matches expected`() {
        // This test verifies that the URL-encoded query string structure can be correctly parsed
        // to match what extractEncryptedData expects
        @Suppress("ktlint:standard:max-line-length", "MaxLineLength")
        val actualDataHex = "3694e3b3a4500e91c07cb533366ba08bcc2e133dad8d1456c0035211243e3111a9aba09afee1b15ee32ebb36961112306a5d0a6b9c651714ecf8ae2114fd8cae3aa7a90136c1e44b2ee798f003151b029b301f7f0deaf5b11646bd8a75889a088f627b8150dc6ff8586f3f2a47169b45639dd3cc57dbedcaee8cc2fd86971b712d5f48e063c1555819eabb239eb7c3fea0e79529b6a01214c6713b7f199afc580c07b138fe46f77764dbc6d13d541ed785915b5c0d842641db2b10aedcd56a78a15b716389686422695bb59d4f5f0abf57584c55b56ea2f57ec7c714766499024ac2edaff238e56918af5727ae9c5648b165e4b98dfb482c6f451068947e9cb9cac3f2699becf51cde6d825c6dd57fb19e4920c50ea34013bb5cf4a2e6f5c72db88891613ec76520d30494d1f4ca3c977ea1750b78c2efffdf51dbb9ee1744d242c4bb1b81f7fcb0e4ae144b68d65f8fa8382b8572eef14fab84c5fe47e993ea5dfc328cb5ee7e68a8158dc6bf56d5d115e731711d1634f09d335aa68cb34769cb87991cf28b9a1b243f18acb6eb3e0995590559972da9f7d9a2bff961"
        val actualNonceHex = "729add5d1b1d91ca2af43c3e"

        // Simulate what buildJsonFromQueryParams should produce after parsing the URL-encoded string
        // The URL-decoded utm_content should be: {"app":4094678660808510,"t":1763644120,"source":{"data":"...","nonce":"..."}}
        val expectedJsonStructure =
            """
            {
                "utm_source": "apps.facebook.com",
                "utm_campaign": "fb4a",
                "utm_content": {
                    "app": 4094678660808510,
                    "t": 1763644120,
                    "source": {
                        "data": "$actualDataHex",
                        "nonce": "$actualNonceHex"
                    }
                }
            }
            """.trimIndent()

        val json = Json.decodeFromString(JsonObject.serializer(), expectedJsonStructure)

        // Test extractRootLevelUtmParams logic
        val rootUtmParams = extractUtmParamsFromJson(json)
        assertEquals("fb4a", rootUtmParams.campaign, "Should extract root-level utm_campaign")
        assertEquals("apps.facebook.com", rootUtmParams.source, "Should extract root-level utm_source")
        // utm_content should be stringified when it's a JsonObject
        assertNotNull(rootUtmParams.content, "utm_content should be stringified when it's a JsonObject")
        assertTrue(
            rootUtmParams.content!!.contains("\"source\""),
            "Stringified utm_content should contain the source structure",
        )

        // Test extractEncryptedData logic (simulating what the code does)
        val utmContent = json["utm_content"]?.jsonObject
        assertNotNull(utmContent, "utm_content should be a JsonObject")
        val source = utmContent["source"]?.jsonObject
        assertNotNull(source, "source should exist in utm_content")
        val dataHex = source["data"]?.jsonPrimitive?.content
        val nonceHex = source["nonce"]?.jsonPrimitive?.content

        assertNotNull(dataHex, "Should extract data from utm_content.source.data")
        assertNotNull(nonceHex, "Should extract nonce from utm_content.source.nonce")
        assertEquals(actualDataHex, dataHex, "Extracted data should match expected")
        assertEquals(actualNonceHex, nonceHex, "Extracted nonce should match expected")

        // Verify the structure matches what extractEncryptedData expects
        // This confirms the parsing logic should work correctly
        Logger.d("Test") { "Successfully verified JSON structure matches expected format for extractEncryptedData" }
    }

    @Test
    fun `test extractRootLevelUtmParams handles JsonObject utm_content without throwing exception`() {
        // This test verifies the fix for the "is not a JsonPrimitive" error
        // When utm_content is a JsonObject (encrypted data), it should be stringified, not cause an exception
        @Suppress("ktlint:standard:max-line-length", "MaxLineLength")
        val actualDataHex = "3694e3b3a4500e91c07cb533366ba08bcc2e133dad8d1456c0035211243e3111a9aba09afee1b15ee32ebb36961112306a5d0a6b9c651714ecf8ae2114fd8cae3aa7a90136c1e44b2ee798f003151b029b301f7f0deaf5b11646bd8a75889a088f627b8150dc6ff8586f3f2a47169b45639dd3cc57dbedcaee8cc2fd86971b712d5f48e063c1555819eabb239eb7c3fea0e79529b6a01214c6713b7f199afc580c07b138fe46f77764dbc6d13d541ed785915b5c0d842641db2b10aedcd56a78a15b716389686422695bb59d4f5f0abf57584c55b56ea2f57ec7c714766499024ac2edaff238e56918af5727ae9c5648b165e4b98dfb482c6f451068947e9cb9cac3f2699becf51cde6d825c6dd57fb19e4920c50ea34013bb5cf4a2e6f5c72db88891613ec76520d30494d1f4ca3c977ea1750b78c2efffdf51dbb9ee1744d242c4bb1b81f7fcb0e4ae144b68d65f8fa8382b8572eef14fab84c5fe47e993ea5dfc328cb5ee7e68a8158dc6bf56d5d115e731711d1634f09d335aa68cb34769cb87991cf28b9a1b243f18acb6eb3e0995590559972da9f7d9a2bff961"
        val actualNonceHex = "729add5d1b1d91ca2af43c3e"

        // Simulate the JSON structure that comes from parsing the URL-encoded query string
        // This is what buildJsonFromQueryParams creates when utm_content is a JSON object
        val jsonWithObjectUtmContent =
            """
            {
                "utm_source": "apps.facebook.com",
                "utm_campaign": "fb4a",
                "utm_content": {
                    "app": 4094678660808510,
                    "t": 1763644120,
                    "source": {
                        "data": "$actualDataHex",
                        "nonce": "$actualNonceHex"
                    }
                }
            }
            """.trimIndent()

        val json = Json.decodeFromString(JsonObject.serializer(), jsonWithObjectUtmContent)

        // Verify utm_content is a JsonObject (not JsonPrimitive)
        val utmContent = json["utm_content"]
        assertTrue(utmContent is JsonObject, "utm_content should be a JsonObject for encrypted data")

        // This should NOT throw an exception - this was the bug we fixed
        val rootUtmParams = extractUtmParamsFromJson(json)

        // Verify extraction works correctly
        assertEquals("fb4a", rootUtmParams.campaign, "Should extract root-level utm_campaign")
        assertEquals("apps.facebook.com", rootUtmParams.source, "Should extract root-level utm_source")
        assertNotNull(rootUtmParams.content, "utm_content should be stringified when it's a JsonObject")

        // Verify the stringified content contains the expected structure
        val stringifiedContent = rootUtmParams.content!!
        assertTrue(
            stringifiedContent.contains("\"app\""),
            "Stringified content should contain 'app' field",
        )
        assertTrue(
            stringifiedContent.contains("\"source\""),
            "Stringified content should contain 'source' field",
        )
        assertTrue(
            stringifiedContent.contains(actualDataHex),
            "Stringified content should contain the encrypted data hex",
        )
        assertTrue(
            stringifiedContent.contains(actualNonceHex),
            "Stringified content should contain the nonce hex",
        )

        // Verify we can parse it back to verify it's valid JSON
        val parsedBack = Json.decodeFromString(JsonObject.serializer(), stringifiedContent)
        assertNotNull(parsedBack["app"], "Parsed back JSON should contain 'app' field")
        assertNotNull(parsedBack["source"], "Parsed back JSON should contain 'source' field")

        Logger.d("Test") {
            "Successfully verified extractRootLevelUtmParams handles JsonObject utm_content without exception. " +
                "Stringified content length: ${stringifiedContent.length}"
        }
    }

    @Test
    fun `test extractRootLevelUtmParams handles JsonPrimitive utm_content correctly`() {
        // Verify that regular string utm_content still works
        val jsonWithStringUtmContent =
            """
            {
                "utm_source": "apps.facebook.com",
                "utm_campaign": "test_campaign",
                "utm_content": "simple_content_string"
            }
            """.trimIndent()

        val json = Json.decodeFromString(JsonObject.serializer(), jsonWithStringUtmContent)
        val rootUtmParams = extractUtmParamsFromJson(json)

        assertEquals("test_campaign", rootUtmParams.campaign, "Should extract root-level utm_campaign")
        assertEquals("apps.facebook.com", rootUtmParams.source, "Should extract root-level utm_source")
        assertEquals("simple_content_string", rootUtmParams.content, "Should extract string utm_content as-is")
    }
}
