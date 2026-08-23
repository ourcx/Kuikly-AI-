package com.ourcx.kuiklystock.data

import com.ourcx.kuiklystock.domain.ChatProvider
import com.ourcx.kuiklystock.domain.ChatRequest
import com.ourcx.kuiklystock.domain.ChatResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ResilientChatRepositoryTest {
    @Test
    fun usesRemoteResponseWhenAvailable() {
        val repository = ResilientChatRepository(
            remoteRepository = StubRepository(true) {
                Result.success(ChatResponse(answer = "remote", provider = ChatProvider.WORKBUDDY))
            },
            localRepository = StubRepository(true) {
                Result.success(ChatResponse(answer = "local", provider = ChatProvider.LOCAL))
            },
        )

        var result: Result<ChatResponse>? = null
        repository.ask(ChatRequest("question")) { result = it }

        assertEquals("remote", result?.getOrThrow()?.answer)
        assertEquals(ChatProvider.WORKBUDDY, result?.getOrThrow()?.provider)
    }

    @Test
    fun fallsBackToLocalWhenRemoteIsUnavailableOrFails() {
        listOf(
            StubRepository(false) { error("should not be called") },
            StubRepository(true) { Result.failure(IllegalStateException("offline")) },
        ).forEach { remote ->
            val repository = ResilientChatRepository(
                remoteRepository = remote,
                localRepository = StubRepository(true) {
                    Result.success(ChatResponse(answer = "local fallback"))
                },
            )
            var result: Result<ChatResponse>? = null

            repository.ask(ChatRequest("question")) { result = it }

            assertEquals("local fallback", result?.getOrThrow()?.answer)
            assertEquals(ChatProvider.LOCAL, result?.getOrThrow()?.provider)
        }
    }

    @Test
    fun completesOnlyOnceWhenRemoteInvokesCallbackTwice() {
        val remote = object : ChatRepository {
            override val isConfigured = true
            override fun ask(request: ChatRequest, callback: (Result<ChatResponse>) -> Unit) {
                callback(Result.success(ChatResponse(answer = "first")))
                callback(Result.success(ChatResponse(answer = "second")))
            }
        }
        var callbackCount = 0
        ResilientChatRepository(remote, StubRepository(true) { error("unused") })
            .ask(ChatRequest("question")) { callbackCount += 1 }

        assertEquals(1, callbackCount)
        assertTrue(remote.isConfigured)
    }

    @Test
    fun fallsBackWhenRemoteThrowsAndNormalizesLocalProvider() {
        val remote = object : ChatRepository {
            override val isConfigured = true
            override fun ask(request: ChatRequest, callback: (Result<ChatResponse>) -> Unit) {
                throw IllegalStateException("Remote unavailable")
            }
        }
        val repository = ResilientChatRepository(
            remoteRepository = remote,
            localRepository = StubRepository(true) {
                Result.success(ChatResponse(answer = "local", provider = ChatProvider.WORKBUDDY))
            },
        )
        var result: Result<ChatResponse>? = null

        repository.ask(ChatRequest("question")) { result = it }

        assertEquals("local", result?.getOrThrow()?.answer)
        assertEquals(ChatProvider.LOCAL, result?.getOrThrow()?.provider)
    }

    @Test
    fun returnsLocalFailureAndCompletesOnlyOnceWhenFallbackMisbehaves() {
        val localFailure = IllegalStateException("Local unavailable")
        val local = object : ChatRepository {
            override val isConfigured = true
            override fun ask(request: ChatRequest, callback: (Result<ChatResponse>) -> Unit) {
                callback(Result.failure(localFailure))
                callback(Result.success(ChatResponse(answer = "late success")))
            }
        }
        val repository = ResilientChatRepository(
            remoteRepository = StubRepository(true) { Result.failure(IllegalStateException("Remote unavailable")) },
            localRepository = local,
        )
        val results = mutableListOf<Result<ChatResponse>>()

        repository.ask(ChatRequest("question"), results::add)

        assertEquals(1, results.size)
        assertEquals(localFailure, results.single().exceptionOrNull())
    }
}

private class StubRepository(
    override val isConfigured: Boolean,
    private val response: () -> Result<ChatResponse>,
) : ChatRepository {
    override fun ask(request: ChatRequest, callback: (Result<ChatResponse>) -> Unit) {
        callback(response())
    }
}
