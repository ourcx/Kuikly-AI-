package com.ourcx.kuiklystock.presentation

import com.ourcx.kuiklystock.data.InMemoryStockRepository
import com.ourcx.kuiklystock.data.StockNotFoundException
import com.ourcx.kuiklystock.data.StockRepository
import com.ourcx.kuiklystock.domain.LoadState
import com.ourcx.kuiklystock.domain.MarketState
import com.ourcx.kuiklystock.domain.StockDetailContent
import com.ourcx.kuiklystock.domain.StockDetailState

enum class MarketDemoState {
    CONTENT,
    EMPTY,
    ERROR,
    LOADING,
}

class MarketController(
    private val stockRepository: StockRepository = InMemoryStockRepository(),
    private val onStateChanged: (MarketState) -> Unit = {},
) {
    var state: MarketState = MarketState()
        private set

    fun load() {
        updateState(MarketState(LoadState.Loading))
        state = runCatching { stockRepository.getQuotes() }
            .fold(
                onSuccess = { quotes ->
                    MarketState(
                        if (quotes.isEmpty()) LoadState.Empty else LoadState.Content(quotes),
                    )
                },
                onFailure = { error -> MarketState(LoadState.Error(error.readableMessage())) },
            )
        onStateChanged(state)
    }

    fun showDemoState(demoState: MarketDemoState) {
        when (demoState) {
            MarketDemoState.CONTENT -> load()
            MarketDemoState.EMPTY -> updateState(MarketState(LoadState.Empty))
            MarketDemoState.ERROR -> updateState(MarketState(LoadState.Error(DEMO_ERROR_MESSAGE)))
            MarketDemoState.LOADING -> updateState(MarketState(LoadState.Loading))
        }
    }

    fun retry() = load()

    fun selectStock(symbol: String): StockDetailState {
        val normalizedSymbol = symbol.trim()
        if (normalizedSymbol.isEmpty()) {
            return StockDetailState(
                symbol = normalizedSymbol,
                content = LoadState.Error(EMPTY_SYMBOL_MESSAGE),
            )
        }

        return runCatching {
            StockDetailContent(
                quote = stockRepository.getQuote(normalizedSymbol),
                insight = stockRepository.getInsight(normalizedSymbol),
            )
        }.fold(
            onSuccess = { content ->
                StockDetailState(
                    symbol = content.quote.symbol,
                    content = LoadState.Content(content),
                )
            },
            onFailure = { error ->
                StockDetailState(
                    symbol = normalizedSymbol,
                    content = LoadState.Error(error.readableMessage()),
                )
            },
        )
    }

    private fun updateState(newState: MarketState) {
        state = newState
        onStateChanged(newState)
    }
}

internal fun Throwable.readableMessage(): String = when (this) {
    is StockNotFoundException -> "未找到股票：$symbol"
    else -> message?.takeIf(String::isNotBlank) ?: DEFAULT_ERROR_MESSAGE
}

private const val DEMO_ERROR_MESSAGE = "行情加载失败，请重试"
private const val EMPTY_SYMBOL_MESSAGE = "股票代码不能为空"
private const val DEFAULT_ERROR_MESSAGE = "服务暂时不可用，请重试"
