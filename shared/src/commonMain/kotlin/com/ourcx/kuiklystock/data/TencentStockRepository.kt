package com.ourcx.kuiklystock.data

import com.ourcx.kuiklystock.domain.StockInsight
import com.ourcx.kuiklystock.domain.StockQuote

/** Production stock source backed by Tencent's multi-market quote endpoint. */
class TencentStockRepository(
    private val requestInvoker: (codes: List<String>, callback: (Result<String>) -> Unit) -> Unit,
) : StockRepository {
    private var quotesBySymbol: Map<String, StockQuote> = emptyMap()

    override fun getQuotes(callback: (Result<List<StockQuote>>) -> Unit) {
        requestInvoker(STOCKS.map(StockDefinition::apiCode)) { result ->
            callback(
                result.mapCatching { payload ->
                    parseTencentQuotes(payload, STOCKS).also { quotes ->
                        quotesBySymbol = quotes.associateBy { quote -> normalizeSymbol(quote.symbol) }
                    }
                },
            )
        }
    }

    override fun getQuote(symbol: String): StockQuote =
        quotesBySymbol[normalizeSymbol(symbol)] ?: throw StockNotFoundException(symbol)

    override fun getInsight(symbol: String): StockInsight {
        val quote = getQuote(symbol)
        val direction = when {
            quote.changePercent >= 2.0 -> "强势上行"
            quote.changePercent > 0.0 -> "震荡偏强"
            quote.changePercent <= -2.0 -> "明显回落"
            quote.changePercent < 0.0 -> "震荡偏弱"
            else -> "窄幅整理"
        }
        val location = when {
            quote.price >= quote.high -> "最新价位于日内高位"
            quote.price <= quote.low -> "最新价接近日内低位"
            quote.price >= quote.open -> "最新价高于开盘价"
            else -> "最新价低于开盘价"
        }
        return StockInsight(
            symbol = quote.symbol,
            trendLabel = direction,
            summary = "${quote.name}当前$direction，$location，日内振幅为${quote.intradayAmplitude()}%。",
            signals = listOf(
                location,
                "较前收${if (quote.change >= 0) "上涨" else "下跌"}${quote.change.absoluteValue().format(2)}",
            ),
            risks = listOf(
                "实时行情可能因网络或数据源调整出现延迟",
                "短期价格波动不代表长期趋势",
            ),
            updatedAt = quote.updatedAt.ifBlank { "腾讯行情实时数据" },
        )
    }
}

internal fun parseTencentQuotes(
    payload: String,
    definitions: List<StockDefinition> = STOCKS,
): List<StockQuote> {
    val definitionByApiCode = definitions.associateBy(StockDefinition::apiCode)
    val quotes = payload.lineSequence()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .mapNotNull { line ->
            val match = RESPONSE_PATTERN.matchEntire(line) ?: return@mapNotNull null
            val definition = definitionByApiCode[match.groupValues[1]] ?: return@mapNotNull null
            val fields = match.groupValues[2].split('~')
            parseQuote(definition, fields)
        }
        .toList()

    if (quotes.isEmpty()) throw TencentStockException(EMPTY_RESPONSE_MESSAGE)
    return definitions.mapNotNull { definition ->
        quotes.firstOrNull { quote -> quote.symbol == definition.symbol }
    }
}

private fun parseQuote(definition: StockDefinition, fields: List<String>): StockQuote? {
    val price = fields.decimalAt(3) ?: return null
    val previousClose = fields.decimalAt(4) ?: return null
    val open = fields.decimalAt(5) ?: previousClose
    val high = fields.decimalAt(33) ?: maxOf(price, open, previousClose)
    val low = fields.decimalAt(34) ?: minOf(price, open, previousClose)
    val change = fields.decimalAt(31) ?: price - previousClose
    val changePercent = fields.decimalAt(32) ?: if (previousClose == 0.0) {
        0.0
    } else {
        change / previousClose * 100.0
    }
    val trendPoints = listOf(previousClose, open, low, (high + low) / 2.0, high, price)
        .filter { point -> point > 0.0 }

    return StockQuote(
        symbol = definition.symbol,
        name = fields.getOrNull(1)?.takeIf(String::isNotBlank) ?: definition.fallbackName,
        exchange = definition.exchange,
        price = price,
        change = change,
        changePercent = changePercent,
        open = open,
        high = high,
        low = low,
        previousClose = previousClose,
        volume = fields.decimalAt(6)?.toLong() ?: 0L,
        trendPoints = trendPoints,
        updatedAt = fields.getOrNull(30).orEmpty(),
    )
}

internal data class StockDefinition(
    val apiCode: String,
    val symbol: String,
    val exchange: String,
    val fallbackName: String,
)

private fun List<String>.decimalAt(index: Int): Double? =
    getOrNull(index)?.trim()?.toDoubleOrNull()

private fun StockQuote.intradayAmplitude(): String =
    (if (previousClose == 0.0) 0.0 else (high - low) / previousClose * 100.0).format(2)

private fun Double.absoluteValue(): Double = if (this < 0) -this else this

private fun Double.format(decimalPlaces: Int): String {
    val scale = 10.0.pow(decimalPlaces)
    val rounded = kotlin.math.round(this * scale) / scale
    return rounded.toString().let { value ->
        val parts = value.split('.')
        val fraction = parts.getOrElse(1) { "" }.padEnd(decimalPlaces, '0').take(decimalPlaces)
        if (decimalPlaces == 0) parts[0] else "${parts[0]}.$fraction"
    }
}

private fun Double.pow(exponent: Int): Double {
    var result = 1.0
    repeat(exponent) { result *= this }
    return result
}

private fun normalizeSymbol(symbol: String): String = symbol.trim().uppercase()

class TencentStockException(message: String) : IllegalStateException(message)

private val RESPONSE_PATTERN = Regex("""v_([A-Za-z0-9]+)=\"(.*)\";""")
private const val EMPTY_RESPONSE_MESSAGE = "腾讯行情暂未返回有效数据，请稍后重试"

internal val STOCKS = listOf(
    StockDefinition("sz000001", "000001", "SZSE", "平安银行"),
    StockDefinition("sh600519", "600519", "SSE", "贵州茅台"),
    StockDefinition("hk00700", "00700", "HKEX", "腾讯控股"),
    StockDefinition("hk09988", "09988", "HKEX", "阿里巴巴-W"),
    StockDefinition("usAAPL", "AAPL", "NASDAQ", "苹果"),
    StockDefinition("usTSLA", "TSLA", "NASDAQ", "特斯拉"),
)
