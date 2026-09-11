package com.ourcx.kuiklystock.presentation

import com.ourcx.kuiklystock.data.ChatRepository
import com.ourcx.kuiklystock.data.InMemoryChatRepository
import com.ourcx.kuiklystock.data.InMemoryStockRepository
import com.ourcx.kuiklystock.data.InsightRepository
import com.ourcx.kuiklystock.data.StockRepository
import com.ourcx.kuiklystock.data.ResearchServiceConfiguration
import com.ourcx.kuiklystock.domain.AppDestination
import com.ourcx.kuiklystock.domain.AppTab
import com.ourcx.kuiklystock.domain.ChatContext
import com.ourcx.kuiklystock.domain.ChatQuoteContext
import com.ourcx.kuiklystock.domain.LoadState
import com.ourcx.kuiklystock.domain.StockDetailState
import com.ourcx.kuiklystock.domain.StockHomeState
import com.ourcx.kuiklystock.domain.StockQuote

class StockHomeController(
    private val stockRepository: StockRepository = InMemoryStockRepository(),
    chatRepository: ChatRepository = InMemoryChatRepository(),
    insightRepository: InsightRepository? =
        (chatRepository as? InsightRepository) ?: (stockRepository as? InsightRepository),
    serviceConfiguration: ResearchServiceConfiguration? = null,
    private val onStateChanged: (StockHomeState) -> Unit = {},
) {
    var state: StockHomeState = StockHomeState()
        private set

    val marketController = MarketController(stockRepository, insightRepository) { marketState ->
        updateMarketState(marketState)
    }

    val chatController = ChatController(
        chatRepository = chatRepository,
        contextProvider = {
            ChatContext(
                quotes = marketController.completeQuotesSnapshot().map { quote ->
                    ChatQuoteContext(
                        symbol = quote.symbol,
                        name = quote.name,
                        exchange = quote.exchange,
                        price = quote.price,
                        change = quote.change,
                        changePercent = quote.changePercent,
                    )
                },
            )
        },
        onStateChanged = { chatState ->
            updateChatState(chatState)
        },
        serviceConfiguration = serviceConfiguration,
    )

    init {
        state = state.copy(chat = chatController.state)
    }

    fun selectTab(tab: AppTab) {
        updateState(
            state.copy(
                selectedTab = tab,
                destination = AppDestination.Home,
                detail = null,
            ),
        )
    }

    fun selectStock(symbol: String): StockDetailState {
        val selectedDetail = marketController.selectStock(symbol) { resolvedDetail ->
            val currentDestination = state.destination as? AppDestination.Detail
            if (currentDestination?.symbol == resolvedDetail.symbol) {
                updateState(state.copy(detail = resolvedDetail))
            }
        }
        updateState(
            state.copy(
                destination = AppDestination.Detail(
                    symbol = selectedDetail.symbol,
                    previousTab = state.selectedTab,
                ),
                detail = selectedDetail,
            ),
        )
        return selectedDetail
    }

    /**
     * Observable contract:
     * - Ignores blank symbols and missing quotes without changing tab, destination, detail, or chat state.
     * - For a resolved quote, navigates to AI home, clears detail, seeds a stock-specific analysis draft, then sends it.
     * - Preserves the latest aggregated home state while chat callbacks mutate only the chat slice.
     */
    fun askAiAboutStock(symbol: String) {
        val normalizedSymbol = symbol.trim()
        if (normalizedSymbol.isEmpty()) return

        val quote = runCatching { stockRepository.getQuote(normalizedSymbol) }
            .getOrNull() ?: return

        updateState(
            state.copy(
                selectedTab = AppTab.AI,
                destination = AppDestination.Home,
                detail = null,
            ),
        )

        chatController.updateDraft(
            buildAskAiQuestion(
                symbol = quote.symbol,
                name = quote.name,
            ),
        )
        chatController.send()
    }

    fun backFromDetail() {
        val detail = state.destination as? AppDestination.Detail ?: return
        updateState(
            state.copy(
                selectedTab = detail.previousTab,
                destination = AppDestination.Home,
                detail = null,
            ),
        )
    }

    private fun updateState(newState: StockHomeState) {
        state = newState
        onStateChanged(newState)
    }

    private fun updateMarketState(marketState: com.ourcx.kuiklystock.domain.MarketState) {
        updateState(state.copy(market = marketState))
    }

    private fun updateChatState(chatState: com.ourcx.kuiklystock.domain.ChatState) {
        updateState(state.copy(chat = chatState))
    }
}

private fun buildAskAiQuestion(symbol: String, name: String): String =
    "请分析股票$name（$symbol）的当前价格表现、短中期趋势、主要风险，以及接下来需要重点关注的信号。"
