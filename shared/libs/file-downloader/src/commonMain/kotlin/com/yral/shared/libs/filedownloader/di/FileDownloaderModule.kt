package com.yral.shared.libs.filedownloader.di

import com.yral.shared.libs.filedownloader.FileDownloader
import org.koin.dsl.module

expect fun platformFileDownloader(): FileDownloader

val fileDownloaderModule =
    module {
        single<FileDownloader> { platformFileDownloader() }
    }
