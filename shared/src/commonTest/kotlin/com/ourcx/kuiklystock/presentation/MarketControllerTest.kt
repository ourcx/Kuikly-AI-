package com.ourcx.kuiklystock.presentation

import com.ourcx.kuiklystock.data.StockRepository
import com.ourcx.kuiklystock.domain.LoadState
import com.ourcx.kuiklystock.domain.MarketState
import com.ourcx.kuiklystock.domain.StockInsight
import com.ourcx.kuiklystock.domain.StockQuote
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MarketControllerTest {
    @Test
    fun loadPublishesLoadingThenContent() {
        val observed = mutableListOf<MarketState>()
        val controller = MarketController(stockRepository = FakeStockRepository(), onStateChanged = observed::add)

        controller.load()

        assertIs<LoadState.Loading>(observed[0].quotes)
        val content = assertIs<LoadState.Content<List<StockQuote>>>(controller.state.quotes)
        assertEquals(listOf(QUOTE), content.value)
    }

    @Test
    fun loadMapsEmptyAndFailureResultsToReadableStates() {
        val emptyController = MarketController(FakeStockRepository(quotes = emptyList()))
        emptyController.load()
        assertIs<LoadState.Empty>(emptyController.state.quotes)

        val failingController = MarketController(FakeStockRepository(failure = IllegalStateException()))
        failingController.load()
        assertEquals(
            "服务暂时不可用，请重试",
            assertIs<LoadState.Error>(failingController.state.quotes).message,
        )
    }

    @Test
    fun demoStatesAndRetryFollowTheSameStateContract() {
        val controller = MarketController(FakeStockRepository())

        controller.showDemoState(MarketDemoState.EMPTY)
        assertIs<LoadState.Empty>(controller.state.quotes)
        controller.showDemoState(MarketDemoState.ERROR)
        assertEquals("行情加载失败，请重试", assertIs<LoadState.Error>(controller.state.quotes).message)
        controller.showDemoState(MarketDemoState.LOADING)
        assertIs<LoadState.Loading>(controller.state.quotes)
        controller.retry()
        assertIs<LoadState.Content<List<StockQuote>>>(controller.state.quotes)
    }

    @Test
    fun selectStockReturnsContentAndReadableErrors() {
        val controller = MarketController(FakeStockRepository())

        val content = assertIs<LoadState.Content<*>>(controller.selectStock(" DEMO " ).content)
        assertEquals(QUOTE, assertIs<com.ourcx.kuiklystock.domain.StockDetailContent>(content.value).quote)
        assertEquals("股票代码不能为空", assertIs<LoadState.Error>(controller.selectStock("  " ).content).message)
        assertEquals("未找到股票：MISS", assertIs<LoadState.Error>(controller.selectStock("MISS").content).message)
    }
}

private class FakeStockRepository(
    private val quotes: List<StockQuote> = listOf(QUOTE),
    private val failure: Throwable? = null,
) : StockRepository {
    override fun getQuotes(): List<StockQuote> {
        failure?.let { throw it }
        return quotes
    }

    override fun getQuote(symbol: String): StockQuote =
        if (symbol.equals(QUOTE.symbol, ignoreCase = true)) QUOTE else throw com.ourcx.kuiklystock.data.StockNotFoundException(symbol)

    override fun getInsight(symbol: String): StockInsight =
        if (symbol.equals(INSIGHT.symbol, ignoreCase = true)) INSIGHT else throw com.ourcx.kuiklystock.data.StockNotFoundException(symbol)
}

private val QUOTE = StockQuote("DEMO", "示例", "TEST", 10.0, 1.0, 10.0, 9.0, 10.0, 8.0, 9.0, 100, listOf(9.0, 10.0))
private val INSIGHT = StockInsight("DEMO", "上行", "摘要", listOf("信号"), listOf("风险"), "现在")
