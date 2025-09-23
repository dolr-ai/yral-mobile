package com.yral.shared.reportVideo.di

import com.yral.shared.reportVideo.data.IReportVideoDataSource
import com.yral.shared.reportVideo.data.ReportVideoRemoteDataSource
import com.yral.shared.reportVideo.data.ReportVideoRepository
import com.yral.shared.reportVideo.domain.IReportVideoRepository
import com.yral.shared.reportVideo.domain.ReportVideoUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val reportVideoModule =
    module {
        factory<IReportVideoRepository> { ReportVideoRepository(get()) }
        factory<IReportVideoDataSource> { ReportVideoRemoteDataSource(get()) }
        factoryOf(::ReportVideoUseCase)
    }
