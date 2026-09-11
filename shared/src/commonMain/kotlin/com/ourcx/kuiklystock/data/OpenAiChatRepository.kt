package com.ourcx.kuiklystock.data

import com.ourcx.kuiklystock.domain.ChatProvider
import com.ourcx.kuiklystock.domain.ChatRequest
import com.ourcx.kuiklystock.domain.ChatResponse
import kotlinx.serialization.Serializable
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
) : ChatRepository {
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

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
        const val UNCONFIGURED_ERROR = "OpenAI 服务尚未配置"
        const val ENCODING_ERROR = "无法生成 OpenAI 请求"
        const val REQUEST_ERROR = "OpenAI 请求失败"
        const val DECODING_ERROR = "无法解析 OpenAI 响应"
        const val EMPTY_ANSWER_ERROR = "OpenAI 未返回分析内容"
    }
}

@Serializable
private data class OpenAiResponsesRequest(
    val model: String,
    val instructions: String,
    val input: String,
    val previous_response_id: String? = null,
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

class OpenAiChatException(message: String, cause: Throwable? = null) : IllegalStateException(message, cause)

private const val DEFAULT_OPENAI_MODEL = "gpt-5.6"
private const val SYSTEM_INSTRUCTIONS = """
你是谨慎、客观的股票研究助手。只能基于用户问题和提供的实时行情上下文作答；
明确区分事实、推断和未知信息，不编造新闻或基本面数据。使用简洁中文 Markdown，
包含行情观察、可能信号、风险提示，并声明内容不构成投资建议。
"""
