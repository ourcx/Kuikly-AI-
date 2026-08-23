package com.ourcx.kuiklystock.presentation

import com.ourcx.kuiklystock.data.ChatRepository
import com.ourcx.kuiklystock.data.InMemoryChatRepository
import com.ourcx.kuiklystock.data.InMemoryStockRepository
import com.ourcx.kuiklystock.data.StockRepository
import com.ourcx.kuiklystock.domain.AppDestination
import com.ourcx.kuiklystock.domain.AppTab
import com.ourcx.kuiklystock.domain.StockDetailState
import com.ourcx.kuiklystock.domain.StockHomeState

class StockHomeController(
    stockRepository: StockRepository = InMemoryStockRepository(),
    chatRepository: ChatRepository = InMemoryChatRepository(),
    private val onStateChanged: (StockHomeState) -> Unit = {},
) {
    var state: StockHomeState = StockHomeState()
        private set

    val marketController = MarketController(stockRepository) { marketState ->
        updateState(state.copy(market = marketState))
    }

    val chatController = ChatController(chatRepository) { chatState ->
        updateState(state.copy(chat = chatState))
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
        val selectedDetail = marketController.selectStock(symbol)
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
}
