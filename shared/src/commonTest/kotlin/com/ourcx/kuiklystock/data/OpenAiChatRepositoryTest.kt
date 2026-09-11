package com.ourcx.kuiklystock.data

import com.ourcx.kuiklystock.domain.ChatContext
import com.ourcx.kuiklystock.domain.ChatProvider
import com.ourcx.kuiklystock.domain.ChatQuoteContext
import com.ourcx.kuiklystock.domain.ChatRequest
import com.ourcx.kuiklystock.domain.StockQuote
import com.ourcx.kuiklystock.domain.ChatResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OpenAiChatRepositoryTest {
    @Test
    fun stockInsightUsesOpenAiRequestAndDecodesStructuredResult() {
        var requestBody = ""
        val repository = OpenAiChatRepository({ true }, { "gpt-test" }) { body, callback ->
            requestBody = body
            callback(
                Result.success(
                    """{"id":"response-insight","output_text":"{\"trend_label\":\"震荡偏强\",\"summary\":\"价格高于前收。\",\"signals\":[\"关注日内高点\"],\"risks\":[\"波动风险，不构成投资建议\"]}"}""",
                ),
            )
        }
        var result: Result<com.ourcx.kuiklystock.domain.StockInsight>? = null

        repository.getInsight(QUOTE) { result = it }

        val insight = requireNotNull(result).getOrThrow()
        assertEquals("AAPL", insight.symbol)
        assertEquals("震荡偏强", insight.trendLabel)
        assertTrue(requestBody.contains("\"model\":\"gpt-test\""))
        assertTrue(requestBody.contains("AAPL"))
    }

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

private val QUOTE = StockQuote(
    symbol = "AAPL", name = "苹果", exchange = "NASDAQ", price = 326.57, change = 11.23,
    changePercent = 3.56, open = 316.67, high = 326.74, low = 316.51, previousClose = 315.34,
    volume = 70011913, trendPoints = listOf(315.34, 326.57), updatedAt = "2026-09-11 10:43:09",
)

private const val OPENAI_RESPONSE = """
{
  "id": "resp_next",
  "output": [
    {"type": "message", "content": [{"type": "output_text", "text": "AAPL 行情偏强。"}]}
  ]
}
"""
