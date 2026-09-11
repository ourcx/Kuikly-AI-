package com.ourcx.kuiklystock.domain

enum class AppTab {
    MARKET,
    AI,
}

enum class MarketFilter {
    ALL,
    HK,
    CN,
    US,
}

enum class MarketSort {
    DEFAULT,
    GAINERS,
    LOSERS,
    VOLATILITY,
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
    val query: String = "",
    val filter: MarketFilter = MarketFilter.ALL,
    val sort: MarketSort = MarketSort.DEFAULT,
    val favoriteSymbols: Set<String> = emptySet(),
    val recentQuotes: List<StockQuote> = emptyList(),
    val favoritesOnly: Boolean = false,
    val totalCount: Int = 0,
)

data class StockDetailContent(
    val quote: StockQuote,
    val insight: LoadState<StockInsight> = LoadState.Loading,
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
    val provider: ChatProvider = ChatProvider.OPENAI,
    val serviceSettingsVisible: Boolean = false,
    val serviceUrlDraft: String = "",
    val serviceTokenDraft: String = "",
    val serviceModelDraft: String = "",
    val serviceTokenConfigured: Boolean = false,
    val serviceSettingsError: String? = null,
)

data class StockHomeState(
    val selectedTab: AppTab = AppTab.MARKET,
    val destination: AppDestination = AppDestination.Home,
    val detail: StockDetailState? = null,
    val market: MarketState = MarketState(),
    val chat: ChatState = ChatState(),
)
