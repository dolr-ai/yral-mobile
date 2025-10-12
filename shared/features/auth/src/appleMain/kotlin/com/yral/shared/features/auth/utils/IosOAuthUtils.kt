package com.yral.shared.features.auth.utils

import com.yral.shared.features.auth.data.AuthDataSourceImpl.Companion.REDIRECT_URI_HOST
import com.yral.shared.features.auth.data.AuthDataSourceImpl.Companion.REDIRECT_URI_PATH
import com.yral.shared.features.auth.data.AuthDataSourceImpl.Companion.REDIRECT_URI_SCHEME
import com.yral.shared.features.auth.domain.models.TokenClaims
import io.ktor.http.Url
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.Security.SecRandomCopyBytes
import platform.Security.errSecSuccess
import platform.Security.kSecRandomDefault
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class IosOAuthUtils : OAuthUtils {
    override var callBack: ((OAuthResult) -> Unit)? = null
    override var callbackExpiry: Long = 0L

    override fun openOAuth(
        context: Any,
        authUrl: Url,
        callBack: (OAuthResult) -> Unit,
    ) {
        // STUB
    }

    override fun invokeCallback(result: OAuthResult) {
        // STUB
    }

    override fun cleanup() {
        // STUB
    }
}

@OptIn(ExperimentalEncodingApi::class)
class IosOAuthUtilsHelper : OAuthUtilsHelper {
    private val json = Json { ignoreUnknownKeys = true }

    override fun generateCodeVerifier(): String = generateRandomUrlSafeString()

    override fun generateCodeChallenge(codeVerifier: String): String {
        val verifierBytes = codeVerifier.encodeToByteArray()
        val digest = sha256(verifierBytes)
        return digest.toUrlSafeBase64()
    }

    override fun generateState(): String = generateRandomUrlSafeString()

    override fun parseOAuthToken(token: String): TokenClaims {
        val payload = decodeJwtPayload(token)
        val payloadJson = json.parseToJsonElement(payload).jsonObject

        val aud = parseAudience(payloadJson[KEY_AUD])
        val expiry = payloadJson.requireLongClaim(KEY_EXP)
        val issuedAt = payloadJson.requireLongClaim(KEY_IAT)
        val issuerHost = payloadJson.requireStringClaim(KEY_ISS)
        val principal = payloadJson.requireStringClaim(KEY_SUB)
        val nonce = payloadJson[KEY_NONCE]?.jsonPrimitive?.contentOrNull
        val isAnonymous = payloadJson[KEY_IS_ANONYMOUS]?.jsonPrimitive?.booleanOrNull ?: false
        val delegatedIdentity =
            payloadJson[KEY_DELEGATED_IDENTITY]
                ?.takeUnless { it is JsonNull }
                ?.toString()
                ?.encodeToByteArray()
        val email = payloadJson[KEY_EMAIL]?.jsonPrimitive?.contentOrNull

        return TokenClaims(
            aud = aud,
            expiry = expiry,
            issuedAtTime = issuedAt,
            issuerHost = issuerHost,
            principal = principal,
            nonce = nonce,
            extIsAnonymous = isAnonymous,
            delegatedIdentity = delegatedIdentity,
            email = email,
        )
    }

    override fun mapUriToOAuthResult(uri: String): OAuthResult? {
        val url = runCatching { Url(uri) }.getOrElse { return null }
        if (url.protocol.name != REDIRECT_URI_SCHEME ||
            url.host != REDIRECT_URI_HOST ||
            url.encodedPath != REDIRECT_URI_PATH
        ) {
            return null
        }

        val error = url.parameters["error"]
        val errorDescription = url.parameters["error_description"]
        val code = url.parameters["code"]
        val state = url.parameters["state"]

        return when {
            !error.isNullOrBlank() -> {
                OAuthResult.Error(error = error, errorDescription = errorDescription)
            }
            !code.isNullOrBlank() && !state.isNullOrBlank() -> {
                OAuthResult.Success(code = code, state = state)
            }
            else -> {
                OAuthResult.Error(error = "unknown_error", errorDescription = "Missing required parameters")
            }
        }
    }

    private fun generateRandomUrlSafeString(): String {
        val randomBytes = secureRandomBytes(CODE_VERIFIER_LENGTH)
        return randomBytes.toUrlSafeBase64()
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun secureRandomBytes(length: Int): ByteArray {
        val data = UByteArray(length)
        val status =
            data.usePinned { pinned ->
                SecRandomCopyBytes(
                    kSecRandomDefault,
                    length.toULong(),
                    pinned.addressOf(0),
                )
            }
        if (status != errSecSuccess) {
            throw IllegalStateException("SecRandomCopyBytes failed with status: $status")
        }
        return ByteArray(length) { index -> data[index].toByte() }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun sha256(data: ByteArray): ByteArray {
        val digest = UByteArray(CC_SHA256_DIGEST_LENGTH.toInt())
        data.usePinned { inputPinned ->
            digest.usePinned { digestPinned ->
                CC_SHA256(
                    inputPinned.addressOf(0),
                    data.size.toUInt(),
                    digestPinned.addressOf(0),
                )
            }
        }
        return ByteArray(digest.size) { index -> digest[index].toByte() }
    }

    private fun parseAudience(element: JsonElement?): List<String> =
        when (element) {
            null -> emptyList()
            is JsonArray -> element.mapNotNull { it.jsonPrimitive.contentOrNull }.filter { it.isNotBlank() }
            is JsonPrimitive -> element.contentOrNull?.let(::listOf) ?: emptyList()
            else -> emptyList()
        }

    private fun decodeJwtPayload(token: String): String {
        val parts = token.split(".")
        require(parts.size == 3) { "Malformed JWT" }
        val payloadSegment = parts[1]
        val payloadBytes = decodeBase64Url(payloadSegment)
        return payloadBytes.decodeToString()
    }

    private fun decodeBase64Url(payload: String): ByteArray {
        val padded = payload.padBase64()
        return Base64.UrlSafe.decode(padded)
    }

    private fun ByteArray.toUrlSafeBase64(): String = Base64.UrlSafe.encode(this).trimEnd('=')

    private fun String.padBase64(): String {
        val padding = (4 - length % 4) % 4
        return this + "=".repeat(padding)
    }

    private fun JsonObject.requireStringClaim(key: String): String =
        this[key]?.jsonPrimitive?.contentOrNull
            ?: throw IllegalStateException("Missing $key claim")

    private fun JsonObject.requireLongClaim(key: String): Long {
        val primitive =
            this[key]?.jsonPrimitive
                ?: throw IllegalStateException("Missing $key claim")
        return primitive.longOrNull
            ?: primitive.doubleOrNull?.toLong()
            ?: primitive.content.toLong()
    }
}
