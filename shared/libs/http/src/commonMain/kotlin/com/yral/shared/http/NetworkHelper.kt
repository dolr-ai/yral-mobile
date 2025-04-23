package com.yral.shared.http

import com.yral.shared.http.exception.NetworkException
import com.yral.shared.http.exception.UnknownException
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.RedirectResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

@Suppress("TooGenericExceptionCaught")
suspend inline fun <reified K> httpGet(
    httpClient: HttpClient,
    json: Json,
    block: HttpRequestBuilder.() -> Unit,
): K {
    try {
        val response: HttpResponse = httpClient.get(block)
        if (HTTPResponseStatus.from(response.status.value) == HTTPResponseStatus.SUCCESS) {
            val deserializer = json.serializersModule.serializer<K>()
            val apiResponseString = response.bodyAsText()
            val apiResponse =
                json.decodeFromString(
                    deserializer = deserializer,
                    string = apiResponseString,
                )
            return apiResponse
        }
    } catch (e: Exception) {
        return handleException(e)
    }
    throw UnknownException("Error making network call")
}

@Suppress("TooGenericExceptionCaught")
suspend inline fun <reified K> httpPost(
    httpClient: HttpClient,
    json: Json,
    block: HttpRequestBuilder.() -> Unit,
): K {
    try {
        val response: HttpResponse = httpClient.post(block)
        if (HTTPResponseStatus.from(response.status.value) == HTTPResponseStatus.SUCCESS) {
            val deserializer = json.serializersModule.serializer<K>()
            val apiResponseString = response.bodyAsText()
            val apiResponse =
                json.decodeFromString(
                    deserializer = deserializer,
                    string = apiResponseString,
                )
            return apiResponse
        }
    } catch (e: Exception) {
        return handleException(e)
    }
    throw UnknownException("Error making network call")
}

@Suppress("TooGenericExceptionCaught")
suspend inline fun httpPostWithBytesResponse(
    httpClient: HttpClient,
    block: HttpRequestBuilder.() -> Unit,
): ByteArray {
    try {
        val response: HttpResponse = httpClient.post(block)
        if (HTTPResponseStatus.from(response.status.value) == HTTPResponseStatus.SUCCESS) {
            return response.bodyAsBytes()
        }
    } catch (e: Exception) {
        return handleException(e)
    }
    throw UnknownException("Error making network call")
}

fun <K> handleException(exception: Exception): K =
    when (exception) {
        is RedirectResponseException,
        is ServerResponseException,
        is ClientRequestException,
        -> throw NetworkException(exception)

        else -> throw UnknownException(exception)
    }
