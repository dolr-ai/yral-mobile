package com.yral.shared.features.profile.domain

import androidx.paging.PagingSource
import androidx.paging.PagingState
import co.touchlab.kermit.Logger
import com.yral.shared.data.domain.CommonApis
import com.yral.shared.data.domain.models.FeedDetails
import com.yral.shared.features.profile.domain.repository.ProfileRepository

class ProfileVideosPagingSource(
    private val canisterId: String,
    private val userPrincipal: String,
    private val isFromServiceCanister: Boolean,
    private val profileRepository: ProfileRepository,
    private val commonApis: CommonApis,
    private val isOwnProfile: Boolean = false,
) : PagingSource<ULong, FeedDetails>() {
    override suspend fun load(params: LoadParams<ULong>): LoadResult<ULong, FeedDetails> =
        runCatching {
            val startIndex = params.key ?: 0UL
            val pageSize = params.loadSize.toULong()

            // On first page for own profile, prepend draft videos
            val draftVideos =
                if (startIndex == 0UL && isOwnProfile) {
                    fetchDraftVideos()
                } else {
                    emptyList()
                }

            val result =
                profileRepository
                    .getProfileVideos(
                        canisterId = canisterId,
                        userPrincipal = userPrincipal,
                        isFromServiceCanister = isFromServiceCanister,
                        startIndex = startIndex,
                        pageSize = pageSize,
                    )
            var profileVideos = result.posts

            val allVideos = draftVideos + profileVideos
            val videoStats =
                commonApis
                    .getVideoViewsCount(allVideos.map { it.videoID })
                    .associate { it.videoId to it.allViews }
            if (videoStats.isNotEmpty()) {
                profileVideos =
                    profileVideos.map {
                        it.copy(
                            viewCount = videoStats[it.videoID] ?: it.viewCount,
                            bulkViewCount = videoStats[it.videoID] ?: it.viewCount,
                        )
                    }
            }
            LoadResult.Page(
                data = draftVideos + profileVideos,
                prevKey = null,
                nextKey = if (result.hasNextPage) result.nextStartIndex else null,
            )
        }.getOrElse {
            Logger.e("ProfileVideosPaging", it)
            LoadResult.Error(it)
        }

    private suspend fun fetchDraftVideos(): List<FeedDetails> =
        runCatching {
            profileRepository
                .getDraftVideos(
                    canisterId = canisterId,
                    startIndex = 0UL,
                    pageSize = DRAFT_PAGE_SIZE,
                ).posts
        }.getOrElse {
            Logger.e("ProfileVideosPaging", it) { "Error fetching draft videos" }
            emptyList()
        }

    override fun getRefreshKey(state: PagingState<ULong, FeedDetails>): ULong? = null

    private companion object {
        const val DRAFT_PAGE_SIZE = 100UL
    }
}
