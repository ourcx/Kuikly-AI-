package com.ourcx.kuiklystock.data

import com.ourcx.kuiklystock.domain.ChatContext
import com.ourcx.kuiklystock.domain.ChatQuoteContext
import com.ourcx.kuiklystock.domain.ChatRequest
import com.ourcx.kuiklystock.domain.ChatResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WorkBuddyChatRepositoryTest {
    @Test
    fun askSerializesConversationAndQuoteContextAndDecodesResponseBitsUT() {
        var capturedPayload = ""
        val repository = WorkBuddyChatRepository(isConfigured = true) { payload, callback ->
            capturedPayload = payload
            callback(
                Result.success(
                    """{"answer":"Analysis ready","conversation_id":"conversation-2","symbols":["AAPL"],"show_trend":true,"ignored":1}""",
                ),
            )
        }
        var result: Result<ChatResponse>? = null

        repository.ask(
            ChatRequest(
                question = "Analyze AAPL",
                conversationId = "conversation-1",
                context = ChatContext(
                    quotes = listOf(
                        ChatQuoteContext(
                            symbol = "AAPL",
                            name = "Apple",
                            exchange = "NASDAQ",
                            price = 227.16,
                            change = 2.44,
                            changePercent = 1.09,
                        ),
                    ),
                ),
            ),
        ) { result = it }

        assertTrue(capturedPayload.contains("\"conversation_id\":\"conversation-1\""))
        assertTrue(capturedPayload.contains("\"change_percent\":1.09"))
        val response = requireNotNull(result).getOrThrow()
        assertEquals("Analysis ready", response.answer)
        assertEquals("conversation-2", response.conversationId)
        assertEquals(listOf("AAPL"), response.symbols)
        assertTrue(response.showTrend)
    }

    @Test
    fun askReturnsFailureWithoutInvokingNativeWhenUnconfiguredBitsUT() {
        var invoked = false
        val repository = WorkBuddyChatRepository(isConfigured = false) { _, _ -> invoked = true }
        var result: Result<ChatResponse>? = null

        repository.ask(ChatRequest(question = "Analyze AAPL")) { result = it }

        assertFalse(invoked)
        assertTrue(requireNotNull(result).isFailure)
        assertTrue(requireNotNull(result).exceptionOrNull() is WorkBuddyChatException)
    }

    @Test
    fun askMapsNativeAndInvalidJsonFailuresBitsUT() {
        val nativeRepository = WorkBuddyChatRepository(isConfigured = true) { _, callback ->
            callback(Result.failure(IllegalStateException("Proxy unavailable")))
        }
        var nativeResult: Result<ChatResponse>? = null
        nativeRepository.ask(ChatRequest(question = "Analyze AAPL")) { nativeResult = it }

        val invalidJsonRepository = WorkBuddyChatRepository(isConfigured = true) { _, callback ->
            callback(Result.success("not-json"))
        }
        var invalidJsonResult: Result<ChatResponse>? = null
        invalidJsonRepository.ask(ChatRequest(question = "Analyze AAPL")) { invalidJsonResult = it }

        assertTrue(requireNotNull(nativeResult).exceptionOrNull() is WorkBuddyChatException)
        assertTrue(requireNotNull(invalidJsonResult).exceptionOrNull() is WorkBuddyChatException)
    }
}
