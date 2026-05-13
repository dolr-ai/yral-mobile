@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.yral.shared.crashlytics.core

import cocoapods.FirebaseCrashlytics.FIRCrashlytics
import cocoapods.FirebaseCrashlytics.FIRExceptionModel
import cocoapods.FirebaseCrashlytics.FIRStackFrame

private const val MAX_CAUSE_DEPTH = 5
private const val MAX_FRAMES_PER_THROWABLE = 100
private val FRAME_TAIL_REGEX = Regex("""\(([^()]+?):(\d+)(?::\d+)?\)\s*$""")

private class NativeFirebaseCrashlyticsProvider(
    private val crashlytics: FIRCrashlytics,
) : CrashlyticsProvider {
    override val name: String
        get() = "firebase_native_ios"

    override fun recordException(exception: Exception) {
        recordException(
            exception = exception,
            type = ExceptionType.UNKNOWN,
        )
    }

    override fun recordException(
        exception: Exception,
        type: ExceptionType,
    ) {
        crashlytics.setCustomValue(type.name.lowercase(), "error_type")
        crashlytics.recordExceptionModel(exception.asExceptionModel())
    }

    override fun logMessage(message: String) {
        crashlytics.log(message)
    }

    override fun setUserId(id: String) {
        crashlytics.setUserID(id)
    }
}

internal actual fun createCrashlyticsProvider(): CrashlyticsProvider =
    NativeFirebaseCrashlyticsProvider(
        crashlytics = FIRCrashlytics.crashlytics(),
    )

private fun Throwable.asExceptionModel(): FIRExceptionModel {
    val rootName = primaryThrowable()::class.simpleName ?: "KotlinException"
    val reason = buildReason()
    val model = FIRExceptionModel(name = rootName, reason = reason)
    model.setStackTrace(collectStackFrames())
    return model
}

private fun Throwable.primaryThrowable(): Throwable {
    // AppUseCaseFailureListener wraps the original as Exception(throwable). Unwrap the
    // synthetic wrapper so the Crashlytics issue title reflects the real cause class.
    val unwrap = cause
    return if (this::class.simpleName == "Exception" && unwrap != null) unwrap else this
}

private fun Throwable.buildReason(): String {
    val pieces = mutableListOf<String>()
    var current: Throwable? = this
    var depth = 0
    val seen = mutableSetOf<Throwable>()
    while (current != null && current !in seen && depth < MAX_CAUSE_DEPTH) {
        seen += current
        val name = current::class.simpleName ?: "Throwable"
        val msg = current.message?.takeIf { it.isNotBlank() } ?: current.toString()
        pieces += if (depth == 0) "$name: $msg" else "Caused by $name: $msg"
        current = current.cause
        depth++
    }
    return pieces.joinToString(separator = "\n")
}

private fun Throwable.collectStackFrames(): List<FIRStackFrame> {
    val frames = mutableListOf<FIRStackFrame>()
    var current: Throwable? = this
    var depth = 0
    val seen = mutableSetOf<Throwable>()
    while (current != null && current !in seen && depth < MAX_CAUSE_DEPTH) {
        seen += current
        if (depth > 0) {
            val name = current::class.simpleName ?: "Throwable"
            val msg = current.message?.takeIf { it.isNotBlank() } ?: ""
            frames += FIRStackFrame(symbol = "Caused by $name: $msg", file = "", line = 0)
        }
        current
            .stackTraceToString()
            .lineSequence()
            .map { it.trim() }
            .filter { it.startsWith("at ") }
            .take(MAX_FRAMES_PER_THROWABLE)
            .forEach { frames += parseFrame(it) }
        current = current.cause
        depth++
    }
    return frames
}

private fun parseFrame(rawLine: String): FIRStackFrame {
    val line = rawLine.removePrefix("at ").trim()
    val match = FRAME_TAIL_REGEX.find(line)
    return if (match != null) {
        val file = match.groupValues[1]
        val lineNo = match.groupValues[2].toLongOrNull() ?: 0L
        val symbol = line.substring(0, match.range.first).trim().ifEmpty { line }
        FIRStackFrame(symbol = symbol, file = file, line = lineNo)
    } else {
        FIRStackFrame(symbol = line, file = "", line = 0)
    }
}
