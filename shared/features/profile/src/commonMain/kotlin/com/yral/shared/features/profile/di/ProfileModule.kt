package com.yral.shared.features.profile.di

import com.yral.shared.features.profile.analytics.ProfileTelemetry
import com.yral.shared.features.profile.data.FollowersMetadataDataSourceImpl
import com.yral.shared.features.profile.data.ProfileDataSource
import com.yral.shared.features.profile.data.ProfileDataSourceImpl
import com.yral.shared.features.profile.data.ProfileRepositoryImpl
import com.yral.shared.features.profile.domain.DeleteVideoUseCase
import com.yral.shared.features.profile.domain.FollowNotificationUseCase
import com.yral.shared.features.profile.domain.UploadProfileImageUseCase
import com.yral.shared.features.profile.domain.repository.ProfileRepository
import com.yral.shared.features.profile.viewmodel.EditProfileViewModel
import com.yral.shared.features.profile.viewmodel.ProfileViewModel
import com.yral.shared.rust.service.domain.metadata.FollowersMetadataDataSource
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val profileModule =
    module {
        factoryOf(::FollowersMetadataDataSourceImpl).bind<FollowersMetadataDataSource>()
        factoryOf(::ProfileDataSourceImpl).bind<ProfileDataSource>()
        factoryOf(::ProfileRepositoryImpl).bind<ProfileRepository>()
        factoryOf(::DeleteVideoUseCase)
        factoryOf(::UploadProfileImageUseCase)
        factoryOf(::FollowNotificationUseCase)
        factoryOf(::ProfileTelemetry)
        viewModelOf(::ProfileViewModel)
        viewModelOf(::EditProfileViewModel)
    }
