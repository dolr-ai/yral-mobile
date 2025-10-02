package com.yral.shared.features.uploadvideo.utils.di

import com.yral.shared.features.uploadvideo.utils.VideoFileManager
import com.yral.shared.features.uploadvideo.utils.VideoMetadataExtractor
import com.yral.shared.features.uploadvideo.utils.VideoPlayerUtils
import com.yral.shared.features.uploadvideo.utils.VideoValidator
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val videoWidgetModule =
    module {
        factoryOf(::VideoMetadataExtractor)
        factoryOf(::VideoFileManager)
        factoryOf(::VideoValidator)
        factoryOf(::VideoPlayerUtils)
    }
