package com.yral.shared.http.spacetime

import com.yral.shared.core.AppConfigurations
import com.yral.shared.http.exception.NetworkException
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.path
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonNull
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.jsonObject

/**
 * Remote data source for SpacetimeDB REST API calls.
 *
 * Calls SpacetimeDB procedures and reducers via the REST endpoint:
 * `POST /v1/database/{db}/call/{name}` with a JSON array body of positional
 * arguments and the user's yral-auth JWT as the `Authorization: Bearer` token.
 *
 * The JWT's `iss` (issuer) and `sub` (subject) claims are used by SpacetimeDB
 * to compute the caller's `Identity` — this is the same mechanism yral-auth
 * uses for its admin token. User JWTs from yral-auth (Google OAuth / phone OTP)
 * work identently.
 *
 * ## REST response format
 * SpacetimeDB wraps procedure results in an outer array: `[[<result>]]`.
 * For `Option<T>`:
 * - `Some(value)` → `[[0, <value JSON>]]`
 * - `None` → `[[1, []]]`
 */
class SpacetimeDBRemoteDataSource(
    private val httpClient: HttpClient,
    private val json: Json,
    private val preferences: Preferences,
) {
    private val baseUrl: String = AppConfigurations.SPACETIMEDB_BASE_URL
    private val dbName: String = AppConfigurations.SPACETIMEDB_DB_NAME

    // ─────────────────────────────────────────────────────────────────────
    // Post procedures (reads)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Get a single post by ID from SpacetimeDB.
     *
     * Calls the `get_post_by_id` procedure, which checks the `posts_v2`
     * table first (has `creator_principal_text`), falling back to the
     * legacy `posts` table.
     *
     * @param postId The post's UUID.
     * @return `SpacetimePostDetails` if found, `null` if the post doesn't
     *         exist or is deleted.
     */
    suspend fun getPostById(postId: String): SpacetimePostDetails? {
        val idToken = getIdTokenOrNull()
        val responseBody = callProcedure("get_post_by_id", listOf(JsonPrimitive(postId)), idToken)
        return parseOption(responseBody, SpacetimePostDetails.serializer())
    }

    /**
     * Get a single post by ID — alias for IC-compatible naming.
     * Calls `get_individual_post_details_by_id`.
     */
    suspend fun getIndividualPostDetailsById(postId: String): SpacetimePostDetails? {
        val idToken = getIdTokenOrNull()
        val responseBody =
            callProcedure("get_individual_post_details_by_id", listOf(JsonPrimitive(postId)), idToken)
        return parseOption(responseBody, SpacetimePostDetails.serializer())
    }

    /**
     * Get a page of a user's visible posts by principal text (offset pagination).
     * Calls `get_posts_of_user_by_principal`.
     *
     * @param creatorPrincipalText The creator's IC Principal as text.
     * @param offset Number of posts to skip.
     * @param limit Maximum number of posts to return.
     */
    suspend fun getPostsOfUserByPrincipal(
        creatorPrincipalText: String,
        offset: ULong,
        limit: ULong,
    ): SpacetimePostListOffset {
        val idToken = getIdTokenOrNull()
        val responseBody =
            callProcedure(
                "get_posts_of_user_by_principal",
                listOf(
                    JsonPrimitive(creatorPrincipalText),
                    JsonPrimitive(offset.toLong()),
                    JsonPrimitive(limit.toLong()),
                ),
                idToken,
            )
        // The procedure returns a Vec<String> of post IDs; fetch details for each.
        val postIds = parsePostIdList(responseBody)
        val posts = mutableListOf<SpacetimePostDetails>()
        for (postId in postIds) {
            getPostById(postId)?.let { posts.add(it) }
        }
        return SpacetimePostListOffset(posts = posts)
    }

    /**
     * Get a page of the current caller's draft posts by principal text (offset pagination).
     * Calls `get_draft_posts_of_user_by_principal`.
     *
     * @param creatorPrincipalText The creator's IC Principal as text.
     * @param offset Number of posts to skip.
     * @param limit Maximum number of posts to return.
     */
    suspend fun getDraftPostsOfUserByPrincipal(
        creatorPrincipalText: String,
        offset: ULong,
        limit: ULong,
    ): SpacetimePostListOffset {
        val idToken = getIdTokenOrNull()
        val responseBody =
            callProcedure(
                "get_draft_posts_of_user_by_principal",
                listOf(
                    JsonPrimitive(creatorPrincipalText),
                    JsonPrimitive(offset.toLong()),
                    JsonPrimitive(limit.toLong()),
                ),
                idToken,
            )
        // The procedure returns a Vec<String> of post IDs; fetch details for each.
        val postIds = parsePostIdList(responseBody)
        val posts = mutableListOf<SpacetimePostDetails>()
        for (postId in postIds) {
            getPostById(postId)?.let { posts.add(it) }
        }
        return SpacetimePostListOffset(posts = posts)
    }

    // ─────────────────────────────────────────────────────────────────────
    // User info procedures (reads)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Get profile details V7 for a user. Returns `null` if the user doesn't exist.
     * Calls `get_user_profile_details_v7`.
     *
     * @param principalText The user's IC Principal as text (or SpacetimeDB Identity hex).
     */
    suspend fun getUserProfileDetailsV7(principalText: String): SpacetimeUserProfileV7? {
        val idToken = getIdTokenOrNull()
        val responseBody =
            callProcedure("get_user_profile_details_v7", listOf(JsonPrimitive(principalText)), idToken)
        return parseOption(responseBody, SpacetimeUserProfileV7.serializer())
    }

    /**
     * Batch profile lookup. Returns V7 profile details for each principal.
     * Users that are not found are silently skipped.
     * Calls `get_users_profile_details`.
     *
     * @param principalTexts List of IC Principal texts to look up.
     */
    suspend fun getUsersProfileDetails(principalTexts: List<String>): List<SpacetimeUserProfileV7> {
        val idToken = getIdTokenOrNull()
        val args = JsonArray(principalTexts.map { JsonPrimitive(it) })
        val responseBody =
            callProcedure("get_users_profile_details", listOf(args), idToken)
        return parseResultList(responseBody, SpacetimeUserProfileV7.serializer())
    }

    /**
     * Get a page of followers for a user (cursor-paginated).
     * Calls `get_followers`.
     *
     * @param principalText The user whose followers to fetch.
     * @param limit Maximum number of followers per page.
     * @param cursor The `nextCursor` from the previous page, or `null` to start.
     */
    suspend fun getFollowers(
        principalText: String,
        limit: ULong,
        cursor: String?,
    ): SpacetimeFollowersPage {
        val idToken = getIdTokenOrNull()
        val responseBody =
            callProcedure(
                "get_followers",
                listOf(
                    JsonPrimitive(principalText),
                    JsonPrimitive(limit.toLong()),
                    cursor?.let { JsonPrimitive(it) } ?: JsonNull,
                ),
                idToken,
            )
        return parseResult(responseBody, SpacetimeFollowersPage.serializer())
    }

    /**
     * Get a page of users that a user is following (cursor-paginated).
     * Calls `get_following`.
     *
     * @param principalText The user whose following list to fetch.
     * @param limit Maximum number of items per page.
     * @param cursor The `nextCursor` from the previous page, or `null` to start.
     */
    suspend fun getFollowing(
        principalText: String,
        limit: ULong,
        cursor: String?,
    ): SpacetimeFollowingPage {
        val idToken = getIdTokenOrNull()
        val responseBody =
            callProcedure(
                "get_following",
                listOf(
                    JsonPrimitive(principalText),
                    JsonPrimitive(limit.toLong()),
                    cursor?.let { JsonPrimitive(it) } ?: JsonNull,
                ),
                idToken,
            )
        return parseResult(responseBody, SpacetimeFollowingPage.serializer())
    }

    // ─────────────────────────────────────────────────────────────────────
    // User info reducers (writes)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Follow another user. Calls the `follow_user` reducer.
     * The caller's identity is derived from the JWT.
     *
     * @param followeePrincipalText The IC Principal text of the user to follow.
     */
    suspend fun followUser(followeePrincipalText: String) {
        val idToken = getIdTokenOrThrow()
        callProcedure("follow_user", listOf(JsonPrimitive(followeePrincipalText)), idToken)
    }

    /**
     * Unfollow a user. Calls the `unfollow_user` reducer.
     *
     * @param followeePrincipalText The IC Principal text of the user to unfollow.
     */
    suspend fun unfollowUser(followeePrincipalText: String) {
        val idToken = getIdTokenOrThrow()
        callProcedure("unfollow_user", listOf(JsonPrimitive(followeePrincipalText)), idToken)
    }

    /**
     * Register the caller as a new user. Idempotent. Calls `register_new_user`.
     */
    suspend fun registerNewUser() {
        val idToken = getIdTokenOrThrow()
        callProcedure("register_new_user", emptyList(), idToken)
    }

    /**
     * Update the caller's profile details. Calls `update_profile_details_v2`.
     *
     * @param bio New bio, or `null` to leave unchanged.
     * @param websiteUrl New website URL, or `null` to leave unchanged.
     * @param profilePictureUrl New profile picture URL, or `null` to leave unchanged.
     */
    suspend fun updateProfileDetails(
        bio: String?,
        websiteUrl: String?,
        profilePictureUrl: String?,
    ) {
        val idToken = getIdTokenOrThrow()
        callProcedure(
            "update_profile_details",
            listOf(
                bio?.let { JsonPrimitive(it) } ?: JsonNull,
                websiteUrl?.let { JsonPrimitive(it) } ?: JsonNull,
                profilePictureUrl?.let { JsonPrimitive(it) } ?: JsonNull,
            ),
            idToken,
        )
    }

    /**
     * Register a new user or create a bot account. Calls `accept_new_user_registration_v2`.
     *
     * @param newPrincipalText The IC Principal text of the new user.
     * @param authenticated Whether the user is authenticated (accepted for API compat, not used).
     * @param mainAccountText The owner's principal text for bot accounts, or `null` for normal accounts.
     */
    suspend fun acceptNewUserRegistrationV2(
        newPrincipalText: String,
        authenticated: Boolean,
        mainAccountText: String?,
    ) {
        val idToken = getIdTokenOrThrow()
        callProcedure(
            "accept_new_user_registration_v2",
            listOf(
                JsonPrimitive(newPrincipalText),
                JsonPrimitive(authenticated),
                mainAccountText?.let { JsonPrimitive(it) } ?: JsonNull,
            ),
            idToken,
        )
    }

    /**
     * Delete a user's profile. Admin or self only. Calls `delete_user_info`.
     *
     * @param principalToDeleteText The IC Principal text of the user to delete.
     */
    suspend fun deleteUserInfo(principalToDeleteText: String) {
        val idToken = getIdTokenOrThrow()
        callProcedure("delete_user_info", listOf(JsonPrimitive(principalToDeleteText)), idToken)
    }

    /**
     * Register a device notification token for the caller. Calls `register_notification_token`.
     *
     * @param token The FCM/APNS device token.
     */
    suspend fun registerNotificationToken(token: String) {
        val idToken = getIdTokenOrThrow()
        callProcedure("register_notification_token", listOf(JsonPrimitive(token)), idToken)
    }

    /**
     * Unregister a device notification token for the caller. Calls `unregister_notification_token`.
     *
     * @param token The FCM/APNS device token.
     */
    suspend fun unregisterNotificationToken(token: String) {
        val idToken = getIdTokenOrThrow()
        callProcedure("unregister_notification_token", listOf(JsonPrimitive(token)), idToken)
    }

    /**
     * Update the caller's last access time. Calls `update_user_last_access_time`.
     */
    suspend fun updateUserLastAccessTime() {
        val idToken = getIdTokenOrThrow()
        callProcedure("update_user_last_access_time", emptyList(), idToken)
    }

    // ─────────────────────────────────────────────────────────────────────
    // REST call + parsing helpers
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Call a SpacetimeDB procedure or reducer via REST.
     *
     * @param name The procedure/reducer name (e.g. "get_post_by_id").
     * @param args Positional arguments as a list of JSON elements (strings, numbers, nulls, arrays).
     * @param token Optional bearer token (user JWT). If null, the call is
     *              anonymous (SpacetimeDB sets `sender = Identity::ZERO`).
     * @return The raw response body as a string.
     */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun callProcedure(
        name: String,
        args: List<JsonElement>,
        token: String?,
    ): String {
        try {
            val response =
                httpClient.post {
                    url {
                        host = baseUrl
                        path("v1", "database", dbName, "call", name)
                    }
                    contentType(ContentType.Application.Json)
                    if (token != null) {
                        header(HttpHeaders.Authorization, "Bearer $token")
                    }
                    // SpacetimeDB expects a JSON array of positional arguments.
                    val jsonArgs = JsonArray(args)
                    setBody(json.encodeToString(JsonArray.serializer(), jsonArgs))
                }
            return response.bodyAsText()
        } catch (e: Exception) {
            throw NetworkException(e)
        }
    }

    /**
     * Parse the `Option<T>` REST response.
     *
     * The response is `[[variant_index, payload]]`:
     * - `[[0, {<T JSON>}]]` → Some(value)
     * - `[[1, []]]` → None
     */
    @Suppress("ThrowsCount")
    private fun <T> parseOption(
        responseBody: String,
        serializer: kotlinx.serialization.KSerializer<T>,
    ): T? {
        val outerArray =
            json.parseToJsonElement(responseBody) as? JsonArray
                ?: throw parseError("expected JSON array", responseBody)

        val innerArray =
            outerArray.firstOrNull() as? JsonArray
                ?: throw parseError("expected nested array", responseBody)

        val variantIndex =
            (innerArray.getOrNull(0) as? JsonPrimitive)?.content?.toIntOrNull()
                ?: throw parseError("expected variant index", responseBody)

        if (variantIndex != 0) return null // None

        val payload =
            innerArray.getOrNull(1)
                ?: throw parseError("expected payload", responseBody)

        return json.decodeFromString(serializer, payload.toString())
    }

    /**
     * Parse a direct (non-Option) REST response.
     *
     * The response is `[{<T JSON>}]` — a single object wrapped in an outer array.
     */
    @Suppress("ThrowsCount")
    private fun <T> parseResult(
        responseBody: String,
        serializer: kotlinx.serialization.KSerializer<T>,
    ): T {
        val outerArray =
            json.parseToJsonElement(responseBody) as? JsonArray
                ?: throw parseError("expected JSON array", responseBody)

        val payload =
            outerArray.firstOrNull()
                ?: throw parseError("expected result payload", responseBody)

        return json.decodeFromString(serializer, payload.toString())
    }

    /**
     * Parse a `Vec<T>` REST response.
     *
     * The response is `[{<T JSON>}, {<T JSON>}, ...]` — an array of objects
     * wrapped in an outer array.
     */
    @Suppress("ThrowsCount")
    private fun <T> parseResultList(
        responseBody: String,
        serializer: kotlinx.serialization.KSerializer<T>,
    ): List<T> {
        val outerArray =
            json.parseToJsonElement(responseBody) as? JsonArray
                ?: throw parseError("expected JSON array", responseBody)

        val innerArray =
            outerArray.firstOrNull() as? JsonArray
                ?: throw parseError("expected nested array", responseBody)

        return innerArray.map { json.decodeFromString(serializer, it.toString()) }
    }

    /**
     * Parse a `Vec<String>` REST response — a list of string post IDs.
     *
     * SpacetimeDB wraps the result in an outer array, so the response may be
     * `["id1", "id2", ...]` or `[["id1", "id2", ...]]` depending on the procedure.
     * This handles both formats.
     */
    private fun parsePostIdList(responseBody: String): List<String> {
        val outerArray =
            json.parseToJsonElement(responseBody) as? JsonArray
                ?: throw parseError("expected JSON array", responseBody)

        // Try flat format: ["id1", "id2", ...]
        val flatStrings: List<String> =
            outerArray.mapNotNull { element ->
                (element as? JsonPrimitive)?.content
            }
        if (flatStrings.isNotEmpty()) return flatStrings

        // Try nested format: [["id1", "id2", ...]]
        val innerArray =
            outerArray.firstOrNull() as? JsonArray
                ?: throw parseError("expected string array or nested array", responseBody)

        return innerArray.mapNotNull { element ->
            (element as? JsonPrimitive)?.content
        }
    }

    private fun parseError(
        expected: String,
        responseBody: String,
    ): NetworkException =
        NetworkException(
            IllegalArgumentException("SpacetimeDB: $expected, got: $responseBody"),
        )

    /**
     * Read the yral-auth JWT from preferences. Returns null if not authenticated.
     * SpacetimeDB accepts any OIDC-compliant JWT — anonymous calls use
     * `Identity::ZERO` as the sender.
     */
    private suspend fun getIdTokenOrNull(): String? = preferences.getString(PrefKeys.ID_TOKEN.name)

    /**
     * Read the yral-auth JWT from preferences. Throws if not authenticated.
     */
    private suspend fun getIdTokenOrThrow(): String =
        getIdTokenOrNull()
            ?: throw NetworkException(IllegalStateException("Not authenticated — no ID token in preferences"))
}
