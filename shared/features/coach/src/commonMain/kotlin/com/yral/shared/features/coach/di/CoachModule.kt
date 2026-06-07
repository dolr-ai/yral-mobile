package com.yral.shared.features.coach.di

import com.yral.shared.core.di.COACH_SERVER_BASE_URL
import com.yral.shared.features.coach.data.CoachDataSource
import com.yral.shared.features.coach.data.CoachRemoteDataSource
import com.yral.shared.features.coach.data.CoachRepositoryImpl
import com.yral.shared.features.coach.domain.CoachRepository
import com.yral.shared.features.coach.domain.usecases.ApplyCoachProposalUseCase
import com.yral.shared.features.coach.domain.usecases.CreateCoachSessionUseCase
import com.yral.shared.features.coach.domain.usecases.ListCoachMessagesUseCase
import com.yral.shared.features.coach.domain.usecases.SendCoachMessageUseCase
import com.yral.shared.features.coach.viewmodel.CoachViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val coachModule =
    module {
        single<CoachDataSource> {
            CoachRemoteDataSource(
                httpClient = get(),
                json = get(),
                preferences = get(),
                chatBaseUrl = get(COACH_SERVER_BASE_URL),
            )
        }
        factoryOf(::CoachRepositoryImpl) bind CoachRepository::class

        factoryOf(::CreateCoachSessionUseCase)
        factoryOf(::SendCoachMessageUseCase)
        factoryOf(::ApplyCoachProposalUseCase)
        factoryOf(::ListCoachMessagesUseCase)

        viewModelOf(::CoachViewModel)
    }
