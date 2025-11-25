package com.yral.android.installReferrer

import android.net.Uri
import androidx.core.net.toUri
import com.yral.android.BuildConfig
import com.yral.shared.preferences.UtmParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Suppress("TooGenericExceptionCaught")
class MetaInstallReferrerAttribution {
    private companion object {
        private const val AES_ALGORITHM = "AES"
        private const val GCM_ALGORITHM = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128 // 16 bytes
    }

    private val logger = AttributionManager.createLogger("MetaInstallReferrer")

    fun isMetaInstallReferrerData(referrer: String): Boolean {
        val json = convertReferrerToJson(referrer) ?: return false

        // Check if utm_content contains encrypted data structure (source.data and source.nonce)
        val utmContent = json["utm_content"]?.jsonObject
        val source = utmContent?.get("source")?.jsonObject
        val hasData = source?.get("data") != null
        val hasNonce = source?.get("nonce") != null

        return hasData && hasNonce
    }

    suspend fun extractUtmParams(encryptedJsonString: String): UtmParams? =
        withContext(Dispatchers.Default) {
            runCatching {
                val installReferrerJson = convertReferrerToJson(encryptedJsonString) ?: return@withContext null

                val rootUtmParams = extractRootLevelUtmParams(installReferrerJson)
                val encryptedData = extractEncryptedData(installReferrerJson)
                val decryptionKey = getDecryptionKey()

                val utmParams =
                    if (encryptedData != null && decryptionKey != null) {
                        runCatching {
                            buildUtmParams(rootUtmParams, encryptedData, decryptionKey)
                        }.getOrElse {
                            logger.e(it) { "Decryption failed, falling back to root-level UTM params" }
                            rootUtmParams
                        }
                    } else {
                        if (encryptedData != null) {
                            logger.i { "Decryption key not available, using root-level UTM params only" }
                        }
                        rootUtmParams
                    }

                if (utmParams.isEmpty()) {
                    null
                } else {
                    utmParams
                }
            }.getOrElse { exception ->
                logger.e(exception) { "Failed to extract UTM params from Meta Install Referrer data" }
                null
            }
        }

    private fun getDecryptionKey(): String? {
        val key = BuildConfig.META_INSTALL_REFERRER_DECRYPTION_KEY
        return key.ifEmpty {
            logger.e { "Decryption key not available" }
            null
        }
    }

    internal fun convertReferrerToJson(referrerData: String): JsonObject? {
        val trimmed = referrerData.trim()
        return when {
            trimmed.startsWith("{") && trimmed.endsWith("}") -> {
                runCatching {
                    Json.decodeFromString(JsonObject.serializer(), trimmed)
                }.onFailure { e ->
                    logger.e(e) { "Could not parse JSON from referrer data" }
                }.getOrNull()
            }
            trimmed.contains("utm_content=") -> {
                runCatching {
                    val uri = (if (trimmed.contains("://")) trimmed else "https://dummy/?$trimmed").toUri()
                    buildJsonFromQueryParams(uri)
                }.onFailure { e ->
                    logger.e(e) { "Could not parse query string from referrer data: ${e.message}" }
                }.getOrNull()
            }
            else -> null
        }
    }

    private fun buildJsonFromQueryParams(uri: Uri): JsonObject {
        val jsonMap = mutableMapOf<String, JsonElement>()
        uri.getQueryParameter("utm_source")?.let { jsonMap["utm_source"] = JsonPrimitive(it) }
        uri.getQueryParameter("utm_campaign")?.let { jsonMap["utm_campaign"] = JsonPrimitive(it) }
        uri.getQueryParameter("utm_medium")?.let { jsonMap["utm_medium"] = JsonPrimitive(it) }
        uri.getQueryParameter("utm_term")?.let { jsonMap["utm_term"] = JsonPrimitive(it) }
        uri.getQueryParameter("utm_content")?.trim()?.let { content ->
            jsonMap["utm_content"] =
                if (content.startsWith("{") && content.endsWith("}")) {
                    runCatching {
                        Json.decodeFromString(JsonObject.serializer(), content)
                    }.getOrElse { JsonPrimitive(content) }
                } else {
                    JsonPrimitive(content)
                }
        }
        return JsonObject(jsonMap)
    }

    internal fun extractRootLevelUtmParams(json: JsonObject): UtmParams {
        val contentValue =
            when (val utmContent = json["utm_content"]) {
                is JsonPrimitive -> utmContent.content
                is JsonObject -> Json.encodeToString(JsonObject.serializer(), utmContent)
                else -> null // It's null or some other type
            }

        return UtmParams(
            campaign = json["utm_campaign"]?.jsonPrimitive?.content,
            source = json["utm_source"]?.jsonPrimitive?.content,
            medium = json["utm_medium"]?.jsonPrimitive?.content,
            term = json["utm_term"]?.jsonPrimitive?.content,
            content = contentValue,
        )
    }

    internal data class EncryptedData(
        val dataHex: String,
        val nonceHex: String,
    )

    internal fun extractEncryptedData(json: JsonObject): EncryptedData? {
        val utmContent = json["utm_content"] as? JsonObject ?: return null
        val dataHex =
            utmContent["source"]
                ?.jsonObject
                ?.get("data")
                ?.jsonPrimitive
                ?.content
        val nonceHex =
            utmContent["source"]
                ?.jsonObject
                ?.get("nonce")
                ?.jsonPrimitive
                ?.content

        return if (dataHex != null && nonceHex != null) {
            EncryptedData(dataHex, nonceHex)
        } else {
            null
        }
    }

    internal fun buildUtmParams(
        rootUtmParams: UtmParams,
        encryptedData: EncryptedData?,
        decryptionKey: String,
    ): UtmParams {
        val decryptedUtmParams =
            encryptedData?.let {
                decryptAndMapToUtmParams(it, decryptionKey)
            }

        return if (decryptedUtmParams != null) {
            UtmParams(
                source = rootUtmParams.source?.takeIf { it.isNotBlank() } ?: decryptedUtmParams.source,
                medium = rootUtmParams.medium?.takeIf { it.isNotBlank() } ?: decryptedUtmParams.medium,
                campaign = rootUtmParams.campaign?.takeIf { it.isNotBlank() } ?: decryptedUtmParams.campaign,
                term = rootUtmParams.term?.takeIf { it.isNotBlank() } ?: decryptedUtmParams.term,
                content = rootUtmParams.content?.takeIf { it.isNotBlank() } ?: decryptedUtmParams.content,
            )
        } else {
            rootUtmParams
        }
    }

    private fun decryptAndMapToUtmParams(
        encryptedData: EncryptedData,
        decryptionKey: String,
    ): UtmParams {
        val decryptedJsonString = decryptAesGcm(encryptedData.dataHex, encryptedData.nonceHex, decryptionKey)
        val campaignMetadata = Json.decodeFromString(JsonObject.serializer(), decryptedJsonString)
        return mapCampaignMetadataToUtmParams(campaignMetadata)
    }

    /**
     * Maps Facebook campaign metadata to UTM parameters.
     * Reference: https://developers.facebook.com/docs/app-ads/install-referrer
     */
    @Suppress("UnusedPrivateProperty")
    internal fun mapCampaignMetadataToUtmParams(metadata: JsonObject): UtmParams {
        val campaignId = metadata["campaign_id"]?.jsonPrimitive?.content
        val campaignName = metadata["campaign_name"]?.jsonPrimitive?.content
        val adgroupId = metadata["adgroup_id"]?.jsonPrimitive?.content
        val adgroupName = metadata["adgroup_name"]?.jsonPrimitive?.content
        val adId = metadata["ad_id"]?.jsonPrimitive?.content
        val campaignGroupId = metadata["campaign_group_id"]?.jsonPrimitive?.content
        val campaignGroupName = metadata["campaign_group_name"]?.jsonPrimitive?.content
        val accountId = metadata["account_id"]?.jsonPrimitive?.content
        val adObjectiveName = metadata["ad_objective_name"]?.jsonPrimitive?.content

        val utmSource = "meta_ads"
        val utmMedium = "cpc"
        val utmCampaign = campaignName?.takeIf { it.isNotBlank() } ?: campaignId?.takeIf { it.isNotBlank() }
        val utmTerm = adgroupName?.takeIf { it.isNotBlank() } ?: adgroupId?.takeIf { it.isNotBlank() }
        val utmContent = adId?.takeIf { it.isNotBlank() }

        return UtmParams(
            source = utmSource,
            medium = utmMedium,
            campaign = utmCampaign,
            term = utmTerm,
            content = utmContent,
        )
    }

    internal fun decryptAesGcm(
        encryptedDataHex: String,
        nonceHex: String,
        keyString: String,
    ): String {
        val encryptedBytes = hexStringToByteArray(encryptedDataHex)
        val nonceBytes = hexStringToByteArray(nonceHex)
        // Facebook provides the decryption key as a hex string (typically 64 hex chars = 32 bytes = 256-bit)
        val keyBytes = hexStringToByteArray(keyString)
        val keySpec = SecretKeySpec(keyBytes, AES_ALGORITHM)

        val cipher = Cipher.getInstance(GCM_ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, GCMParameterSpec(GCM_TAG_LENGTH, nonceBytes))
        return String(cipher.doFinal(encryptedBytes), Charsets.UTF_8)
    }

    @Suppress("MagicNumber")
    internal fun hexStringToByteArray(hex: String): ByteArray =
        ByteArray(hex.length / 2) { i ->
            ((Character.digit(hex[i * 2], 16) shl 4) + Character.digit(hex[i * 2 + 1], 16)).toByte()
        }
}
