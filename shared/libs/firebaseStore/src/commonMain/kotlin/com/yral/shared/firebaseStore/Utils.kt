package com.yral.shared.firebaseStore

import co.touchlab.kermit.Logger
import com.yral.shared.core.AppConfigurations.FIREBASE_COULD_FUN_REGION
import com.yral.shared.core.AppConfigurations.FIREBASE_COULD_URL
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.app
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

fun cloudFunctionUrl(): String = "$FIREBASE_COULD_FUN_REGION-${Firebase.app.options.projectId}.$FIREBASE_COULD_URL"
