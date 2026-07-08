package com.yral.shared.features.chat.domain

import com.yral.shared.http.exception.NetworkException
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode

/**
 * HTTP status of a failed chat API call, if the failure was an HTTP error.
 * [NetworkException] wraps the Ktor [ResponseException] as its cause
 * (NetworkHelper.handleException); anything else — timeouts, decode
 * failures, missing auth — has no status and returns null.
 */
internal fun Throwable.httpStatusOrNull(): HttpStatusCode? {
    val responseException = (this as? NetworkException)?.cause as? ResponseException
    return responseException?.response?.status
}
