package com.yral.shared.features.profile.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.github.michaelbull.result.getOrElse
import com.yral.shared.core.session.SessionManager
import com.yral.shared.features.profile.domain.GetProfileVideosUseCase
import com.yral.shared.features.profile.viewmodel.ProfileVideo
import com.yral.shared.rust.data.models.toFeedDetails

class ProfileVideosPagingSource(
    private val getProfileVideosUseCase: GetProfileVideosUseCase,
    private val sessionManager: SessionManager,
    private val pageSize: Int = 20,
) : PagingSource<ULong, ProfileVideo>() {
    override suspend fun load(params: LoadParams<ULong>): LoadResult<ULong, ProfileVideo> =
        runCatching {
            val startIndex = params.key ?: 0UL
            val loadSize = params.loadSize.coerceAtLeast(pageSize)

            val result =
                getProfileVideosUseCase(
                    GetProfileVideosUseCase.Params(
                        startIndex = startIndex,
                        pageSize = loadSize.toULong(),
                    ),
                ).getOrElse { error ->
                    return LoadResult.Error(error)
                }

            val canisterId = sessionManager.getCanisterPrincipal() ?: ""
            val profileVideos =
                result.posts.mapNotNull { post ->
                    runCatching {
                        val feedDetails =
                            post.toFeedDetails(
                                postId = post.id.toLong(),
                                canisterId = canisterId,
                                nsfwProbability = 0.0, // Default value, can be updated if needed
                            )
                        ProfileVideo(
                            feedDetail = feedDetails,
                            isDeleting = false,
                        )
                    }.getOrNull() // Return null for failed conversions, skip the post
                }

            LoadResult.Page(
                data = profileVideos,
                prevKey = if (startIndex == 0UL) null else maxOf(0UL, startIndex - pageSize.toULong()),
                nextKey = if (result.hasNextPage) result.nextStartIndex else null,
            )
        }.fold(
            onSuccess = { it },
            onFailure = { exception -> LoadResult.Error(exception) },
        )

    override fun getRefreshKey(state: PagingState<ULong, ProfileVideo>): ULong? =
        state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(pageSize.toULong())
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(pageSize.toULong())
        }
}
