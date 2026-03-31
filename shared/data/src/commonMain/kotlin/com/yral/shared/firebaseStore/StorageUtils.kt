package com.yral.shared.firebaseStore

import co.touchlab.kermit.Logger
import dev.gitlive.firebase.storage.FirebaseStorage

suspend fun getDownloadUrl(
    path: String,
    storage: FirebaseStorage,
): String =
    if (path.isEmpty()) {
        path
    } else {
        runCatching {
            storage.reference(path).getDownloadUrl()
        }.getOrElse {
            "".also { Logger.d("Failed to get Download path $path") }
        }
    }
