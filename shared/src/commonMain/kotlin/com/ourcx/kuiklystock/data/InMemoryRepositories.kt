package com.ourcx.kuiklystock.data

import com.ourcx.kuiklystock.domain.ChatProvider
import com.ourcx.kuiklystock.domain.ChatRequest
import com.ourcx.kuiklystock.domain.ChatResponse
import com.ourcx.kuiklystock.domain.ChatQuoteContext
import com.ourcx.kuiklystock.domain.StockInsight
import com.ourcx.kuiklystock.domain.StockQuote

/** Deterministic, dependency-free stock data source for previews and controller tests. */
class InMemoryStockRepository : StockRepository, InsightRepository {
    override fun getQuotes(callback: (Result<List<StockQuote>>) -> Unit) {
        callback(Result.success(FIXTURE_QUOTES))
    }

    override fun getQuote(symbol: String): StockQuote =
        QUOTES_BY_SYMBOL[normalizeSymbol(symbol)] ?: throw StockNotFoundException(symbol)

    override fun getInsight(quote: StockQuote, callback: (Result<StockInsight>) -> Unit) {
        callback(
            INSIGHTS_BY_SYMBOL[normalizeSymbol(quote.symbol)]
                ?.let(Result.Companion::success)
                ?: Result.success(quote.toFallbackInsight()),
        )
    }
}

private fun StockQuote.toFallbackInsight(): StockInsight {
    val trend = when {
        changePercent > 1.0 -> "偏强运行"
        changePercent < -1.0 -> "偏弱运行"
        else -> "区间震荡"
    }
    val location = when {
        price >= high -> "最新价位于日内高位附近"
        price <= low -> "最新价位于日内低位附近"
        else -> "最新价位于日内高低区间之间"
    }
    return StockInsight(
        symbol = symbol,
        trendLabel = trend,
        summary = "$location，当前较昨收${if (change >= 0.0) "上涨" else "下跌"}。",
        signals = listOf(
            "今开 $open，最新 $price",
            "日内区间 $low 至 $high",
        ),
        risks = listOf("快照数据不能替代完整分时走势", "内容仅供演示，不构成投资建议"),
        updatedAt = updatedAt.ifBlank { "离线演示数据" },
    )
}

/** Deterministic AI response source; questions containing `失败` exercise the retry path. */
class InMemoryChatRepository : ChatRepository {
    override val isConfigured: Boolean = true

    override fun ask(request: ChatRequest, callback: (Result<ChatResponse>) -> Unit) {
        callback(runCatching { createResponse(request) })
    }

    private fun createResponse(request: ChatRequest): ChatResponse {
        val normalizedQuestion = request.question.trim()
        if (normalizedQuestion.contains(FAILURE_KEYWORD)) {
            throw ChatFixtureException(DEMONSTRATION_FAILURE_MESSAGE)
        }

        val contextQuote = request.context.quotes.firstOrNull { quote ->
            normalizedQuestion.uppercase().contains(quote.symbol.uppercase()) || normalizedQuestion.contains(quote.name)
        }
        val matchedSymbol = contextQuote?.symbol ?: findMentionedSymbol(normalizedQuestion) ?: DEFAULT_CHAT_SYMBOL
        val quote = contextQuote ?: QUOTES_BY_SYMBOL.getValue(matchedSymbol).toChatQuoteContext()
        val answer = buildString {
            appendLine("## ${quote.name}（${quote.symbol}）行情速览")
            appendLine()
            appendLine("- 最新价：${quote.price}")
            appendLine("- 涨跌幅：${quote.changePercent}%")
            appendLine("- 观察：${quote.localObservation()}")
            appendLine()
            appendLine("> 内容仅供演示，不构成投资建议")
        }
        return ChatResponse(
            answer = answer,
            conversationId = request.conversationId,
            symbols = listOf(matchedSymbol),
            showTrend = true,
            provider = ChatProvider.LOCAL,
        )
    }

    private fun findMentionedSymbol(question: String): String? {
        val normalizedQuestion = question.uppercase()
        return FIXTURE_QUOTES.firstOrNull { quote ->
            normalizedQuestion.contains(quote.symbol) || question.contains(quote.name)
        }?.symbol
    }
}

private fun StockQuote.toChatQuoteContext(): ChatQuoteContext = ChatQuoteContext(
    symbol = symbol,
    name = name,
    exchange = exchange,
    price = price,
    change = change,
    changePercent = changePercent,
)

private fun ChatQuoteContext.localObservation(): String = when {
    changePercent > 1.0 -> "价格相对昨收明显走强，需继续观察波动与成交承接。"
    changePercent < -1.0 -> "价格相对昨收明显走弱，需关注下行风险。"
    else -> "价格围绕昨收窄幅波动，当前方向信号有限。"
}

private fun normalizeSymbol(symbol: String): String = symbol.trim().uppercase()

private const val DEFAULT_CHAT_SYMBOL = "00700"
private const val FAILURE_KEYWORD = "失败"
private const val DEMONSTRATION_FAILURE_MESSAGE = "演示聊天服务暂时不可用，请重试"

private val FIXTURE_QUOTES = listOf(
    StockQuote(
        symbol = "00700",
        name = "腾讯控股",
        exchange = "HKEX",
        price = 382.40,
        change = 5.80,
        changePercent = 1.54,
        open = 377.80,
        high = 384.20,
        low = 376.60,
        previousClose = 376.60,
        volume = 18_426_300,
        trendPoints = listOf(376.60, 378.20, 377.40, 380.10, 379.60, 381.80, 382.40),
    ),
    StockQuote(
        symbol = "09988",
        name = "阿里巴巴-W",
        exchange = "HKEX",
        price = 82.15,
        change = -0.85,
        changePercent = -1.02,
        open = 83.20,
        high = 83.55,
        low = 81.70,
        previousClose = 83.00,
        volume = 42_105_700,
        trendPoints = listOf(83.00, 83.20, 82.90, 82.45, 82.70, 81.95, 82.15),
    ),
    StockQuote(
        symbol = "600519",
        name = "贵州茅台",
        exchange = "SSE",
        price = 1_438.20,
        change = 12.60,
        changePercent = 0.88,
        open = 1_426.00,
        high = 1_445.80,
        low = 1_421.50,
        previousClose = 1_425.60,
        volume = 3_218_900,
        trendPoints = listOf(1_425.60, 1_429.10, 1_424.80, 1_433.50, 1_440.20, 1_436.70, 1_438.20),
    ),
    StockQuote(
        symbol = "AAPL",
        name = "苹果",
        exchange = "NASDAQ",
        price = 227.16,
        change = 2.44,
        changePercent = 1.09,
        open = 224.82,
        high = 228.34,
        low = 223.91,
        previousClose = 224.72,
        volume = 48_763_200,
        trendPoints = listOf(224.72, 225.30, 224.95, 226.40, 225.88, 227.62, 227.16),
    ),
    StockQuote(
        symbol = "TSLA",
        name = "特斯拉",
        exchange = "NASDAQ",
        price = 248.98,
        change = -3.56,
        changePercent = -1.41,
        open = 253.10,
        high = 255.24,
        low = 247.30,
        previousClose = 252.54,
        volume = 93_547_600,
        trendPoints = listOf(252.54, 253.80, 251.20, 254.10, 250.35, 247.90, 248.98),
    ),
)

private val FIXTURE_INSIGHTS = listOf(
    StockInsight(
        symbol = "00700",
        trendLabel = "温和上行",
        summary = "价格在日内区间上沿附近运行，短线动能偏强。",
        signals = listOf("最新价高于开盘价", "日内低点逐步抬升"),
        risks = listOf("互联网板块波动可能放大", "需留意成交量变化"),
        updatedAt = "2026-08-23 15:30 HKT",
    ),
    StockInsight(
        symbol = "09988",
        trendLabel = "区间整理",
        summary = "股价低于前收，当前仍处于窄幅整理阶段。",
        signals = listOf("日内振幅有限", "尾盘较低点回升"),
        risks = listOf("消费预期变化", "平台经济政策扰动"),
        updatedAt = "2026-08-23 15:30 HKT",
    ),
    StockInsight(
        symbol = "600519",
        trendLabel = "震荡偏强",
        summary = "价格较前收温和上行，高位仍有一定抛压。",
        signals = listOf("最新价高于前收", "盘中触及阶段高点"),
        risks = listOf("高端消费需求波动", "估值水平变化"),
        updatedAt = "2026-08-23 15:00 CST",
    ),
    StockInsight(
        symbol = "AAPL",
        trendLabel = "稳步回升",
        summary = "价格延续日内回升，仍需观察高位成交承接。",
        signals = listOf("收复开盘价", "接近日内高点"),
        risks = listOf("硬件需求不确定性", "汇率与供应链波动"),
        updatedAt = "2026-08-23 16:00 ET",
    ),
    StockInsight(
        symbol = "TSLA",
        trendLabel = "宽幅震荡",
        summary = "价格较前收回落，盘中波动显著高于其他样本。",
        signals = listOf("最新价低于开盘价", "低位出现小幅承接"),
        risks = listOf("交付数据波动", "价格竞争与利润率压力"),
        updatedAt = "2026-08-23 16:00 ET",
    ),
)

private val QUOTES_BY_SYMBOL = FIXTURE_QUOTES.associateBy(StockQuote::symbol)
private val INSIGHTS_BY_SYMBOL = FIXTURE_INSIGHTS.associateBy(StockInsight::symbol)
