package com.ourcx.kuiklystock.data

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

        assertTrue(repository.ask("分析 aapl").contains("苹果（AAPL）"))
        assertTrue(repository.ask("看看贵州茅台").contains("贵州茅台（600519）"))
        assertTrue(repository.ask("今天关注什么").contains("腾讯控股（00700）"))
        assertTrue(repository.ask("分析 TSLA").endsWith("<!--stock-ai:{\"symbols\":[\"TSLA\"],\"showTrend\":true}-->"))
    }

    @Test
    fun chatRepositoryExposesDeterministicFailureScenario() {
        val error = assertFailsWith<ChatFixtureException> {
            InMemoryChatRepository().ask("请演示失败")
        }

        assertEquals("演示聊天服务暂时不可用，请重试", error.message)
    }
}
