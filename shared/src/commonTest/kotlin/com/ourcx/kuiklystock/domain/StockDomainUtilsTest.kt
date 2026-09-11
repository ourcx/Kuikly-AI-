package com.ourcx.kuiklystock.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class StockDomainUtilsTest {
    @Test
    fun formatsPricesChangesAndVolumesForDisplay() {
        assertEquals("382.40", formatStockPrice(382.4))
        assertEquals("+1.24", formatStockChange(1.235))
        assertEquals("-1.24%", formatStockChangePercent(-1.235))
        assertEquals("0.00%", formatStockChangePercent(-0.001))
        assertEquals("9999", formatStockVolume(9_999))
        assertEquals("1.25万", formatStockVolume(12_500))
        assertEquals("1.50亿", formatStockVolume(150_000_000))
    }

    @Test
    fun normalizesTrendPointsAndNeutralizesUnavailableValues() {
        assertEquals(listOf(0f, 0.5f, 1f), normalizeTrendPoints(listOf(10.0, 15.0, 20.0)))
        assertEquals(listOf(0.5f, 0.5f), normalizeTrendPoints(listOf(3.0, 3.0)))
        assertEquals(listOf(0f, 0.5f, 1f), normalizeTrendPoints(listOf(1.0, Double.NaN, 3.0)))
        assertEquals(listOf(0.5f, 0.5f), normalizeTrendPoints(listOf(Double.NaN, Double.POSITIVE_INFINITY)))
        assertTrue(normalizeTrendPoints(emptyList()).isEmpty())
    }

    @Test
    fun analyzesSnapshotRangeWithoutClaimingTimeSeries() {
        val analysis = analyzeTrend(
            StockQuote(
                symbol = "TEST", name = "测试", exchange = "SSE", price = 108.0,
                change = 8.0, changePercent = 8.0, open = 102.0, high = 110.0,
                low = 90.0, previousClose = 100.0, volume = 1L,
                trendPoints = listOf(100.0, 102.0, 90.0, 110.0, 108.0), updatedAt = "",
            ),
        )

        assertEquals(20.0, analysis.rangePercent)
        assertEquals(90.0, analysis.rangePositionPercent)
        assertEquals(8.0, analysis.relativeToPreviousClosePercent)
    }

    @Test
    fun parsesValidTrailingStockMetadata() {
        val parsed = parseStockAiMetadata(
            "## 行情速览\n正文\n<!--stock-ai:{\"symbols\":[\"00700\",\"AAPL\"],\"showTrend\":true}-->",
        )

        assertEquals("## 行情速览\n正文", parsed.markdown)
        assertEquals(listOf("00700", "AAPL"), parsed.symbols)
        assertTrue(parsed.showTrend)
    }

    @Test
    fun preservesMarkdownWhenMetadataIsMissingOrMalformed() {
        val plain = "普通 Markdown"
        val malformed = "正文\n<!--stock-ai:{not-json}-->"

        assertEquals(ParsedStockAiContent(plain), parseStockAiMetadata(plain))
        assertEquals(ParsedStockAiContent(malformed), parseStockAiMetadata(malformed))
    }

    @Test
    fun buildsStructuredBlocksInStableSymbolOrder() {
        val blocks = buildChatContentBlocks(
            ParsedStockAiContent("分析正文", listOf("00700", "AAPL"), showTrend = true),
        )

        assertEquals(5, blocks.size)
        assertEquals("分析正文", assertIs<ChatContentBlock.Markdown>(blocks[0]).text)
        assertEquals("00700", assertIs<ChatContentBlock.StockCard>(blocks[1]).symbol)
        assertEquals("00700", assertIs<ChatContentBlock.Trend>(blocks[2]).symbol)
        assertEquals("AAPL", assertIs<ChatContentBlock.StockCard>(blocks[3]).symbol)
        assertEquals("AAPL", assertIs<ChatContentBlock.Trend>(blocks[4]).symbol)

        val withoutTrend = buildChatContentBlocks(ParsedStockAiContent("正文", listOf("TSLA")))
        assertEquals(2, withoutTrend.size)
        assertFalse(withoutTrend.any { it is ChatContentBlock.Trend })
    }
}
