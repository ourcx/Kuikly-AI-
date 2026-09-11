package com.ourcx.kuiklystock.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TencentStockRepositoryTest {
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
    }

    @Test
    fun rejectsPayloadWithoutValidQuotes() {
        assertFailsWith<TencentStockException> { parseTencentQuotes("invalid") }
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
