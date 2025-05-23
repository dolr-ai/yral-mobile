package com.yral.shared.core.logging

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.StaticConfig

class YralLogger(
    writer: LogWriter?,
) : Logger(
        tag = "YralApp",
        config =
            StaticConfig(
                logWriterList = writer?.let { listOf(writer) } ?: emptyList(),
            ),
    )
