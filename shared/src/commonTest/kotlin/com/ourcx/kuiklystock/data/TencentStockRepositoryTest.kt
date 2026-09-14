package com.ourcx.kuiklystock.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import com.ourcx.kuiklystock.domain.QuoteDataSource

class TencentStockRepositoryTest {
    @Test
    fun normalizesSupportedUserStockFormats() {
        assertEquals("sh600519", parseUserStockDefinition("600519")?.apiCode)
        assertEquals("sz000001", parseUserStockDefinition("000001")?.apiCode)
        assertEquals("hk00700", parseUserStockDefinition("00700")?.apiCode)
        assertEquals("usAAPL", parseUserStockDefinition("aapl")?.apiCode)
        assertEquals("sz300750", parseUserStockDefinition("sz300750")?.apiCode)
        assertEquals(null, parseUserStockDefinition("123"))
        assertEquals(null, parseUserStockDefinition("unknown-symbol"))
    }

    @Test
    fun validatesAddsAndPersistsCustomStockWithoutDuplicates() {
        val persisted = mutableListOf<List<String>>()
        val repository = TencentStockRepository(
            requestInvoker = { codes, callback ->
                val definition = requireNotNull(parseUserStockDefinition(codes.single()))
                callback(
                    Result.success(
                        quoteLine(definition.apiCode, "Custom Stock", definition.symbol, 25.0, 24.0, 24.5, 100.0, 1.0, 4.17, 26.0, 23.0),
                    ),
                )
            },
            onCustomCodesChanged = persisted::add,
        )
        var first: Result<com.ourcx.kuiklystock.domain.StockQuote>? = null
        var second: Result<com.ourcx.kuiklystock.domain.StockQuote>? = null

        repository.addQuote("002594") { first = it }
        repository.addQuote("sz002594") { second = it }

        assertEquals("002594", requireNotNull(first).getOrThrow().symbol)
        assertTrue(requireNotNull(second).isSuccess)
        assertEquals("002594", repository.directory.first().symbol)
        assertEquals(1, repository.directory.count { it.symbol == "002594" })
        assertEquals(listOf(listOf("sz002594")), persisted)
        assertEquals(25.0, repository.getQuote("002594").price)
    }

    @Test
    fun restoresPersistedCustomCodesBeforeBuiltInDirectory() {
        val repository = TencentStockRepository(
            requestInvoker = { _, _ -> error("No request expected") },
            initialCustomCodes = listOf("usMETA", "usMETA", "bad-symbol"),
        )

        assertEquals("META", repository.directory.first().symbol)
        assertEquals(STOCKS.size + 1, repository.directory.size)
    }

    @Test
    fun stockDirectoryCoversMultiplePagesAndMarkets() {
        assertTrue(STOCKS.size >= 12)
        assertTrue(STOCKS.count { it.exchange == "SSE" || it.exchange == "SZSE" } >= 4)
        assertTrue(STOCKS.count { it.exchange == "HKEX" } >= 4)
        assertTrue(STOCKS.count { it.exchange == "NASDAQ" || it.exchange == "NYSE" } >= 4)
    }

    @Test
    fun requestsAndCachesOnlyTheSelectedPage() {
        val requestedCodes = mutableListOf<List<String>>()
        val expectedDefinitions = STOCKS.drop(2).take(2)
        val repository = TencentStockRepository { codes, callback ->
            requestedCodes += codes
            callback(
                Result.success(
                    expectedDefinitions.joinToString("\n") { definition ->
                        quoteLine(
                            definition.apiCode,
                            definition.fallbackName,
                            definition.symbol,
                            12.0,
                            10.0,
                            11.0,
                            100.0,
                            2.0,
                            20.0,
                            13.0,
                            9.0,
                        )
                    },
                ),
            )
        }
        var result: Result<List<com.ourcx.kuiklystock.domain.StockQuote>>? = null

        repository.getQuotesPage(offset = 2, limit = 2) { result = it }

        assertEquals(listOf(expectedDefinitions.map(StockDefinition::apiCode)), requestedCodes)
        assertEquals(expectedDefinitions.map(StockDefinition::symbol), requireNotNull(result).getOrThrow().map { it.symbol })
        assertEquals(12.0, repository.getQuote(expectedDefinitions.last().symbol).price)
        assertEquals(STOCKS.size, repository.directory.size)
    }

    @Test
    fun parsesMultiMarketQuoteFieldsInDefinitionOrder() {
        val payload = listOf(
            quoteLine("usAAPL", "苹果", "AAPL.OQ", 326.57, 315.34, 316.67, 70011913.0, 11.23, 3.56, 326.74, 316.51),
            quoteLine("sz000001", "平安银行", "000001", 11.76, 11.85, 11.82, 382715.0, -0.09, -0.76, 11.86, 11.73),
        ).joinToString("\n")
        val definitions = listOf(
            StockDefinition("sz000001", "000001", "SZSE", "平安银行"),
            StockDefinition("usAAPL", "AAPL", "NASDAQ", "苹果"),
        )

        val quotes = parseTencentQuotes(payload, definitions)

        assertEquals(listOf("000001", "AAPL"), quotes.map { it.symbol })
        assertEquals(11.76, quotes.first().price)
        assertEquals(-0.76, quotes.first().changePercent)
        assertEquals(70011913L, quotes.last().volume)
        assertTrue(quotes.first().trendPoints.isNotEmpty())
        assertEquals(QuoteDataSource.TENCENT, quotes.first().dataSource)
    }

    @Test
    fun rejectsPayloadWithoutEveryRequestedQuote() {
        assertFailsWith<TencentStockException> { parseTencentQuotes("invalid") }

        val definitions = listOf(
            StockDefinition("sz000001", "000001", "SZSE", "平安银行"),
            StockDefinition("usAAPL", "AAPL", "NASDAQ", "苹果"),
        )
        val onlyOneQuote = quoteLine("usAAPL", "苹果", "AAPL.OQ", 326.57, 315.34, 316.67, 1.0, 11.23, 3.56, 326.74, 316.51)
        assertFailsWith<TencentStockException> { parseTencentQuotes(onlyOneQuote, definitions) }
    }

    @Test
    fun rejectsDuplicateOrMalformedRequestedQuote() {
        val definition = StockDefinition("usAAPL", "AAPL", "NASDAQ", "苹果")
        val validQuote = quoteLine("usAAPL", "苹果", "AAPL.OQ", 326.57, 315.34, 316.67, 1.0, 11.23, 3.56, 326.74, 316.51)

        assertFailsWith<TencentStockException> {
            parseTencentQuotes("$validQuote\n$validQuote", listOf(definition))
        }
        assertFailsWith<TencentStockException> {
            parseTencentQuotes(quoteLine("usAAPL", "苹果", "AAPL.OQ", Double.NaN, 315.34, 316.67, 1.0, 11.23, 3.56, 326.74, 316.51).replace("NaN", "bad-price"), listOf(definition))
        }
        assertFailsWith<TencentStockException> {
            parseTencentQuotes(quoteLine("usAAPL", "苹果", "MSFT.OQ", 326.57, 315.34, 316.67, 1.0, 11.23, 3.56, 326.74, 316.51), listOf(definition))
        }
    }

    @Test
    fun repositoryKeepsLastCompleteCacheWhenNextPayloadIsIncomplete() {
        val payloads = ArrayDeque(
            listOf(
                STOCKS.joinToString("\n") { definition ->
                    quoteLine(definition.apiCode, definition.fallbackName, definition.symbol, 10.0, 9.0, 9.5, 100.0, 1.0, 11.11, 10.5, 9.5)
                },
                quoteLine(STOCKS.first().apiCode, STOCKS.first().fallbackName, STOCKS.first().symbol, 11.0, 10.0, 10.5, 100.0, 1.0, 10.0, 11.5, 10.5),
            ),
        )
        val repository = TencentStockRepository { _, callback -> callback(Result.success(payloads.removeFirst())) }
        var firstResult: Result<List<com.ourcx.kuiklystock.domain.StockQuote>>? = null
        var secondResult: Result<List<com.ourcx.kuiklystock.domain.StockQuote>>? = null

        repository.getQuotes { firstResult = it }
        repository.getQuotes { secondResult = it }

        assertTrue(requireNotNull(firstResult).isSuccess)
        assertTrue(requireNotNull(secondResult).isFailure)
        assertEquals(10.0, repository.getQuote(STOCKS.last().symbol).price)
    }
}

private fun quoteLine(
    apiCode: String,
    name: String,
    returnedSymbol: String,
    price: Double,
    previousClose: Double,
    open: Double,
    volume: Double,
    change: Double,
    changePercent: Double,
    high: Double,
    low: Double,
): String {
    val fields = MutableList(35) { "" }
    fields[0] = "1"
    fields[1] = name
    fields[2] = returnedSymbol
    fields[3] = price.toString()
    fields[4] = previousClose.toString()
    fields[5] = open.toString()
    fields[6] = volume.toString()
    fields[30] = "2026-09-11 10:43:09"
    fields[31] = change.toString()
    fields[32] = changePercent.toString()
    fields[33] = high.toString()
    fields[34] = low.toString()
    return "v_$apiCode=\"${fields.joinToString("~")}\";"
}
