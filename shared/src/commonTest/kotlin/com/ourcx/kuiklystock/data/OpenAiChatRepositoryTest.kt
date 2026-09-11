package com.ourcx.kuiklystock.data

import com.ourcx.kuiklystock.domain.ChatContext
import com.ourcx.kuiklystock.domain.ChatProvider
import com.ourcx.kuiklystock.domain.ChatQuoteContext
import com.ourcx.kuiklystock.domain.ChatRequest
import com.ourcx.kuiklystock.domain.ChatResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OpenAiChatRepositoryTest {
    @Test
    fun createsResponsesRequestAndParsesOutputText() {
        var payload = ""
        val repository = OpenAiChatRepository({ true }, { "gpt-test" }) { body, callback ->
            payload = body
            callback(Result.success(OPENAI_RESPONSE))
        }
        var result: Result<ChatResponse>? = null

        repository.ask(
            ChatRequest(
                question = "分析 AAPL",
                conversationId = "resp_previous",
                context = ChatContext(listOf(ChatQuoteContext("AAPL", "苹果", "NASDAQ", 326.57, 11.23, 3.56))),
            ),
        ) { result = it }

        assertTrue(payload.contains("\"model\":\"gpt-test\""))
        assertTrue(payload.contains("\"previous_response_id\":\"resp_previous\""))
        val response = requireNotNull(result).getOrThrow()
        assertEquals("resp_next", response.conversationId)
        assertEquals("AAPL 行情偏强。", response.answer)
        assertEquals(ChatProvider.OPENAI, response.provider)
        assertEquals(listOf("AAPL"), response.symbols)
    }

    @Test
    fun doesNotInvokeProxyWhenUnconfigured() {
        var invoked = false
        val repository = OpenAiChatRepository({ false }, { "gpt-test" }) { _, _ -> invoked = true }
        var result: Result<ChatResponse>? = null

        repository.ask(ChatRequest(question = "分析行情")) { result = it }

        assertFalse(invoked)
        assertTrue(requireNotNull(result).isFailure)
    }
}

private const val OPENAI_RESPONSE = """
{
  "id": "resp_next",
  "output": [
    {"type": "message", "content": [{"type": "output_text", "text": "AAPL 行情偏强。"}]}
  ]
}
"""
