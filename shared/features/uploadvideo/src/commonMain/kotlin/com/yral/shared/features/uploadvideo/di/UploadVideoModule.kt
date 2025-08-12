package com.yral.shared.features.uploadvideo.di

import com.yral.shared.features.uploadvideo.analytics.UploadVideoTelemetry
import com.yral.shared.features.uploadvideo.data.UploadRepositoryImpl
import com.yral.shared.features.uploadvideo.data.remote.UploadVideoRemoteDataSource
import com.yral.shared.features.uploadvideo.domain.GenerateVideoUseCase
import com.yral.shared.features.uploadvideo.domain.GetFreeCreditsStatusUseCase
import com.yral.shared.features.uploadvideo.domain.GetProvidersUseCase
import com.yral.shared.features.uploadvideo.domain.GetUploadEndpointUseCase
import com.yral.shared.features.uploadvideo.domain.PollGenerationStatusUseCase
import com.yral.shared.features.uploadvideo.domain.UpdateMetaUseCase
import com.yral.shared.features.uploadvideo.domain.UploadRepository
import com.yral.shared.features.uploadvideo.domain.UploadVideoUseCase
import com.yral.shared.features.uploadvideo.presentation.UploadVideoViewModel
import com.yral.shared.features.uploadvideo.presentation.UploadVideoViewModel.RequiredUseCases
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val uploadVideoModule =
    module {
        factoryOf(::UploadVideoRemoteDataSource)
        factoryOf(::UploadRepositoryImpl) bind UploadRepository::class
        factoryOf(::GetUploadEndpointUseCase)
        factoryOf(::UploadVideoUseCase)
        factoryOf(::UpdateMetaUseCase)
        factoryOf(::GetProvidersUseCase)
        factoryOf(::GenerateVideoUseCase)
        factoryOf(::PollGenerationStatusUseCase)
        factoryOf(::GetFreeCreditsStatusUseCase)
        factoryOf(::UploadVideoTelemetry)
        factoryOf(::RequiredUseCases)
        viewModelOf(::UploadVideoViewModel)
    }
