package com.yral.shared.features.chat.data

import co.touchlab.kermit.Logger
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.features.chat.data.models.AssistantErrorDto
import com.yral.shared.features.chat.data.models.SendMessageRequestDto
import com.yral.shared.features.chat.data.models.StreamDonePayloadDto
import com.yral.shared.features.chat.data.models.StreamTokenPayloadDto
import com.yral.shared.features.chat.data.models.toDomain
import com.yral.shared.features.chat.domain.models.StreamEvent
import com.yral.shared.preferences.PrefKeys
import com.yral.shared.preferences.Preferences
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.plugins.sse.sse
import io.ktor.client.plugins.timeout
import io.ktor.client.request.headers
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.path
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json

/**
 * SSE consumer for POST .../messages/stream (Phase 2.7).
 *
 * Lives outside [ChatDataSource] because the existing one-shot data source's
 * symmetry (`httpGet` / `httpPost` / Dto in, Dto out) doesn't translate to
 * an SSE long-poll. Repository-level callers compose this alongside the
 * one-shot source.
 *
 * Lifecycle: the returned Flow is cold. Cancelling the consuming coroutine
 * cancels the underlying SSE connection via Ktor's `client.sse {}` lambda
 * cooperating with structured concurrency.
 *
 * Timeout: the per-call timeout override is set to INFINITE so the global
 * HttpTimeout(30s) doesn't cut the stream off mid-reply. The ViewModel
 * adds an explicit idle watchdog separately (Phase 7).
 */
class ChatStreamingDataSource(
    private val httpClient: HttpClient,
    private val json: Json,
    private val preferences: Preferences,
    private val chatBaseUrl: String,
) {
    fun streamMessage(
        conversationId: String,
        request: SendMessageRequestDto,
    ): Flow<StreamEvent> =
        flow {
            val idToken =
                preferences.getString(PrefKeys.ID_TOKEN.name)
                    ?: throw YralException("Authorisation not found")

            httpClient.sse(
                request = {
                    method = HttpMethod.Post
                    url {
                        host = chatBaseUrl
                        path(CONVERSATIONS_PATH, conversationId, MESSAGES_STREAM_PATH)
                    }
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $idToken")
                        append(HttpHeaders.Accept, ContentType.Text.EventStream.toString())
                    }
                    contentType(ContentType.Application.Json)
                    setBody(request)
                    timeout { requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS }
                },
            ) {
                incoming.collect { event ->
                    val name = event.event ?: return@collect
                    val data = event.data ?: return@collect
                    when (name) {
                        EVENT_TOKEN -> {
                            val payload = json.decodeFromString(StreamTokenPayloadDto.serializer(), data)
                            emit(StreamEvent.Token(text = payload.text))
                        }

                        EVENT_DONE -> {
                            val payload = json.decodeFromString(StreamDonePayloadDto.serializer(), data)
                            emit(
                                StreamEvent.Done(
                                    assistantMessage = payload.assistantMessage.toDomain(conversationId),
                                    provider = payload.provider,
                                    blocked = payload.blocked,
                                ),
                            )
                        }

                        EVENT_ERROR -> {
                            val payload = json.decodeFromString(AssistantErrorDto.serializer(), data)
                            emit(StreamEvent.Failed(error = payload.toDomain()))
                        }

                        else -> {
                            logger.d { "Unknown SSE event: name=$name dataLength=${data.length}" }
                        }
                    }
                }
            }
        }

    private companion object {
        private val logger = Logger.withTag("ChatStreamingDataSource")
        private const val CONVERSATIONS_PATH = "api/v1/chat/conversations"
        private const val MESSAGES_STREAM_PATH = "messages/stream"
        private const val EVENT_TOKEN = "token"
        private const val EVENT_DONE = "done"
        private const val EVENT_ERROR = "error"
    }
}
