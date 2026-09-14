package com.ourcx.kuiklystock.data

import com.ourcx.kuiklystock.domain.ChatProvider
import com.ourcx.kuiklystock.domain.ChatRequest
import com.ourcx.kuiklystock.domain.ChatResponse
import kotlinx.atomicfu.atomic

/**
 * Repository contract:
 * - Always reports configured because a local fallback is always available.
 * - Prefers the remote repository when it is configured.
 * - Falls back to the local repository when the remote path is unconfigured, fails asynchronously,
 *   or throws synchronously.
 * - Publishes exactly one final callback result even if an underlying repository misbehaves and
 *   invokes its callback multiple times.
 */
class ResilientChatRepository(
    private val remoteRepository: ChatRepository,
    private val localRepository: ChatRepository,
) : ChatRepository {
    override val isConfigured: Boolean = true
    val isRemoteConfigured: Boolean
        get() = remoteRepository.isConfigured

    /**
     * Observable contract:
     * - Success returns the remote response as-is, preserving its provider semantics.
     * - Local fallback success is normalized to `LOCAL`.
     * - When both channels fail, the final failure from the local channel is returned.
     * - [callback] is invoked at most once.
     */
    override fun ask(request: ChatRequest, callback: (Result<ChatResponse>) -> Unit) {
        val phase = atomic(PHASE_REMOTE)

        fun complete(expectedPhase: Int, result: Result<ChatResponse>) {
            if (phase.compareAndSet(expectedPhase, PHASE_COMPLETED)) callback(result)
        }

        fun askLocal() {
            if (!phase.compareAndSet(PHASE_REMOTE, PHASE_LOCAL)) return
            try {
                localRepository.ask(request) { localResult ->
                    complete(PHASE_LOCAL, localResult.map(::normalizeLocalResponse))
                }
            } catch (error: Exception) {
                complete(PHASE_LOCAL, Result.failure(error))
            }
        }

        if (!remoteRepository.isConfigured) {
            askLocal()
            return
        }

        try {
            remoteRepository.ask(request) { remoteResult ->
                remoteResult.fold(
                    onSuccess = { response -> complete(PHASE_REMOTE, Result.success(response)) },
                    onFailure = { askLocal() },
                )
            }
        } catch (_: Exception) {
            askLocal()
        }
    }

    override fun askStreaming(
        request: ChatRequest,
        onDelta: (String) -> Unit,
        callback: (Result<ChatResponse>) -> Unit,
    ) {
        val phase = atomic(PHASE_REMOTE)
        val receivedRemoteDelta = atomic(false)

        fun complete(expectedPhase: Int, result: Result<ChatResponse>) {
            if (phase.compareAndSet(expectedPhase, PHASE_COMPLETED)) callback(result)
        }

        fun askLocal() {
            if (!phase.compareAndSet(PHASE_REMOTE, PHASE_LOCAL)) return
            try {
                localRepository.askStreaming(
                    request = request,
                    onDelta = onDelta,
                    callback = { localResult ->
                        complete(PHASE_LOCAL, localResult.map(::normalizeLocalResponse))
                    },
                )
            } catch (error: Exception) {
                complete(PHASE_LOCAL, Result.failure(error))
            }
        }

        if (!remoteRepository.isConfigured) {
            askLocal()
            return
        }

        try {
            remoteRepository.askStreaming(
                request = request,
                onDelta = { delta ->
                    if (phase.value == PHASE_REMOTE && delta.isNotEmpty()) {
                        receivedRemoteDelta.value = true
                        onDelta(delta)
                    }
                },
                callback = { remoteResult ->
                    remoteResult.fold(
                        onSuccess = { response -> complete(PHASE_REMOTE, Result.success(response)) },
                        onFailure = { error ->
                            // Mixing a local answer into an already visible remote answer would corrupt the turn.
                            if (receivedRemoteDelta.value) complete(PHASE_REMOTE, Result.failure(error)) else askLocal()
                        },
                    )
                },
            )
        } catch (error: Exception) {
            if (receivedRemoteDelta.value) complete(PHASE_REMOTE, Result.failure(error)) else askLocal()
        }
    }

    private fun normalizeLocalResponse(response: ChatResponse): ChatResponse =
        if (response.provider == ChatProvider.LOCAL) response else response.copy(provider = ChatProvider.LOCAL)

    private companion object {
        const val PHASE_REMOTE = 0
        const val PHASE_LOCAL = 1
        const val PHASE_COMPLETED = 2
    }
}
