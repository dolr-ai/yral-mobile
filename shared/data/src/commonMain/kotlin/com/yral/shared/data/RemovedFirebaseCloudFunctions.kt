package com.yral.shared.data

import com.yral.shared.core.exceptions.YralException

private const val REMOVED_FIREBASE_CLOUD_FUNCTIONS_MESSAGE =
    "Firebase Cloud Functions dependency has been removed from the project"

fun removedFirebaseCloudFunctionsException(operation: String): YralException =
    YralException("$operation is unavailable because $REMOVED_FIREBASE_CLOUD_FUNCTIONS_MESSAGE")
