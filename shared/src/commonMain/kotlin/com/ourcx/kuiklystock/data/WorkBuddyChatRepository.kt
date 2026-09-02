package com.ourcx.kuiklystock.data

import com.ourcx.kuiklystock.domain.ChatRequest
import com.ourcx.kuiklystock.domain.ChatResponse
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class WorkBuddyChatRepository(
    private val configurationProvider: () -> Boolean,
    private val requestInvoker: (payload: String, callback: (Result<String>) -> Unit) -> Unit,
) : ChatRepository {
    constructor(
        isConfigured: Boolean,
        requestInvoker: (payload: String, callback: (Result<String>) -> Unit) -> Unit,
    ) : this({ isConfigured }, requestInvoker)

    override val isConfigured: Boolean
        get() = configurationProvider()

    override fun ask(request: ChatRequest, callback: (Result<ChatResponse>) -> Unit) {
        if (!isConfigured) {
            callback(Result.failure(WorkBuddyChatException(UNCONFIGURED_ERROR)))
            return
        }

        val payload = runCatching { json.encodeToString(request) }
            .getOrElse { error ->
                callback(Result.failure(WorkBuddyChatException(ENCODING_ERROR, error)))
                return
            }

        var nativeCallbackStarted = false
        try {
            requestInvoker(payload) { nativeResult ->
                nativeCallbackStarted = true
                callback(
                    nativeResult.fold(
                        onSuccess = { responseJson -> decodeResponse(responseJson) },
                        onFailure = { error ->
                            Result.failure(WorkBuddyChatException(NATIVE_REQUEST_ERROR, error))
                        },
                    ),
                )
            }
        } catch (error: Throwable) {
            if (nativeCallbackStarted) throw error
            callback(Result.failure(WorkBuddyChatException(NATIVE_INVOCATION_ERROR, error)))
        }
    }

    private fun decodeResponse(responseJson: String): Result<ChatResponse> =
        runCatching { json.decodeFromString<ChatResponse>(responseJson) }
            .fold(
                onSuccess = { Result.success(it) },
                onFailure = { error ->
                    Result.failure(WorkBuddyChatException(DECODING_ERROR, error))
                },
            )

    private companion object {
        val json = Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }

        const val UNCONFIGURED_ERROR = "WorkBuddy chat is not configured"
        const val ENCODING_ERROR = "Failed to encode WorkBuddy chat request"
        const val NATIVE_INVOCATION_ERROR = "Failed to invoke the WorkBuddy native request"
        const val NATIVE_REQUEST_ERROR = "WorkBuddy native request failed"
        const val DECODING_ERROR = "Failed to decode WorkBuddy chat response"
    }
}

class WorkBuddyChatException(
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)
