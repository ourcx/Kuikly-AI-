package com.ourcx.kuiklystock.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class ChatMessage(
    val id: String,
    val role: ChatRole,
    val blocks: List<ChatContentBlock>,
    val status: ChatMessageStatus,
    val retryQuestion: String? = null,
)

sealed interface ChatContentBlock {
    data class Markdown(val text: String) : ChatContentBlock

    data class StockCard(val symbol: String) : ChatContentBlock

    data class Trend(val symbol: String) : ChatContentBlock
}

enum class ChatRole {
    USER,
    ASSISTANT,
}

enum class ChatMessageStatus {
    COMPLETE,
    GENERATING,
    FAILED,
}

enum class WorkBuddyConnectionStatus {
    UNCONFIGURED,
    AVAILABLE,
    SENDING,
    ERROR,
}

@Serializable
data class ChatRequest(
    val question: String,
    @SerialName("conversation_id")
    val conversationId: String? = null,
    val context: ChatContext = ChatContext(),
)

@Serializable
data class ChatContext(
    val quotes: List<ChatQuoteContext> = emptyList(),
)

@Serializable
data class ChatQuoteContext(
    val symbol: String,
    val name: String,
    val exchange: String,
    val price: Double,
    val change: Double,
    @SerialName("change_percent")
    val changePercent: Double,
)

@Serializable
data class ChatResponse(
    val answer: String,
    @SerialName("conversation_id")
    val conversationId: String? = null,
    val symbols: List<String> = emptyList(),
    @SerialName("show_trend")
    val showTrend: Boolean = false,
)
