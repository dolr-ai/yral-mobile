package com.yral.shared.core.logging

import co.touchlab.kermit.DefaultFormatter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Message
import co.touchlab.kermit.MessageStringFormatter
import co.touchlab.kermit.Severity
import co.touchlab.kermit.Tag
import co.touchlab.kermit.loggerConfigInit
import co.touchlab.kermit.platformLogWriter
import com.yral.shared.core.AppConfig

class YralLogger(
    appConfig: AppConfig,
) : Logger(
        tag = "YralApp",
        config =
            loggerConfigInit(
                platformLogWriter(if (appConfig.isDebug) DefaultFormatter else NoOpFormatter()),
            ),
    )

internal class NoOpFormatter : MessageStringFormatter {
    override fun formatMessage(
        severity: Severity?,
        tag: Tag?,
        message: Message,
    ): String = ""
}
