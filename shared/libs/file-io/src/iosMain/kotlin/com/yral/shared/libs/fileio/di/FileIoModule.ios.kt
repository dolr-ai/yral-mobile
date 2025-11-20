package com.yral.shared.libs.fileio.di

import com.yral.shared.libs.fileio.FileDownloader
import com.yral.shared.libs.fileio.IosFileDownloader

actual fun platformFileDownloader(): FileDownloader = IosFileDownloader()
