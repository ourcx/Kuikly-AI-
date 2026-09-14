package com.ourcx.kuiklystock.data

import com.ourcx.kuiklystock.domain.StockQuote
import com.ourcx.kuiklystock.domain.QuoteDataSource

/** Production stock source backed by Tencent's multi-market quote endpoint. */
class TencentStockRepository(
    initialCustomCodes: List<String> = emptyList(),
    private val onCustomCodesChanged: (List<String>) -> Unit = {},
    private val requestInvoker: (codes: List<String>, callback: (Result<String>) -> Unit) -> Unit,
) : StockRepository {
    private var quotesBySymbol: Map<String, StockQuote> = emptyMap()
    private val customCodes = initialCustomCodes.mapNotNull(::parseUserStockDefinition)
        .map(StockDefinition::apiCode)
        .distinct()
        .toMutableList()
    private val stockDefinitions = (
        customCodes.mapNotNull(::definitionFromApiCode) + STOCKS
    ).distinctBy(StockDefinition::apiCode).toMutableList()
    override val directory: List<StockDirectoryEntry>
        get() = stockDefinitions.map { definition ->
            StockDirectoryEntry(definition.symbol, definition.fallbackName, definition.exchange)
        }

    override fun getQuotes(callback: (Result<List<StockQuote>>) -> Unit) {
        getQuotesPage(offset = 0, limit = stockDefinitions.size, callback = callback)
    }

    override fun getQuotesPage(offset: Int, limit: Int, callback: (Result<List<StockQuote>>) -> Unit) {
        if (offset < 0 || limit <= 0) {
            callback(Result.failure(IllegalArgumentException("Invalid quote page")))
            return
        }
        val definitions = stockDefinitions.drop(offset).take(limit)
        if (definitions.isEmpty()) {
            callback(Result.success(emptyList()))
            return
        }
        requestInvoker(definitions.map(StockDefinition::apiCode)) { result ->
            callback(
                result.mapCatching { payload ->
                    parseTencentQuotes(payload, definitions).also { quotes ->
                        quotesBySymbol = quotesBySymbol + quotes.associateBy { quote -> normalizeSymbol(quote.symbol) }
                    }
                },
            )
        }
    }

    override fun getQuote(symbol: String): StockQuote =
        quotesBySymbol[normalizeSymbol(symbol)] ?: throw StockNotFoundException(symbol)

    override fun addQuote(symbol: String, callback: (Result<StockQuote>) -> Unit) {
        val definition = parseUserStockDefinition(symbol)
        if (definition == null) {
            callback(Result.failure(IllegalArgumentException(CUSTOM_SYMBOL_FORMAT_MESSAGE)))
            return
        }
        requestInvoker(listOf(definition.apiCode)) { result ->
            callback(
                result.mapCatching { payload ->
                    val quote = parseTencentQuotes(payload, listOf(definition)).single()
                    val existingIndex = stockDefinitions.indexOfFirst { item -> item.apiCode == definition.apiCode }
                    if (existingIndex < 0) {
                        stockDefinitions.add(0, definition.copy(fallbackName = quote.name))
                        customCodes.add(0, definition.apiCode)
                        onCustomCodesChanged(customCodes.distinct())
                    }
                    quotesBySymbol = quotesBySymbol + (normalizeSymbol(quote.symbol) to quote)
                    quote
                },
            )
        }
    }

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
        dataSource = QuoteDataSource.TENCENT,
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

internal fun parseUserStockDefinition(input: String): StockDefinition? {
    val normalized = input.trim().replace(" ", "").uppercase()
    if (normalized.isEmpty()) return null
    val explicit = EXPLICIT_STOCK_PATTERN.matchEntire(normalized)
    if (explicit != null) {
        return createStockDefinition(explicit.groupValues[1].lowercase(), explicit.groupValues[2])
    }
    return when {
        normalized.matches(Regex("^[0-9]{5}$")) -> createStockDefinition("hk", normalized)
        normalized.matches(Regex("^[0-9]{6}$")) -> {
            val prefix = if (normalized.first() in setOf('5', '6', '9')) "sh" else "sz"
            createStockDefinition(prefix, normalized)
        }
        normalized.matches(Regex("^[A-Z][A-Z0-9]{0,9}$")) -> createStockDefinition("us", normalized)
        else -> null
    }
}

private fun definitionFromApiCode(apiCode: String): StockDefinition? {
    val match = EXPLICIT_STOCK_PATTERN.matchEntire(apiCode.uppercase()) ?: return null
    return createStockDefinition(match.groupValues[1].lowercase(), match.groupValues[2])
}

private fun createStockDefinition(prefix: String, rawSymbol: String): StockDefinition? {
    val symbol = rawSymbol.uppercase()
    val valid = when (prefix) {
        "sh", "sz" -> symbol.matches(Regex("^[0-9]{6}$"))
        "hk" -> symbol.matches(Regex("^[0-9]{5}$"))
        "us" -> symbol.matches(Regex("^[A-Z][A-Z0-9]{0,9}$"))
        else -> false
    }
    if (!valid) return null
    val exchange = when (prefix) {
        "sh" -> "SSE"
        "sz" -> "SZSE"
        "hk" -> "HKEX"
        else -> "NASDAQ"
    }
    return StockDefinition("$prefix$symbol", symbol, exchange, symbol)
}

class TencentStockException(message: String) : IllegalStateException(message)

private val RESPONSE_PATTERN = Regex("""v_([A-Za-z0-9]+)=\"(.*)\";""")
private const val INCOMPLETE_RESPONSE_MESSAGE = "腾讯行情返回不完整，请稍后重试"
private const val CUSTOM_SYMBOL_FORMAT_MESSAGE = "请输入 A 股 6 位代码、港股 5 位代码或美股代码"
private val EXPLICIT_STOCK_PATTERN = Regex("^(SH|SZ|HK|US)([A-Z0-9]{1,12})$")

internal val STOCKS = listOf(
    StockDefinition("sz000001", "000001", "SZSE", "平安银行"),
    StockDefinition("hk00700", "00700", "HKEX", "腾讯控股"),
    StockDefinition("usAAPL", "AAPL", "NASDAQ", "苹果"),
    StockDefinition("sh600519", "600519", "SSE", "贵州茅台"),
    StockDefinition("hk09988", "09988", "HKEX", "阿里巴巴-W"),
    StockDefinition("usTSLA", "TSLA", "NASDAQ", "特斯拉"),
    StockDefinition("sh600036", "600036", "SSE", "招商银行"),
    StockDefinition("hk03690", "03690", "HKEX", "美团-W"),
    StockDefinition("usMSFT", "MSFT", "NASDAQ", "微软"),
    StockDefinition("sh601318", "601318", "SSE", "中国平安"),
    StockDefinition("hk00941", "00941", "HKEX", "中国移动"),
    StockDefinition("usNVDA", "NVDA", "NASDAQ", "英伟达"),
    StockDefinition("sz300750", "300750", "SZSE", "宁德时代"),
    StockDefinition("hk01299", "01299", "HKEX", "友邦保险"),
    StockDefinition("usAMZN", "AMZN", "NASDAQ", "亚马逊"),
    StockDefinition("sh600900", "600900", "SSE", "长江电力"),
    StockDefinition("hk01810", "01810", "HKEX", "小米集团-W"),
    StockDefinition("usGOOGL", "GOOGL", "NASDAQ", "谷歌-A"),
)
