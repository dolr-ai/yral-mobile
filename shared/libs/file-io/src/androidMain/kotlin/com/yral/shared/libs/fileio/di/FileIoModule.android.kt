package com.yral.shared.libs.fileio.di

import android.content.Context
import com.yral.shared.libs.fileio.AndroidFileDownloader
import com.yral.shared.libs.fileio.FileDownloader
import org.koin.mp.KoinPlatform.getKoin

actual fun platformFileDownloader(): FileDownloader {
    val context = getKoin().get<Context>()
    return AndroidFileDownloader(context)
}
