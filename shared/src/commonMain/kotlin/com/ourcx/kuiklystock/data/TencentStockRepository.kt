package com.ourcx.kuiklystock.data

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

}

internal fun parseTencentQuotes(
    payload: String,
    definitions: List<StockDefinition> = STOCKS,
): List<StockQuote> {
    val definitionByApiCode = definitions.associateBy(StockDefinition::apiCode)
    val quotesByApiCode = payload.lineSequence()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .mapNotNull { line -> RESPONSE_PATTERN.matchEntire(line) }
        .filter { match -> match.groupValues[1] in definitionByApiCode }
        .groupBy { match -> match.groupValues[1] }

    return definitions.map { definition ->
        val matches = quotesByApiCode[definition.apiCode].orEmpty()
        if (matches.size != 1) throw TencentStockException(INCOMPLETE_RESPONSE_MESSAGE)
        val quote = matches.single().let { match ->
            val fields = match.groupValues[2].split('~')
            parseQuote(definition, fields)
        } ?: throw TencentStockException(INCOMPLETE_RESPONSE_MESSAGE)
        quote
    }
}

private fun parseQuote(definition: StockDefinition, fields: List<String>): StockQuote? {
    val returnedSymbol = fields.getOrNull(2)?.trim().orEmpty().substringBefore('.')
    if (!returnedSymbol.equals(definition.symbol, ignoreCase = true)) return null
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
    getOrNull(index)?.trim()?.toDoubleOrNull()?.takeIf { value -> value.isFinite() }

private fun normalizeSymbol(symbol: String): String = symbol.trim().uppercase()

class TencentStockException(message: String) : IllegalStateException(message)

private val RESPONSE_PATTERN = Regex("""v_([A-Za-z0-9]+)=\"(.*)\";""")
private const val INCOMPLETE_RESPONSE_MESSAGE = "腾讯行情返回不完整，请稍后重试"

internal val STOCKS = listOf(
    StockDefinition("sz000001", "000001", "SZSE", "平安银行"),
    StockDefinition("sh600519", "600519", "SSE", "贵州茅台"),
    StockDefinition("hk00700", "00700", "HKEX", "腾讯控股"),
    StockDefinition("hk09988", "09988", "HKEX", "阿里巴巴-W"),
    StockDefinition("usAAPL", "AAPL", "NASDAQ", "苹果"),
    StockDefinition("usTSLA", "TSLA", "NASDAQ", "特斯拉"),
)
