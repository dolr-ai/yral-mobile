package com.yral.shared.features.tournament.cache

import com.yral.shared.features.tournament.domain.model.HotOrNotVoteResult
import com.yral.shared.features.tournament.domain.model.VideoEmoji
import com.yral.shared.features.tournament.domain.model.VoteOutcome
import com.yral.shared.features.tournament.domain.model.VoteResult
import com.yral.shared.features.tournament.domain.model.VotedSmiley
import com.yral.shared.preferences.Preferences
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

interface TournamentResumeCacheStore {
    suspend fun saveProgress(
        scopeKey: String,
        tournamentId: String,
        progress: TournamentProgressData,
    )

    suspend fun loadProgress(
        scopeKey: String,
        tournamentId: String,
        nowEpochMs: Long,
    ): TournamentProgressData?

    suspend fun savePage(
        scopeKey: String,
        tournamentId: String,
        endEpochMs: Long,
        pageIndex: Int,
    )

    suspend fun loadPage(
        scopeKey: String,
        tournamentId: String,
        nowEpochMs: Long,
    ): Int?

    suspend fun clearTournament(
        scopeKey: String,
        tournamentId: String,
    )

    suspend fun clearExpired(nowEpochMs: Long)
}

data class TournamentProgressData(
    val endEpochMs: Long,
    val diamonds: Int,
    val position: Int,
    val activeParticipantCount: Int,
    val wins: Int,
    val losses: Int,
    val voteResults: Map<String, VoteResult>,
    val hotOrNotVoteResults: Map<String, HotOrNotVoteResult>,
    val shownCoinDeltaAnimations: Set<String>,
)

class TournamentResumeCacheStoreImpl(
    private val preferences: Preferences,
    private val json: Json,
) : TournamentResumeCacheStore {
    override suspend fun saveProgress(
        scopeKey: String,
        tournamentId: String,
        progress: TournamentProgressData,
    ) {
        val snapshot = progress.toSnapshot()
        preferences.putString(progressKey(scopeKey, tournamentId), json.encodeToString(snapshot))
        updateIndex(scopeKey, tournamentId, progress.endEpochMs)
    }

    @Suppress("ReturnCount")
    override suspend fun loadProgress(
        scopeKey: String,
        tournamentId: String,
        nowEpochMs: Long,
    ): TournamentProgressData? {
        val key = progressKey(scopeKey, tournamentId)
        val raw = preferences.getString(key) ?: return null
        val snapshot =
            runCatching {
                json.decodeFromString<TournamentProgressSnapshot>(raw)
            }.getOrNull()
                ?: run {
                    preferences.remove(key)
                    return null
                }

        if (snapshot.endEpochMs <= nowEpochMs) {
            clearTournament(scopeKey, tournamentId)
            return null
        }
        return snapshot.toDomain()
    }

    override suspend fun savePage(
        scopeKey: String,
        tournamentId: String,
        endEpochMs: Long,
        pageIndex: Int,
    ) {
        val snapshot =
            TournamentPageSnapshot(
                endEpochMs = endEpochMs,
                pageIndex = pageIndex.coerceAtLeast(0),
            )
        preferences.putString(pageKey(scopeKey, tournamentId), json.encodeToString(snapshot))
        updateIndex(scopeKey, tournamentId, endEpochMs)
    }

    @Suppress("ReturnCount")
    override suspend fun loadPage(
        scopeKey: String,
        tournamentId: String,
        nowEpochMs: Long,
    ): Int? {
        val key = pageKey(scopeKey, tournamentId)
        val raw = preferences.getString(key) ?: return null
        val snapshot =
            runCatching {
                json.decodeFromString<TournamentPageSnapshot>(raw)
            }.getOrNull()
                ?: run {
                    preferences.remove(key)
                    return null
                }
        if (snapshot.endEpochMs <= nowEpochMs) {
            clearTournament(scopeKey, tournamentId)
            return null
        }
        return snapshot.pageIndex.coerceAtLeast(0)
    }

    override suspend fun clearTournament(
        scopeKey: String,
        tournamentId: String,
    ) {
        preferences.remove(progressKey(scopeKey, tournamentId))
        preferences.remove(pageKey(scopeKey, tournamentId))
        val index = loadIndex()
        val filtered =
            index.entries.filterNot {
                it.scopeKey == scopeKey && it.tournamentId == tournamentId
            }
        saveIndex(TournamentCacheIndex(entries = filtered))
    }

    override suspend fun clearExpired(nowEpochMs: Long) {
        val index = loadIndex()
        if (index.entries.isEmpty()) return

        val expired = index.entries.filter { it.endEpochMs <= nowEpochMs }
        if (expired.isEmpty()) return

        expired.forEach { entry ->
            preferences.remove(progressKey(entry.scopeKey, entry.tournamentId))
            preferences.remove(pageKey(entry.scopeKey, entry.tournamentId))
        }
        val remaining = index.entries.filterNot { entry -> entry.endEpochMs <= nowEpochMs }
        saveIndex(TournamentCacheIndex(entries = remaining))
    }

    private suspend fun updateIndex(
        scopeKey: String,
        tournamentId: String,
        endEpochMs: Long,
    ) {
        val index = loadIndex()
        val updatedEntries =
            index.entries
                .filterNot { it.scopeKey == scopeKey && it.tournamentId == tournamentId }
                .plus(
                    TournamentCacheIndexEntry(
                        scopeKey = scopeKey,
                        tournamentId = tournamentId,
                        endEpochMs = endEpochMs,
                    ),
                )
        saveIndex(TournamentCacheIndex(entries = updatedEntries))
    }

    private suspend fun loadIndex(): TournamentCacheIndex {
        val raw = preferences.getString(INDEX_KEY) ?: return TournamentCacheIndex()
        return runCatching { json.decodeFromString<TournamentCacheIndex>(raw) }
            .getOrElse { TournamentCacheIndex() }
    }

    private suspend fun saveIndex(index: TournamentCacheIndex) {
        if (index.entries.isEmpty()) {
            preferences.remove(INDEX_KEY)
            return
        }
        preferences.putString(INDEX_KEY, json.encodeToString(index))
    }

    private fun progressKey(
        scopeKey: String,
        tournamentId: String,
    ): String = "$PROGRESS_KEY_PREFIX${scopeKey}_$tournamentId"

    private fun pageKey(
        scopeKey: String,
        tournamentId: String,
    ): String = "$PAGE_KEY_PREFIX${scopeKey}_$tournamentId"

    companion object {
        private const val PROGRESS_KEY_PREFIX = "tournament_progress_v1_"
        private const val PAGE_KEY_PREFIX = "tournament_page_v1_"
        private const val INDEX_KEY = "tournament_progress_index_v1"
    }
}

@Serializable
private data class TournamentCacheIndex(
    val entries: List<TournamentCacheIndexEntry> = emptyList(),
)

@Serializable
private data class TournamentCacheIndexEntry(
    val scopeKey: String,
    val tournamentId: String,
    val endEpochMs: Long,
)

@Serializable
private data class TournamentPageSnapshot(
    val endEpochMs: Long,
    val pageIndex: Int,
)

@Serializable
private data class TournamentProgressSnapshot(
    val endEpochMs: Long,
    val diamonds: Int,
    val position: Int,
    val activeParticipantCount: Int,
    val wins: Int,
    val losses: Int,
    val voteResults: List<VoteResultSnapshot> = emptyList(),
    val hotOrNotVoteResults: List<HotOrNotVoteResultSnapshot> = emptyList(),
    val shownCoinDeltaAnimations: List<String> = emptyList(),
)

@Serializable
private data class VoteResultSnapshot(
    val videoId: String,
    val outcome: String,
    val smiley: VotedSmileySnapshot,
    val tournamentWins: Int,
    val tournamentLosses: Int,
    val diamonds: Int,
    val position: Int,
    val diamondDelta: Int? = null,
    val videoEmojis: List<VideoEmojiSnapshot>? = null,
    val activeParticipantCount: Int = 0,
)

@Serializable
private data class VotedSmileySnapshot(
    val id: String,
    val unicode: String? = null,
    val displayName: String? = null,
    val imageUrl: String? = null,
    val isActive: Boolean? = null,
    val clickAnimation: String? = null,
    val imageFallback: String? = null,
)

@Serializable
private data class VideoEmojiSnapshot(
    val id: String,
    val unicode: String,
    val displayName: String,
)

@Serializable
private data class HotOrNotVoteResultSnapshot(
    val videoId: String,
    val outcome: String,
    val vote: String,
    val aiVerdict: String,
    val diamonds: Int,
    val diamondDelta: Int,
    val wins: Int = 0,
    val losses: Int = 0,
    val position: Int = 0,
    val activeParticipantCount: Int = 0,
)

private fun TournamentProgressData.toSnapshot(): TournamentProgressSnapshot =
    TournamentProgressSnapshot(
        endEpochMs = endEpochMs,
        diamonds = diamonds,
        position = position,
        activeParticipantCount = activeParticipantCount,
        wins = wins,
        losses = losses,
        voteResults =
            voteResults.map { (videoId, result) ->
                VoteResultSnapshot(
                    videoId = videoId,
                    outcome = result.outcome.name,
                    smiley = result.smiley.toSnapshot(),
                    tournamentWins = result.tournamentWins,
                    tournamentLosses = result.tournamentLosses,
                    diamonds = result.diamonds,
                    position = result.position,
                    diamondDelta = result.diamondDelta,
                    videoEmojis = result.videoEmojis?.map { it.toSnapshot() },
                    activeParticipantCount = result.activeParticipantCount,
                )
            },
        hotOrNotVoteResults =
            hotOrNotVoteResults.map { (videoId, result) ->
                HotOrNotVoteResultSnapshot(
                    videoId = videoId,
                    outcome = result.outcome,
                    vote = result.vote,
                    aiVerdict = result.aiVerdict,
                    diamonds = result.diamonds,
                    diamondDelta = result.diamondDelta,
                    wins = result.wins,
                    losses = result.losses,
                    position = result.position,
                    activeParticipantCount = result.activeParticipantCount,
                )
            },
        shownCoinDeltaAnimations = shownCoinDeltaAnimations.toList(),
    )

private fun TournamentProgressSnapshot.toDomain(): TournamentProgressData =
    TournamentProgressData(
        endEpochMs = endEpochMs,
        diamonds = diamonds,
        position = position,
        activeParticipantCount = activeParticipantCount,
        wins = wins,
        losses = losses,
        voteResults =
            voteResults.associate { snapshot ->
                snapshot.videoId to snapshot.toDomain()
            },
        hotOrNotVoteResults =
            hotOrNotVoteResults.associate { snapshot ->
                snapshot.videoId to snapshot.toDomain()
            },
        shownCoinDeltaAnimations = shownCoinDeltaAnimations.toSet(),
    )

private fun VoteResultSnapshot.toDomain(): VoteResult =
    VoteResult(
        outcome = VoteOutcome.fromString(outcome),
        smiley = smiley.toDomain(),
        tournamentWins = tournamentWins,
        tournamentLosses = tournamentLosses,
        diamonds = diamonds,
        position = position,
        diamondDelta = diamondDelta,
        videoEmojis = videoEmojis?.map { it.toDomain() },
        activeParticipantCount = activeParticipantCount,
    )

private fun VotedSmiley.toSnapshot(): VotedSmileySnapshot =
    VotedSmileySnapshot(
        id = id,
        unicode = unicode,
        displayName = displayName,
        imageUrl = imageUrl,
        isActive = isActive,
        clickAnimation = clickAnimation,
        imageFallback = imageFallback,
    )

private fun VotedSmileySnapshot.toDomain(): VotedSmiley =
    VotedSmiley(
        id = id,
        unicode = unicode,
        displayName = displayName,
        imageUrl = imageUrl,
        isActive = isActive,
        clickAnimation = clickAnimation,
        imageFallback = imageFallback,
    )

private fun VideoEmoji.toSnapshot(): VideoEmojiSnapshot =
    VideoEmojiSnapshot(
        id = id,
        unicode = unicode,
        displayName = displayName,
    )

private fun VideoEmojiSnapshot.toDomain(): VideoEmoji =
    VideoEmoji(
        id = id,
        unicode = unicode,
        displayName = displayName,
    )

private fun HotOrNotVoteResultSnapshot.toDomain(): HotOrNotVoteResult =
    HotOrNotVoteResult(
        outcome = outcome,
        vote = vote,
        aiVerdict = aiVerdict,
        diamonds = diamonds,
        diamondDelta = diamondDelta,
        wins = wins,
        losses = losses,
        position = position,
        activeParticipantCount = activeParticipantCount,
    )
