package com.yral.android.installReferrer

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import com.yral.android.BuildConfig
import com.yral.shared.core.logging.YralLogger
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.koin.koinInstance
import com.yral.shared.preferences.UtmAttributionStore
import com.yral.shared.preferences.UtmParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.core.qualifier.named
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Suppress("TooGenericExceptionCaught")
class MetaInstallReferrerAttribution(
    private val scope: CoroutineScope,
) {
    private companion object {
        private const val AES_ALGORITHM = "AES"
        private const val GCM_ALGORITHM = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128 // 16 bytes
    }

    private val crashlyticsManager: CrashlyticsManager by lazy { koinInstance.get<CrashlyticsManager>() }
    private val utmAttributionStore: UtmAttributionStore by lazy { koinInstance.get<UtmAttributionStore>() }
    private val logger: Logger by lazy {
        val baseLogger = koinInstance.get<YralLogger>()
        val sentryLogWriter = koinInstance.get<LogWriter>(named("installReferrerLogWriter"))
        baseLogger.withAdditionalLogWriter(sentryLogWriter).withTag("MetaInstallReferrer")
    }

    fun isMetaInstallReferrerData(referrer: String): Boolean {
        val trimmed = referrer.trim()
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            return false
        }
        return trimmed.contains("\"data\"") && trimmed.contains("\"nonce\"")
    }

    fun processEncryptedData(encryptedJsonString: String) {
        if (utmAttributionStore.isInstallReferrerCompleted()) {
            logger.i { "Install referrer attribution already completed, skipping." }
            return
        }

        scope.launch {
            runCatching {
                val installReferrerJson = parseInstallReferrerJson(encryptedJsonString) ?: return@runCatching

                val rootUtmParams = extractRootLevelUtmParams(installReferrerJson)
                val encryptedData = extractEncryptedData(installReferrerJson)
                val decryptionKey = getDecryptionKey()

                val utmParams =
                    if (encryptedData != null && decryptionKey != null) {
                        // Decrypt and merge with root-level values
                        buildUtmParams(rootUtmParams, encryptedData, decryptionKey)
                    } else {
                        // No encrypted data or no key - use root-level values only
                        if (encryptedData != null) {
                            logger.i { "Decryption key not available, using root-level UTM params only" }
                        }
                        rootUtmParams
                    }

                storeUtmParams(utmParams)
            }.onFailure { exception ->
                logger.e(exception) { "Failed to process Meta Install Referrer data" }
                crashlyticsManager.recordException(
                    exception as? Exception ?: Exception(exception),
                    ExceptionType.INSTALL_REFERRER,
                )
            }
        }
    }

    private fun getDecryptionKey(): String? {
        val key = BuildConfig.META_INSTALL_REFERRER_DECRYPTION_KEY
        return key.ifEmpty {
            logger.i { "Decryption key not available" }
            null
        }
    }

    private fun parseInstallReferrerJson(jsonString: String): JsonObject? =
        try {
            Json.decodeFromString(JsonObject.serializer(), jsonString)
        } catch (e: Exception) {
            logger.i(e) { "Could not parse JSON from referrer data" }
            null
        }

    private fun extractRootLevelUtmParams(json: JsonObject): UtmParams =
        UtmParams(
            campaign = json["utm_campaign"]?.jsonPrimitive?.content,
            source = json["utm_source"]?.jsonPrimitive?.content,
            medium = json["utm_medium"]?.jsonPrimitive?.content,
            term = json["utm_term"]?.jsonPrimitive?.content,
            content = json["utm_content"]?.jsonPrimitive?.content,
        )

    private data class EncryptedData(
        val dataHex: String,
        val nonceHex: String,
    )

    private fun extractEncryptedData(json: JsonObject): EncryptedData? {
        val dataHex =
            json["utm_content"]
                ?.jsonObject
                ?.get("source")
                ?.jsonObject
                ?.get("data")
                ?.jsonPrimitive
                ?.content
        val nonceHex =
            json["utm_content"]
                ?.jsonObject
                ?.get("source")
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

    private fun buildUtmParams(
        rootUtmParams: UtmParams,
        encryptedData: EncryptedData?,
        decryptionKey: String,
    ): UtmParams {
        val decryptedUtmParams =
            encryptedData?.let {
                decryptAndMapToUtmParams(it, decryptionKey)
            }

        return if (decryptedUtmParams != null) {
            // Merge: root-level values take priority, fall back to decrypted values
            UtmParams(
                source = rootUtmParams.source?.takeIf { it.isNotBlank() } ?: decryptedUtmParams.source,
                medium = rootUtmParams.medium?.takeIf { it.isNotBlank() } ?: decryptedUtmParams.medium,
                campaign = rootUtmParams.campaign?.takeIf { it.isNotBlank() } ?: decryptedUtmParams.campaign,
                term = rootUtmParams.term?.takeIf { it.isNotBlank() } ?: decryptedUtmParams.term,
                content = rootUtmParams.content?.takeIf { it.isNotBlank() } ?: decryptedUtmParams.content,
            )
        } else {
            // No encrypted data, use root-level values only
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

    private fun storeUtmParams(utmParams: UtmParams) {
        if (utmParams.isEmpty()) {
            logger.d { "No UTM parameters found in Meta Install Referrer data" }
            return
        }

        utmAttributionStore.storeIfEmpty(
            source = utmParams.source,
            medium = utmParams.medium,
            campaign = utmParams.campaign,
            term = utmParams.term,
            content = utmParams.content,
        )
        utmAttributionStore.markInstallReferrerCompleted()
        logger.i {
            "Successfully stored Meta Install Referrer UTM params: " +
                "source=${utmParams.source}, campaign=${utmParams.campaign}, " +
                "term=${utmParams.term}, content=${utmParams.content}"
        }
    }

    /**
     * Maps Facebook campaign metadata to UTM parameters.
     * Reference: https://developers.facebook.com/docs/app-ads/install-referrer
     */
    @Suppress("UnusedPrivateProperty")
    private fun mapCampaignMetadataToUtmParams(metadata: JsonObject): UtmParams {
        // Extract all campaign metadata fields
        val campaignId = metadata["campaign_id"]?.jsonPrimitive?.content
        val campaignName = metadata["campaign_name"]?.jsonPrimitive?.content
        val adgroupId = metadata["adgroup_id"]?.jsonPrimitive?.content
        val adgroupName = metadata["adgroup_name"]?.jsonPrimitive?.content
        val adId = metadata["ad_id"]?.jsonPrimitive?.content
        val campaignGroupId = metadata["campaign_group_id"]?.jsonPrimitive?.content
        val campaignGroupName = metadata["campaign_group_name"]?.jsonPrimitive?.content
        val accountId = metadata["account_id"]?.jsonPrimitive?.content
        val adObjectiveName = metadata["ad_objective_name"]?.jsonPrimitive?.content

        // Map extracted values to UTM parameters
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

    private fun UtmParams.isEmpty(): Boolean =
        source.isNullOrBlank() &&
            medium.isNullOrBlank() &&
            campaign.isNullOrBlank() &&
            term.isNullOrBlank() &&
            content.isNullOrBlank()
}
