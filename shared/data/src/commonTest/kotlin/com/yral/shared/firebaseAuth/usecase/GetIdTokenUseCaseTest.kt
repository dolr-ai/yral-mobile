@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.yral.shared.firebaseAuth.usecase

import com.github.michaelbull.result.Ok
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.testsupport.preferences.FakePreferences
import com.yral.shared.testsupport.usecase.NoOpUseCaseFailureListener
import com.yral.shared.testsupport.usecase.RecordingUseCaseFailureListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetIdTokenUseCaseTest {
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
    fun returnsStoredIdToken() =
        runTest {
            val preferences = FakePreferences()
            preferences.putString(PrefKeys.ID_TOKEN.name, "token-123")
            val useCase =
                GetIdTokenUseCase(
                    appDispatchers = AppDispatchers(),
                    useCaseFailureListener = NoOpUseCaseFailureListener(),
                    preferences = preferences,
                )

            val result = useCase(GetIdTokenUseCase.DEFAULT)

            assertEquals(Ok("token-123"), result)
        }

    @Test
    fun returnsFailureWhenIdTokenMissing() =
        runTest {
            val failureListener = RecordingUseCaseFailureListener()
            val useCase =
                GetIdTokenUseCase(
                    appDispatchers = AppDispatchers(),
                    useCaseFailureListener = failureListener,
                    preferences = FakePreferences(),
                )

            useCase(GetIdTokenUseCase.DEFAULT)

            assertEquals("ID token is missing", failureListener.lastFailure?.message)
        }
}
