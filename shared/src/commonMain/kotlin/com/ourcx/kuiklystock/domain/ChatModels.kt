package com.ourcx.kuiklystock.domain

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
