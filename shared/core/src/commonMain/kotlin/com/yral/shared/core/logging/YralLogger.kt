package com.yral.shared.core.logging

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.StaticConfig

class YralLogger(
    logWriters: List<LogWriter> = emptyList(),
) : Logger(
        tag = "YralApp",
        config =
            StaticConfig(
                logWriterList = logWriters,
            ),
    )
