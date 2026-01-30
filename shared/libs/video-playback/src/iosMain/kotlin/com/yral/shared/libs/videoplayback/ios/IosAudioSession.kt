package com.yral.shared.libs.videoplayback.ios

import co.touchlab.kermit.Logger
import com.yral.shared.core.utils.runOnMainSync
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVAudioSessionModeDefault
import platform.AVFAudio.setActive
import platform.Foundation.NSError
import platform.Foundation.NSLock

internal object IosAudioSession {
    private val logger = Logger.withTag("IosAudioSession")
    private val lock = NSLock()
    private var configured = false

    fun ensurePlaybackSessionActive() {
        if (configured) return
        lock.lock()
        try {
            if (configured) return
            val success = runOnMainSync { configurePlaybackSession() }
            if (success) {
                configured = true
            }
        } finally {
            lock.unlock()
        }
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun configurePlaybackSession(): Boolean =
        memScoped {
            val session = AVAudioSession.sharedInstance()
            val errorPtr = alloc<ObjCObjectVar<NSError?>>()

            errorPtr.value = null
            if (!session.setCategory(AVAudioSessionCategoryPlayback, error = errorPtr.ptr)) {
                logFailure("setCategory", errorPtr.value)
                return false
            }

            errorPtr.value = null
            if (!session.setMode(AVAudioSessionModeDefault, error = errorPtr.ptr)) {
                logFailure("setMode", errorPtr.value)
                return false
            }

            errorPtr.value = null
            if (!session.setActive(true, error = errorPtr.ptr)) {
                logFailure("setActive", errorPtr.value)
                return false
            }

            logger.d { "Configured AVAudioSession for playback" }
            true
        }

    private fun logFailure(
        stage: String,
        error: NSError?,
    ) {
        val reason = error?.localizedDescription ?: "unknown error"
        logger.e { "Audio session $stage failed: $reason" }
    }
}
