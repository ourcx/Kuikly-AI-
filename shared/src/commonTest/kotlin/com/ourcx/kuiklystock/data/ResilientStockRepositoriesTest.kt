package com.ourcx.kuiklystock.data

import com.ourcx.kuiklystock.domain.QuoteDataSource
import com.ourcx.kuiklystock.domain.StockInsight
import com.ourcx.kuiklystock.domain.StockQuote
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ResilientStockRepositoriesTest {
    @Test
    fun fallsBackToFixtureQuotesWhenRemoteFails() {
        val fixture = InMemoryStockRepository()
        val repository = ResilientStockRepository(
            remoteRepository = FailingStockRepository,
            fixtureRepository = fixture,
        )
        var result: Result<List<StockQuote>>? = null

        repository.getQuotes { result = it }

        val quotes = requireNotNull(result).getOrThrow()
        assertTrue(quotes.isNotEmpty())
        assertTrue(quotes.all { it.dataSource == QuoteDataSource.FIXTURE })
        assertEquals("腾讯控股", repository.getQuote("00700").name)
    }

    @Test
    fun fallsBackToFixtureInsightWhenRemoteIsUnavailable() {
        val fixture = InMemoryStockRepository()
        val quote = fixture.getQuote("AAPL")
        val repository = ResilientInsightRepository(
            remoteRepository = FailingInsightRepository,
            fixtureRepository = fixture,
            remoteConfigured = { true },
        )
        var result: Result<StockInsight>? = null

        repository.getInsight(quote) { result = it }

        assertEquals("AAPL", requireNotNull(result).getOrThrow().symbol)
    }
}

private object FailingStockRepository : StockRepository {
    override fun getQuotes(callback: (Result<List<StockQuote>>) -> Unit) =
        callback(Result.failure(IllegalStateException("offline")))

    override fun getQuote(symbol: String): StockQuote = error("offline")
}

private object FailingInsightRepository : InsightRepository {
    override fun getInsight(quote: StockQuote, callback: (Result<StockInsight>) -> Unit) =
        callback(Result.failure(IllegalStateException("offline")))
}
