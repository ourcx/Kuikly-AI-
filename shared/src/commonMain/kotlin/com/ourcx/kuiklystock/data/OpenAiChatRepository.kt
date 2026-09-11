package com.ourcx.kuiklystock.data

import com.ourcx.kuiklystock.domain.ChatProvider
import com.ourcx.kuiklystock.domain.ChatRequest
import com.ourcx.kuiklystock.domain.ChatResponse
import com.ourcx.kuiklystock.domain.StockInsight
import com.ourcx.kuiklystock.domain.StockQuote
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/** OpenAI Responses API client that delegates credential handling to an HTTPS backend proxy. */
class OpenAiChatRepository(
    private val configurationProvider: () -> Boolean,
    private val modelProvider: () -> String,
    private val requestInvoker: (payload: String, callback: (Result<String>) -> Unit) -> Unit,
) : ChatRepository, InsightRepository {
    override val isConfigured: Boolean
        get() = configurationProvider()

    override fun ask(request: ChatRequest, callback: (Result<ChatResponse>) -> Unit) {
        if (!isConfigured) {
            callback(Result.failure(OpenAiChatException(UNCONFIGURED_ERROR)))
            return
        }
        val payload = runCatching { json.encodeToString(request.toOpenAiRequest(modelProvider())) }
            .getOrElse { error ->
                callback(Result.failure(OpenAiChatException(ENCODING_ERROR, error)))
                return
            }

        try {
            requestInvoker(payload) { nativeResult ->
                callback(
                    nativeResult.fold(
                        onSuccess = { responseJson -> decodeResponse(request, responseJson) },
                        onFailure = { error -> Result.failure(OpenAiChatException(REQUEST_ERROR, error)) },
                    ),
                )
            }
        } catch (error: Throwable) {
            callback(Result.failure(OpenAiChatException(REQUEST_ERROR, error)))
        }
    }

    override fun getInsight(quote: StockQuote, callback: (Result<StockInsight>) -> Unit) {
        if (!isConfigured) {
            callback(Result.failure(OpenAiChatException(UNCONFIGURED_ERROR)))
            return
        }
        val payload = runCatching { json.encodeToString(quote.toOpenAiInsightRequest(modelProvider())) }
            .getOrElse { error ->
                callback(Result.failure(OpenAiChatException(ENCODING_ERROR, error)))
                return
            }

        try {
            requestInvoker(payload) { nativeResult ->
                callback(
                    nativeResult.fold(
                        onSuccess = { responseJson -> decodeInsight(quote, responseJson) },
                        onFailure = { error -> Result.failure(OpenAiChatException(REQUEST_ERROR, error)) },
                    ),
                )
            }
        } catch (error: Throwable) {
            callback(Result.failure(OpenAiChatException(REQUEST_ERROR, error)))
        }
    }

    private fun decodeResponse(request: ChatRequest, responseJson: String): Result<ChatResponse> = runCatching {
        val response = json.parseToJsonElement(responseJson) as? JsonObject
            ?: throw OpenAiChatException(DECODING_ERROR)
        val answer = response.extractOutputText().takeIf(String::isNotBlank)
            ?: throw OpenAiChatException(EMPTY_ANSWER_ERROR)
        val referencedSymbols = request.context.quotes
            .filter { quote -> answer.contains(quote.symbol, ignoreCase = true) || request.question.contains(quote.symbol, ignoreCase = true) }
            .map { quote -> quote.symbol }
            .distinct()
        ChatResponse(
            answer = answer,
            conversationId = response["id"]?.jsonPrimitive?.content,
            symbols = referencedSymbols,
            showTrend = referencedSymbols.isNotEmpty(),
            provider = ChatProvider.OPENAI,
        )
    }

    private fun decodeInsight(quote: StockQuote, responseJson: String): Result<StockInsight> = runCatching {
        val response = json.parseToJsonElement(responseJson) as? JsonObject
            ?: throw OpenAiChatException(DECODING_ERROR)
        val outputText = response.extractOutputText().takeIf(String::isNotBlank)
            ?: throw OpenAiChatException(EMPTY_ANSWER_ERROR)
        val payload = json.decodeFromString<OpenAiInsightPayload>(outputText.removeJsonFence())
        if (payload.trendLabel.isBlank() || payload.summary.isBlank() || payload.signals.isEmpty() || payload.risks.isEmpty()) {
            throw OpenAiChatException(INCOMPLETE_INSIGHT_ERROR)
        }
        StockInsight(
            symbol = quote.symbol,
            trendLabel = payload.trendLabel,
            summary = payload.summary,
            signals = payload.signals,
            risks = payload.risks,
            updatedAt = quote.updatedAt.ifBlank { "腾讯行情实时数据" },
        )
    }

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
        const val UNCONFIGURED_ERROR = "OpenAI 服务尚未配置"
        const val ENCODING_ERROR = "无法生成 OpenAI 请求"
        const val REQUEST_ERROR = "OpenAI 请求失败"
        const val DECODING_ERROR = "无法解析 OpenAI 响应"
        const val EMPTY_ANSWER_ERROR = "OpenAI 未返回分析内容"
        const val INCOMPLETE_INSIGHT_ERROR = "OpenAI 返回的洞察内容不完整"
    }
}

@Serializable
private data class OpenAiResponsesRequest(
    val model: String,
    val instructions: String,
    val input: String,
    val previous_response_id: String? = null,
)

@Serializable
private data class OpenAiInsightPayload(
    @SerialName("trend_label") val trendLabel: String,
    val summary: String,
    val signals: List<String>,
    val risks: List<String>,
)

private fun ChatRequest.toOpenAiRequest(model: String): OpenAiResponsesRequest {
    val quoteContext = context.quotes.joinToString(separator = "\n") { quote ->
        "${quote.name}(${quote.symbol}, ${quote.exchange})：最新价 ${quote.price}，涨跌 ${quote.change}，涨跌幅 ${quote.changePercent}%"
    }.ifBlank { "当前没有可用行情数据。" }
    return OpenAiResponsesRequest(
        model = model.ifBlank { DEFAULT_OPENAI_MODEL },
        instructions = SYSTEM_INSTRUCTIONS,
        input = "用户问题：$question\n\n腾讯实时行情上下文：\n$quoteContext",
        previous_response_id = conversationId,
    )
}

private fun StockQuote.toOpenAiInsightRequest(model: String): OpenAiResponsesRequest = OpenAiResponsesRequest(
    model = model.ifBlank { DEFAULT_OPENAI_MODEL },
    instructions = INSIGHT_INSTRUCTIONS,
    input = """
        请分析以下腾讯实时行情事实：
        名称：$name
        代码：$symbol
        交易所：$exchange
        最新价：$price
        涨跌额：$change
        涨跌幅：$changePercent%
        开盘价：$open
        最高价：$high
        最低价：$low
        前收盘价：$previousClose
        成交量：$volume
        行情时间：$updatedAt
    """.trimIndent(),
)

private fun JsonObject.extractOutputText(): String {
    this["output_text"]?.jsonPrimitive?.content?.let { return it }
    val output = this["output"] as? JsonArray ?: return ""
    return output.flatMap { item ->
        ((item as? JsonObject)?.get("content") as? JsonArray).orEmpty()
    }.mapNotNull { content ->
        val contentObject = content as? JsonObject ?: return@mapNotNull null
        if (contentObject["type"]?.jsonPrimitive?.content != "output_text") return@mapNotNull null
        contentObject["text"]?.jsonPrimitive?.content
    }.joinToString("\n")
}

private fun String.removeJsonFence(): String {
    val trimmed = trim()
    if (!trimmed.startsWith("```")) return trimmed
    return trimmed.lines().drop(1).dropLast(1).joinToString("\n").trim()
}

class OpenAiChatException(message: String, cause: Throwable? = null) : IllegalStateException(message, cause)

private const val DEFAULT_OPENAI_MODEL = "gpt-5.6"
private const val SYSTEM_INSTRUCTIONS = """
你是谨慎、客观的股票研究助手。只能基于用户问题和提供的实时行情上下文作答；
明确区分事实、推断和未知信息，不编造新闻或基本面数据。使用简洁中文 Markdown，
包含行情观察、可能信号、风险提示，并声明内容不构成投资建议。
"""
private const val INSIGHT_INSTRUCTIONS = """
你是谨慎、客观的股票研究助手。仅根据提供的腾讯实时行情事实生成洞察，不得编造新闻、基本面或预测。
只返回严格 JSON，不要使用 Markdown 代码块，格式必须是：
{"trend_label":"简短趋势标签","summary":"客观摘要","signals":["信号"],"risks":["风险"]}
signals 和 risks 均至少包含一项，风险中需说明内容不构成投资建议。
"""
