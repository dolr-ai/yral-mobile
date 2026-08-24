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
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

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
 * SpacetimeDB's REST `/call` endpoint serializes procedure return values
 * using `SerdeWrapper(AlgebraicValue)`, which serializes SATS product types
 * (structs) as **positional JSON arrays** and sum types (Option, enums) as
 * `[variant_tag, payload]`. The response body IS the return value directly
 * — there is no extra wrapping array.
 *
 * For `Option<T>` (a SATS sum type with variants `Some` = tag 0, `None` = tag 1):
 * - `Some(value)` → `[0, <value SATS array> ]`
 * - `None` → `[1, []]`
 *
 * For a struct like `PostDetailsForFrontend`:
 * - `["id", "description", [...hashtags], "videoUid", [...identity], "creatorOauthSubject", [...timestamp], viewCount, likeCount, likedByMe, [statusTag, []]]`
 */
@Suppress("TooManyFunctions")
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
     * Calls the `get_post_by_id` procedure, which checks the `posts_3`
     * table first (has `creator_oauth_subject`), lazily migrating from
     * `posts_v2`, falling back to the legacy `posts` table.
     *
     * @param postId The post's UUID.
     * @return `SpacetimePostDetails` if found, `null` if the post doesn't
     *         exist or is deleted.
     */
    suspend fun getPostById(postId: String): SpacetimePostDetails? {
        val idToken = getIdTokenOrNull()
        val responseBody = callProcedure("get_post_by_id", listOf(JsonPrimitive(postId)), idToken)
        return parseOptionPost(responseBody)
    }

    /**
     * Get a single post by ID — alias for IC-compatible naming.
     * Calls `get_individual_post_details_by_id`.
     */
    suspend fun getIndividualPostDetailsById(postId: String): SpacetimePostDetails? {
        val idToken = getIdTokenOrNull()
        val responseBody =
            callProcedure("get_individual_post_details_by_id", listOf(JsonPrimitive(postId)), idToken)
        return parseOptionPost(responseBody)
    }

    /**
     * Get a page of a user's visible posts by OAuth subject (offset pagination).
     * Calls `get_posts_of_user_by_principal`.
     *
     * @param creatorOauthSubject The creator's OAuth subject (`sub` claim from yral-auth JWT).
     * @param offset Number of posts to skip.
     * @param limit Maximum number of posts to return.
     */
    suspend fun getPostsOfUserByPrincipal(
        creatorOauthSubject: String,
        offset: ULong,
        limit: ULong,
    ): SpacetimePostListOffset {
        val idToken = getIdTokenOrNull()
        val responseBody =
            callProcedure(
                "get_posts_of_user_by_principal",
                listOf(
                    JsonPrimitive(creatorOauthSubject),
                    JsonPrimitive(offset.toLong()),
                    JsonPrimitive(limit.toLong()),
                ),
                idToken,
            )
        // The procedure returns PostListOffset (full post objects), not post IDs.
        return parsePostListOffset(responseBody)
    }

    /**
     * Get a page of the current caller's draft posts by OAuth subject (offset pagination).
     * Calls `get_draft_posts_of_user_by_principal`.
     *
     * @param creatorOauthSubject The creator's OAuth subject (`sub` claim from yral-auth JWT).
     * @param offset Number of posts to skip.
     * @param limit Maximum number of posts to return.
     */
    suspend fun getDraftPostsOfUserByPrincipal(
        creatorOauthSubject: String,
        offset: ULong,
        limit: ULong,
    ): SpacetimePostListOffset {
        val idToken = getIdTokenOrNull()
        val responseBody =
            callProcedure(
                "get_draft_posts_of_user_by_principal",
                listOf(
                    JsonPrimitive(creatorOauthSubject),
                    JsonPrimitive(offset.toLong()),
                    JsonPrimitive(limit.toLong()),
                ),
                idToken,
            )
        // The procedure returns PostListOffset (full post objects), not post IDs.
        return parsePostListOffset(responseBody)
    }

    // ─────────────────────────────────────────────────────────────────────
    // User info procedures (reads)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Get profile details V7 for a user. Returns `null` if the user doesn't exist.
     * Calls `get_user_profile_details_v_7`.
     *
     * Note: SpacetimeDB's automatic snake_case conversion splits `v7` into `v_7`,
     * so the REST procedure name is `get_user_profile_details_v_7`, not
     * `get_user_profile_details_v7`.
     *
     * @param oauthSubject The user's OAuth subject (or SpacetimeDB Identity hex).
     */
    suspend fun getUserProfileDetailsV7(oauthSubject: String): SpacetimeUserProfileV7? {
        val idToken = getIdTokenOrNull()
        val responseBody =
            callProcedure("get_user_profile_details_v_7", listOf(JsonPrimitive(oauthSubject)), idToken)
        return parseOptionUserProfile(responseBody)
    }

    /**
     * Batch profile lookup. Returns V7 profile details for each principal.
     * Users that are not found are silently skipped.
     * Calls `get_users_profile_details`.
     *
     * @param oauthSubjects List of OAuth subjects to look up.
     */
    suspend fun getUsersProfileDetails(oauthSubjects: List<String>): List<SpacetimeUserProfileV7> {
        val idToken = getIdTokenOrNull()
        val args = JsonArray(oauthSubjects.map { JsonPrimitive(it) })
        val responseBody =
            callProcedure("get_users_profile_details", listOf(args), idToken)
        return parseUserProfileList(responseBody)
    }

    /**
     * Get a page of followers for a user (cursor-paginated).
     * Calls `get_followers`.
     *
     * @param oauthSubject The user whose followers to fetch.
     * @param limit Maximum number of followers per page.
     * @param cursor The `nextCursor` from the previous page, or `null` to start.
     */
    suspend fun getFollowers(
        oauthSubject: String,
        limit: ULong,
        cursor: String?,
    ): SpacetimeFollowersPage {
        val idToken = getIdTokenOrNull()
        val responseBody =
            callProcedure(
                "get_followers",
                listOf(
                    JsonPrimitive(oauthSubject),
                    JsonPrimitive(limit.toLong()),
                    cursor?.let { JsonPrimitive(it) } ?: JsonNull,
                ),
                idToken,
            )
        return parseFollowersPage(responseBody)
    }

    /**
     * Get a page of users that a user is following (cursor-paginated).
     * Calls `get_following`.
     *
     * @param oauthSubject The user whose following list to fetch.
     * @param limit Maximum number of items per page.
     * @param cursor The `nextCursor` from the previous page, or `null` to start.
     */
    suspend fun getFollowing(
        oauthSubject: String,
        limit: ULong,
        cursor: String?,
    ): SpacetimeFollowingPage {
        val idToken = getIdTokenOrNull()
        val responseBody =
            callProcedure(
                "get_following",
                listOf(
                    JsonPrimitive(oauthSubject),
                    JsonPrimitive(limit.toLong()),
                    cursor?.let { JsonPrimitive(it) } ?: JsonNull,
                ),
                idToken,
            )
        return parseFollowingPage(responseBody)
    }

    // ─────────────────────────────────────────────────────────────────────
    // User info reducers (writes)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Follow another user. Calls the `follow_user` reducer.
     * The caller's identity is derived from the JWT.
     *
     * @param followeeSubject The OAuth subject of the user to follow.
     */
    suspend fun followUser(followeeSubject: String) {
        val idToken = getIdTokenOrThrow()
        callProcedure("follow_user", listOf(JsonPrimitive(followeeSubject)), idToken)
    }

    /**
     * Unfollow a user. Calls the `unfollow_user` reducer.
     *
     * @param followeeSubject The OAuth subject of the user to unfollow.
     */
    suspend fun unfollowUser(followeeSubject: String) {
        val idToken = getIdTokenOrThrow()
        callProcedure("unfollow_user", listOf(JsonPrimitive(followeeSubject)), idToken)
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
     * Register a new user or create a bot account. Calls `accept_new_user_registration_v_2`.
     *
     * @param newPrincipalText The principal text of the new user.
     * @param authenticated Whether the user is authenticated (accepted for API compat, not used).
     * @param mainAccountText The owner's principal text for bot accounts, or `null` for normal accounts.
     */
    suspend fun acceptNewUserRegistrationV2(
        newPrincipalText: String,
        authenticated: Boolean,
        mainAccountText: String?,
    ) {
        val idToken = getIdTokenOrThrow()
        // main_account_text is Option<String>. As a reducer ARGUMENT, SpacetimeDB
        // encodes a sum type as [variantIndex, payload]: Some(v) -> [0, v],
        // None -> [1, []]. (The doubly-nested [[...]] form is the *response* shape.)
        val mainAccount: JsonElement =
            if (mainAccountText != null) {
                JsonArray(listOf(JsonPrimitive(0), JsonPrimitive(mainAccountText)))
            } else {
                JsonArray(listOf(JsonPrimitive(1), JsonArray(emptyList())))
            }
        callProcedure(
            // Deployed module mangles the Rust fn name `..._v2` → reducer `..._v_2`
            // (SpacetimeDB codegen inserts an underscore before the digit).
            "accept_new_user_registration_v_2",
            listOf(
                JsonPrimitive(newPrincipalText),
                JsonPrimitive(authenticated),
                mainAccount,
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

    // ─────────────────────────────────────────────────────────────────────
    // Positional SATS response parsers
    // ─────────────────────────────────────────────────────────────────────
    //
    // SpacetimeDB's REST `/call` endpoint serializes procedure return values
    // using `SerdeWrapper(AlgebraicValue)`. Product types (structs) are
    // positional JSON arrays; sum types (Option, enums) are `[tag, payload]`.
    // The response body IS the return value — no extra wrapping array.

    /**
     * Parse an `Option<PostDetailsForFrontend>` REST response.
     * The response is a SATS sum type: `[0, postArray]` for Some, `[1, []]` for None.
     */
    private fun parseOptionPost(responseBody: String): SpacetimePostDetails? {
        val array = parseResponseArray(responseBody)
        val payload = parseOptionArray(array) ?: return null
        return SpacetimePostDetails.fromJsonArray(payload)
    }

    /**
     * Parse an `Option<UserProfileDetailsV7>` REST response.
     * The response is a SATS sum type: `[0, profileArray]` for Some, `[1, []]` for None.
     */
    private fun parseOptionUserProfile(responseBody: String): SpacetimeUserProfileV7? {
        val array = parseResponseArray(responseBody)
        val payload = parseOptionArray(array) ?: return null
        return SpacetimeUserProfileV7.fromJsonArray(payload)
    }

    /**
     * Parse a `PostListOffset` REST response.
     * The response is a SATS product type: `[[postArray0, postArray1, ...]]`.
     */
    private fun parsePostListOffset(responseBody: String): SpacetimePostListOffset {
        val array = parseResponseArray(responseBody)
        return SpacetimePostListOffset.fromJsonArray(array)
    }

    /**
     * Parse a `FollowersPage` REST response.
     * The response is a SATS product type: `[[followerItem0, ...], totalCount, [0, cursor] | [1, []]]`.
     */
    private fun parseFollowersPage(responseBody: String): SpacetimeFollowersPage {
        val array = parseResponseArray(responseBody)
        return SpacetimeFollowersPage.fromJsonArray(array)
    }

    /**
     * Parse a `FollowingPage` REST response.
     * The response is a SATS product type: `[[followingItem0, ...], totalCount, [0, cursor] | [1, []]]`.
     */
    private fun parseFollowingPage(responseBody: String): SpacetimeFollowingPage {
        val array = parseResponseArray(responseBody)
        return SpacetimeFollowingPage.fromJsonArray(array)
    }

    /**
     * Parse a `Vec<UserProfileDetailsV7>` REST response.
     * The response is a SATS array of product arrays: `[[profile0], [profile1], ...]`.
     */
    private fun parseUserProfileList(responseBody: String): List<SpacetimeUserProfileV7> {
        val array = parseResponseArray(responseBody)
        return array.map { SpacetimeUserProfileV7.fromJsonArray(it as JsonArray) }
    }

    /**
     * Parse the raw response body as a JSON array, throwing a clear error if not.
     */
    private fun parseResponseArray(responseBody: String): JsonArray =
        json.parseToJsonElement(responseBody) as? JsonArray
            ?: throw parseError("expected JSON array", responseBody)

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
