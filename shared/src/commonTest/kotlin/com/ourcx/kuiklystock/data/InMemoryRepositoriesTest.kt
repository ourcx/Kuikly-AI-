package com.ourcx.kuiklystock.data

import com.ourcx.kuiklystock.domain.ChatRequest
import com.ourcx.kuiklystock.domain.ChatResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class InMemoryRepositoriesTest {
    @Test
    fun stockRepositoryReturnsStableFixturesAndNormalizesSymbols() {
        val repository = InMemoryStockRepository()

        assertEquals(listOf("00700", "09988", "600519", "AAPL", "TSLA"), repository.getQuotes().map { it.symbol })
        assertEquals("腾讯控股", repository.getQuote(" 00700 " ).name)
        assertEquals("AAPL", repository.getInsight("aapl").symbol)
    }

    @Test
    fun stockRepositoryReportsUnknownSymbols() {
        val error = assertFailsWith<StockNotFoundException> {
            InMemoryStockRepository().getQuote("UNKNOWN")
        }

        assertEquals("UNKNOWN", error.symbol)
    }

    @Test
    fun chatRepositoryMatchesCodeAndNameAndFallsBackToDefault() {
        val repository = InMemoryChatRepository()

        assertTrue(repository.askSuccessfully("Analyze AAPL").answer.contains("AAPL"))
        assertTrue(repository.askSuccessfully("600519").answer.contains("600519"))
        assertEquals(listOf("00700"), repository.askSuccessfully("market focus").symbols)
        val response = repository.askSuccessfully("Analyze TSLA")
        assertEquals(listOf("TSLA"), response.symbols)
        assertTrue(response.showTrend)
    }

    @Test
    fun chatRepositoryExposesDeterministicFailureScenario() {
        var result: Result<ChatResponse>? = null
        InMemoryChatRepository().ask(ChatRequest(question = "\u5931\u8d25")) { result = it }

        assertTrue(requireNotNull(result).isFailure)
        assertTrue(requireNotNull(result).exceptionOrNull() is ChatFixtureException)
    }
}

private fun InMemoryChatRepository.askSuccessfully(question: String): ChatResponse {
    var result: Result<ChatResponse>? = null
    ask(ChatRequest(question = question)) { result = it }
    return requireNotNull(result).getOrThrow()
}
