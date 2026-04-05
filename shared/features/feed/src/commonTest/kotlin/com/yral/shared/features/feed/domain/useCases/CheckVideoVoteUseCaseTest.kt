@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.yral.shared.features.feed.domain.useCases

import com.github.michaelbull.result.Ok
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import com.yral.shared.testsupport.usecase.NoOpUseCaseFailureListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CheckVideoVoteUseCaseTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun returnsFalseWithoutFirebaseLookup() =
        runTest {
            val useCase =
                CheckVideoVoteUseCase(
                    dispatchers = AppDispatchers(),
                    useCaseFailureListener = NoOpUseCaseFailureListener(),
                )

            val result =
                useCase(
                    CheckVideoVoteUseCase.Params(
                        videoId = "video-1",
                        principalId = "principal-1",
                    ),
                )

            assertEquals(Ok(false), result)
        }
}
