package com.yral.shared.core.logging

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Logger.Companion.config
import co.touchlab.kermit.Logger.Companion.tag
import co.touchlab.kermit.StaticConfig

class YralLogger(
    logWriters: List<LogWriter> = emptyList(),
) : Logger(
        tag = "YralApp",
        config =
            StaticConfig(
                logWriterList = logWriters,
            ),
    ) {
    fun withAdditionalLogWriter(writer: LogWriter): Logger =
        Logger(
            tag = tag,
            config =
                StaticConfig(
                    minSeverity = config.minSeverity,
                    logWriterList = config.logWriterList + writer,
                ),
        )
}
