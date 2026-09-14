package com.ourcx.kuiklystock.data

import com.ourcx.kuiklystock.domain.ChatRequest
import com.ourcx.kuiklystock.domain.ChatResponse
import com.ourcx.kuiklystock.domain.ChatContext
import com.ourcx.kuiklystock.domain.ChatQuoteContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class InMemoryRepositoriesTest {
    @Test
    fun chatUsesRemoteMarketContextEvenWhenSymbolIsNotInFixture() {
        val repository = InMemoryChatRepository()
        var result: Result<ChatResponse>? = null

        repository.ask(
            ChatRequest(
                question = "分析平安银行 000001",
                context = ChatContext(
                    listOf(ChatQuoteContext("000001", "平安银行", "SZSE", 11.78, -0.07, -0.59)),
                ),
            ),
        ) { result = it }

        val response = requireNotNull(result).getOrThrow()
        assertEquals(listOf("000001"), response.symbols)
        assertTrue(response.answer.contains("平安银行"))
    }

    @Test
    fun portfolioQuestionBuildsMultipleCardsAndTrendsFromCurrentContext() {
        val repository = InMemoryChatRepository()
        var result: Result<ChatResponse>? = null
        val quotes = listOf(
            ChatQuoteContext("CALM", "平稳股", "TEST", 10.0, 0.1, 1.0),
            ChatQuoteContext("HOT", "异动股", "TEST", 20.0, 1.0, 5.0),
            ChatQuoteContext("DOWN", "回落股", "TEST", 30.0, -0.9, -3.0),
            ChatQuoteContext("MID", "中位股", "TEST", 40.0, 0.8, 2.0),
        )

        repository.ask(
            ChatRequest(question = "请梳理自选股风险", context = ChatContext(quotes)),
        ) { result = it }

        val response = requireNotNull(result).getOrThrow()
        assertEquals(listOf("HOT", "DOWN", "MID"), response.symbols)
        assertTrue(response.showTrend)
        response.symbols.forEach { symbol -> assertTrue(response.answer.contains(symbol)) }
    }

    @Test
    fun buildsConservativeFallbackInsightForUnknownRemoteQuote() {
        val repository = InMemoryStockRepository()
        val quote = repository.getQuote("AAPL").copy(symbol = "NEW")
        var result: Result<com.ourcx.kuiklystock.domain.StockInsight>? = null

        repository.getInsight(quote) { result = it }

        val insight = requireNotNull(result).getOrThrow()
        assertEquals("NEW", insight.symbol)
        assertTrue(insight.risks.any { it.contains("不构成投资建议") })
    }
    @Test
    fun stockRepositoryReturnsStableFixturesAndNormalizesSymbols() {
        val repository = InMemoryStockRepository()

        var quotes: Result<List<com.ourcx.kuiklystock.domain.StockQuote>>? = null
        repository.getQuotes { quotes = it }
        assertEquals(
            listOf("00700", "09988", "600519", "AAPL", "TSLA"),
            requireNotNull(quotes).getOrThrow().map { it.symbol },
        )
        assertEquals("腾讯控股", repository.getQuote(" 00700 " ).name)
        var insight: Result<com.ourcx.kuiklystock.domain.StockInsight>? = null
        repository.getInsight(repository.getQuote("aapl")) { insight = it }
        assertEquals("AAPL", requireNotNull(insight).getOrThrow().symbol)
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
