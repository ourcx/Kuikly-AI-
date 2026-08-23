package com.ourcx.kuiklystock.domain

enum class AppTab {
    MARKET,
    AI,
}

sealed interface AppDestination {
    data object Home : AppDestination

    data class Detail(
        val symbol: String,
        val previousTab: AppTab,
    ) : AppDestination
}

data class MarketState(
    val quotes: LoadState<List<StockQuote>> = LoadState.Loading,
)

data class StockDetailContent(
    val quote: StockQuote,
    val insight: StockInsight,
)

data class StockDetailState(
    val symbol: String,
    val content: LoadState<StockDetailContent> = LoadState.Loading,
)

data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val draft: String = "",
    val isSending: Boolean = false,
    val error: String? = null,
    val conversationId: String? = null,
    val connectionStatus: WorkBuddyConnectionStatus = WorkBuddyConnectionStatus.UNCONFIGURED,
)

data class StockHomeState(
    val selectedTab: AppTab = AppTab.MARKET,
    val destination: AppDestination = AppDestination.Home,
    val detail: StockDetailState? = null,
    val market: MarketState = MarketState(),
    val chat: ChatState = ChatState(),
)
