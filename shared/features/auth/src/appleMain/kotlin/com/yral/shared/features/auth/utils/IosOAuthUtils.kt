package com.yral.shared.features.auth.utils

import com.yral.shared.core.logging.YralLogger
import com.yral.shared.features.auth.di.AuthEnv
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
import platform.AuthenticationServices.ASPresentationAnchor
import platform.AuthenticationServices.ASWebAuthenticationPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASWebAuthenticationSession
import platform.AuthenticationServices.ASWebAuthenticationSessionErrorCodeCanceledLogin
import platform.AuthenticationServices.ASWebAuthenticationSessionErrorDomain
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.Security.SecRandomCopyBytes
import platform.Security.errSecSuccess
import platform.Security.kSecRandomDefault
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class IosOAuthUtils(
    yralLogger: YralLogger,
    authEnv: AuthEnv,
    private val helper: OAuthUtilsHelper,
) : OAuthUtils {
    private val redirectUri: AuthEnv.RedirectUri = authEnv.redirectUri
    private val logger = yralLogger.withTag("IosOAuthUtils")
    override var callBack: ((OAuthResult) -> Unit)? = null
    override var callbackExpiry: Long = 0L
    private var authSession: ASWebAuthenticationSession? = null
    private var currentPresentationProvider: OAuthPresentationAnchorProvider? = null

    @OptIn(ExperimentalTime::class)
    override fun openOAuth(
        context: Any,
        authUrl: Url,
        callBack: (OAuthResult) -> Unit,
    ) {
        logger.d { "openOAuth" }
        val nsUrl = NSURL(string = authUrl.toString())
        this.callBack = callBack
        this.callbackExpiry = Clock.System.now().toEpochMilliseconds() + CALLBACK_TIMEOUT_MS
        val provider = OAuthPresentationAnchorProvider(context as? UIViewController)
        currentPresentationProvider = provider
        val strongSelf = this
        dispatch_async(dispatch_get_main_queue()) {
            strongSelf.startSession(nsUrl, provider)
        }
    }

    @OptIn(ExperimentalTime::class)
    override fun invokeCallback(result: OAuthResult) {
        logger.d { "invokeCallback" }
        if (callbackExpiry > 0 && Clock.System.now().toEpochMilliseconds() > callbackExpiry) {
            logger.d { "invokeCallback TimedOut" }
            callBack?.invoke(OAuthResult.TimedOut)
        } else {
            callBack?.invoke(result)
        }
        cleanup()
    }

    override fun cleanup() {
        logger.d { "cleanup" }
        val session = authSession
        authSession = null
        currentPresentationProvider = null
        callBack = null
        callbackExpiry = 0L
        if (session != null) {
            dispatch_async(dispatch_get_main_queue()) {
                session.cancel()
            }
        }
    }

    private fun startSession(
        url: NSURL,
        provider: OAuthPresentationAnchorProvider,
    ) {
        logger.d { "startSession" }
        authSession?.cancel()
        val session =
            ASWebAuthenticationSession(
                uRL = url,
                callbackURLScheme = redirectUri.scheme,
                completionHandler = { callbackUrl, error ->
                    handleSessionCompletion(callbackUrl, error)
                },
            )
        session.presentationContextProvider = provider
        session.prefersEphemeralWebBrowserSession = true
        authSession = session
        if (!session.start()) {
            logger.d { "failed to start session" }
            dispatch_async(dispatch_get_main_queue()) {
                invokeCallback(
                    OAuthResult.Error(
                        error = "session_start_failed",
                        errorDescription = "Unable to launch ASWebAuthenticationSession",
                    ),
                )
            }
        }
    }

    private fun handleSessionCompletion(
        callbackUrl: NSURL?,
        error: NSError?,
    ) {
        logger.d { "handleSessionCompletion" }
        val result =
            when {
                callbackUrl != null -> {
                    callbackUrl.absoluteString?.let { helper.mapUriToOAuthResult(it) }
                        ?: OAuthResult.Error(
                            error = "invalid_callback",
                            errorDescription = "OAuth callback missing required parameters",
                        )
                }
                error != null -> {
                    if (error.domain == ASWebAuthenticationSessionErrorDomain &&
                        error.code == ASWebAuthenticationSessionErrorCodeCanceledLogin
                    ) {
                        OAuthResult.Cancelled
                    } else {
                        OAuthResult.Error(
                            error = error.domain ?: "unknown_error",
                            errorDescription = error.localizedDescription,
                        )
                    }
                }
                else -> {
                    OAuthResult.Error(
                        error = "unknown_error",
                        errorDescription = "OAuth flow finished without a callback URL or error",
                    )
                }
            }
        dispatch_async(dispatch_get_main_queue()) {
            invokeCallback(result)
        }
    }

    companion object {
        private const val CALLBACK_TIMEOUT_MS = 5 * 60 * 1000L
    }
}

private class OAuthPresentationAnchorProvider(
    private val viewController: UIViewController?,
) : NSObject(),
    ASWebAuthenticationPresentationContextProvidingProtocol {
    override fun presentationAnchorForWebAuthenticationSession(session: ASWebAuthenticationSession): ASPresentationAnchor {
        viewController?.view?.window?.let { return it }
        return resolveDefaultWindow()
    }

    private fun resolveDefaultWindow(): UIWindow {
        UIApplication.sharedApplication.keyWindow?.let { return it }
        val windows = UIApplication.sharedApplication.windows
        val count = windows.count()
        for (index in 0 until count) {
            val window = windows[index] as? UIWindow
            if (window != null && !window.hidden) {
                return window
            }
        }
        return UIWindow()
    }
}

@OptIn(ExperimentalEncodingApi::class)
class IosOAuthUtilsHelper(
    authEnv: AuthEnv,
) : OAuthUtilsHelper {
    private val redirectUri: AuthEnv.RedirectUri = authEnv.redirectUri
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
        val botDelegatedIdentities =
            payloadJson[KEY_AI_ACCOUNT_DELEGATED_IDENTITIES]
                ?.jsonArrayOrNull()
                ?.mapNotNull { element ->
                    when (element) {
                        is JsonNull -> null
                        is JsonPrimitive -> element.contentOrNull?.encodeToByteArray()
                        else -> element.toString().encodeToByteArray()
                    }
                }
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
            botDelegatedIdentities = botDelegatedIdentities,
        )
    }

    override fun mapUriToOAuthResult(uri: String): OAuthResult? {
        val url = runCatching { Url(uri) }.getOrElse { return null }
        if (url.protocol.name != redirectUri.scheme ||
            url.host != redirectUri.host ||
            url.encodedPath != redirectUri.path
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

    private fun JsonElement.jsonArrayOrNull(): JsonArray? = this as? JsonArray

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
        val digest = UByteArray(CC_SHA256_DIGEST_LENGTH)
        data.usePinned { inputPinned ->
            digest.usePinned { digestPinned ->
                // Avoid addressOf(0) on empty arrays; CommonCrypto tolerates null when length is zero.
                val dataPointer = if (data.isNotEmpty()) inputPinned.addressOf(0) else null
                CC_SHA256(
                    dataPointer,
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
