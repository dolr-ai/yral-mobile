package com.yral.android.installReferrer

import co.touchlab.kermit.Logger
import com.yral.shared.preferences.UtmParams
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MetaInstallReferrerAttributionTest {
    @Suppress("ktlint:standard:max-line-length", "MaxLineLength")
    private val encryptedDataHex = "afe56cf6228c6ea8c79da49186e718e92a579824596ae1d0d4d20d7793dca797bd4034ccf467bfae5c79a3981e7a2968c41949237e2b2db678c1c3d39c9ae564c5cafd52f2b77a3dc77bf1bae063114d0283b97417487207735da31ddc1531d5645a9c3e602c195a0ebf69c272aa5fda3a2d781cb47e117310164715a54c7a5a032740584e2789a7b4e596034c16425139a77e507c492b629c848573c714a03a2e7d25b9459b95842332b460f3682d19c35dbc7d53e3a51e0497ff6a6cbb367e760debc4194ae097498108df7b95eac2fa9bac4320077b510be3b7b823248bfe02ae501d9fe4ba179c7de6733c92bf89d523df9e31238ef497b9db719484cbab7531dbf6c5ea5a8087f95d59f5e4f89050e0f1dc03e464168ad76a64cca64b79"
    private val nonceHex = "b7203c6a6fb633d16e9cf5c1"
    private val decryptionKey = "2575590594a9cd809e5bfacf397f8c1ac730dbc38a3e137ecd1ab66591c8c3c9"

    // When set to false,
    // requires actual matching encryptedDataHex, nonceHex, and decryptionKey from your Facebook App Dashboard.
    // If example values are not a matching pair and will cause tests to fail with AEADBadTagException.
    private val skipDecryptionTests = false

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
            "Should detect Meta Install Referrer data",
        )

        assertTrue(
            attribution.isMetaInstallReferrerData("""{"data": "test", "nonce": "test"}"""),
            "Should detect Meta Install Referrer data with data and nonce",
        )

        assertTrue(
            !attribution.isMetaInstallReferrerData("plain text"),
            "Should not detect non-JSON as Meta Install Referrer data",
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
        val rootUtmParams =
            UtmParams(
                campaign = json["utm_campaign"]?.jsonPrimitive?.content,
                source = json["utm_source"]?.jsonPrimitive?.content,
                medium = json["utm_medium"]?.jsonPrimitive?.content,
                term = json["utm_term"]?.jsonPrimitive?.content,
                content =
                    try {
                        json["utm_content"]?.jsonPrimitive?.content
                    } catch (_: IllegalArgumentException) {
                        null // utm_content is an object, not a string
                    },
            )

        assertEquals("test_campaign", rootUtmParams.campaign, "Should extract root-level utm_campaign")
        assertEquals("apps.facebook.com", rootUtmParams.source, "Should extract root-level utm_source")
        assertEquals("cpc", rootUtmParams.medium, "Should extract root-level utm_medium")
        assertEquals("test_term", rootUtmParams.term, "Should extract root-level utm_term")
        assertEquals(null, rootUtmParams.content, "utm_content should be null when it's an object")
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

        // Extract root-level values as UtmParams
        val rootUtmParams =
            UtmParams(
                campaign = json["utm_campaign"]?.jsonPrimitive?.content,
                source = json["utm_source"]?.jsonPrimitive?.content,
                medium = json["utm_medium"]?.jsonPrimitive?.content,
                term = json["utm_term"]?.jsonPrimitive?.content,
                content =
                    try {
                        json["utm_content"]?.jsonPrimitive?.content
                    } catch (_: IllegalArgumentException) {
                        null // utm_content is an object, not a string
                    },
            )

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

        // No root-level UTM parameters - create empty UtmParams
        val rootUtmParams =
            UtmParams(
                campaign = json["utm_campaign"]?.jsonPrimitive?.content,
                source = json["utm_source"]?.jsonPrimitive?.content,
                medium = json["utm_medium"]?.jsonPrimitive?.content,
                term = json["utm_term"]?.jsonPrimitive?.content,
                content =
                    try {
                        json["utm_content"]?.jsonPrimitive?.content
                    } catch (_: IllegalArgumentException) {
                        null // utm_content is an object, not a string
                    },
            )

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

        // Extract root-level values as UtmParams
        val rootUtmParams =
            UtmParams(
                campaign = json["utm_campaign"]?.jsonPrimitive?.content,
                source = json["utm_source"]?.jsonPrimitive?.content,
                medium = json["utm_medium"]?.jsonPrimitive?.content,
                term = json["utm_term"]?.jsonPrimitive?.content,
                content = json["utm_content"]?.jsonPrimitive?.content,
            )

        // Check that utm_content is a string, not an object
        val utmContentObject =
            try {
                json["utm_content"]?.jsonObject
            } catch (_: IllegalArgumentException) {
                null
            }

        assertEquals("simple_campaign", rootUtmParams.campaign, "Should extract root-level utm_campaign")
        assertEquals("apps.facebook.com", rootUtmParams.source, "Should extract root-level utm_source")
        assertEquals("cpc", rootUtmParams.medium, "Should extract root-level utm_medium")
        assertEquals("simple_term", rootUtmParams.term, "Should extract root-level utm_term")
        assertEquals("simple_content", rootUtmParams.content, "Should extract root-level utm_content as string")
        assertEquals(null, utmContentObject, "utm_content should be a string, not an object")
    }

    private fun createTestInstance(): MetaInstallReferrerAttribution {
        val mockScope = kotlinx.coroutines.CoroutineScope(Dispatchers.Unconfined)
        return MetaInstallReferrerAttribution(mockScope)
    }
}
