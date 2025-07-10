package com.yral.android.ui.widgets.video.di

import com.yral.android.ui.widgets.video.VideoFileManager
import com.yral.android.ui.widgets.video.VideoMetadataExtractor
import com.yral.android.ui.widgets.video.VideoPlayerUtils
import com.yral.android.ui.widgets.video.VideoValidator
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val videoWidgetModule =
    module {
        factoryOf(::VideoMetadataExtractor)
        factoryOf(::VideoFileManager)
        factoryOf(::VideoValidator)
        factoryOf(::VideoPlayerUtils)
    }
