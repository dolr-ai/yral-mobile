package com.yral.shared.libs.filedownloader.di

import com.yral.shared.libs.filedownloader.FileDownloader
import com.yral.shared.libs.filedownloader.IosFileDownloader

actual fun platformFileDownloader(): FileDownloader = IosFileDownloader()
