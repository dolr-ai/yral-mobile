package com.yral.shared.rust.service.domain.pagedDataSource

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.yral.shared.rust.service.domain.UserInfoRepository
import com.yral.shared.rust.service.domain.models.PagedFollowerItem
import com.yral.shared.uniffi.generated.Principal

class FollowersPagingSource(
    private val profileRepository: UserInfoRepository,
    private val principal: Principal,
    private val targetPrincipal: Principal,
    private val withCallerFollows: Boolean? = null,
) : PagingSource<String, PagedFollowerItem>() {
    override suspend fun load(params: LoadParams<String>): LoadResult<String, PagedFollowerItem> =
        runCatching {
            val limit = params.loadSize.toULong()
            val cursorPrincipal: Principal? = params.key

            val result =
                profileRepository.getFollowers(
                    principal = principal,
                    targetPrincipal = targetPrincipal,
                    cursorPrincipal = cursorPrincipal,
                    limit = limit,
                    withCallerFollows = withCallerFollows,
                )

            LoadResult.Page(
                data =
                    listOf(
                        PagedFollowerItem(
                            items = result.followers,
                            totalCount = result.totalCount,
                        ),
                    ),
                prevKey = null,
                nextKey = result.nextCursor,
            )
        }.getOrElse { LoadResult.Error(it) }

    override fun getRefreshKey(state: PagingState<String, PagedFollowerItem>): String? = null
}
