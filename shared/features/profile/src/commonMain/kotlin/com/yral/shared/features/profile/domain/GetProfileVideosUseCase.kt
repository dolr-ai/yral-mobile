package com.yral.shared.features.profile.domain

import com.yral.shared.core.exceptions.YralException
import com.yral.shared.core.session.SessionManager
import com.yral.shared.features.profile.domain.models.ProfileVideosPageResult
import com.yral.shared.libs.arch.domain.SuspendUseCase
import com.yral.shared.libs.arch.domain.UseCaseFailureListener
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import com.yral.shared.rust.services.IndividualUserServiceFactory
import com.yral.shared.uniffi.generated.GetPostsOfUserProfileError
import com.yral.shared.uniffi.generated.Result12

class GetProfileVideosUseCase(
    private val sessionManager: SessionManager,
    private val individualUserServiceFactory: IndividualUserServiceFactory,
    appDispatchers: AppDispatchers,
    failureListener: UseCaseFailureListener,
) : SuspendUseCase<GetProfileVideosUseCase.Params, ProfileVideosPageResult>(
        coroutineDispatcher = appDispatchers.network,
        failureListener = failureListener,
    ) {
    data class Params(
        val startIndex: ULong,
        val pageSize: ULong,
    )

    override suspend fun execute(parameter: Params): ProfileVideosPageResult {
        val canisterId =
            sessionManager.getCanisterPrincipal()
                ?: throw YralException("User not authenticated")

        val service = individualUserServiceFactory.service(canisterId)

        val result =
            service.getPostsOfThisUserProfileWithPaginationCursor(
                parameter.startIndex,
                parameter.pageSize,
            )

        return when (result) {
            is Result12.Ok -> {
                val posts = result.v1
                ProfileVideosPageResult(
                    posts = posts,
                    hasNextPage = posts.size == parameter.pageSize.toInt(),
                    nextStartIndex = parameter.startIndex + parameter.pageSize,
                )
            }

            is Result12.Err -> {
                when (result.v1) {
                    GetPostsOfUserProfileError.REACHED_END_OF_ITEMS_LIST -> {
                        ProfileVideosPageResult(
                            posts = emptyList(),
                            hasNextPage = false,
                            nextStartIndex = parameter.startIndex,
                        )
                    }

                    GetPostsOfUserProfileError.INVALID_BOUNDS_PASSED -> {
                        throw YralException("Invalid bounds passed for pagination")
                    }

                    GetPostsOfUserProfileError.EXCEEDED_MAX_NUMBER_OF_ITEMS_ALLOWED_IN_ONE_REQUEST -> {
                        throw YralException("Exceeded max number of items allowed in one request")
                    }
                }
            }
        }
    }
}
