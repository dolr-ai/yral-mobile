package com.yral.shared.libs.fileio.di

import com.yral.shared.libs.fileio.FileDownloader
import org.koin.dsl.module

expect fun platformFileDownloader(): FileDownloader

val fileIoModule =
    module {
        single<FileDownloader> { platformFileDownloader() }
    }
