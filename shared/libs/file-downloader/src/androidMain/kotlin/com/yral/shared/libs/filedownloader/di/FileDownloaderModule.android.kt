package com.yral.shared.libs.filedownloader.di

import android.content.Context
import com.yral.shared.libs.filedownloader.AndroidFileDownloader
import com.yral.shared.libs.filedownloader.FileDownloader
import org.koin.mp.KoinPlatform.getKoin

actual fun platformFileDownloader(): FileDownloader = AndroidFileDownloader(getKoin().get<Context>())
