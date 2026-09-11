package com.ourcx.kuiklystock.presentation

import com.ourcx.kuiklystock.data.InMemoryStockRepository
import com.ourcx.kuiklystock.data.StockNotFoundException
import com.ourcx.kuiklystock.data.StockRepository
import com.ourcx.kuiklystock.domain.LoadState
import com.ourcx.kuiklystock.domain.MarketFilter
import com.ourcx.kuiklystock.domain.MarketSort
import com.ourcx.kuiklystock.domain.MarketState
import com.ourcx.kuiklystock.domain.StockDetailContent
import com.ourcx.kuiklystock.domain.StockDetailState
import com.ourcx.kuiklystock.domain.StockQuote

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

    private var originalQuotesSnapshot: List<StockQuote> = emptyList()
    private var sourceStatus: MarketSourceStatus = MarketSourceStatus.LOADING

    /** Intent: Load quotes into a stable source snapshot and publish the derived market state under current discovery controls. */
    fun load() {
        sourceStatus = MarketSourceStatus.LOADING
        updateState(state.copy(quotes = LoadState.Loading, totalCount = 0))
        runCatching {
            stockRepository.getQuotes { result ->
                result.fold(
                onSuccess = { quotes ->
                    originalQuotesSnapshot = quotes
                    sourceStatus = if (quotes.isEmpty()) {
                        MarketSourceStatus.EMPTY
                    } else {
                        MarketSourceStatus.CONTENT
                    }
                    publishMarket()
                },
                onFailure = { error ->
                    sourceStatus = MarketSourceStatus.ERROR
                    updateState(
                        state.copy(
                            quotes = LoadState.Error(error.readableMessage()),
                            totalCount = 0,
                        ),
                    )
                },
                )
            }
        }.onFailure { error ->
            sourceStatus = MarketSourceStatus.ERROR
            updateState(
                state.copy(
                    quotes = LoadState.Error(error.readableMessage()),
                    totalCount = 0,
                ),
            )
        }
    }

    /** Intent: Keep the demo-state API compatible while preserving current discovery controls and total-count semantics. */
    fun showDemoState(demoState: MarketDemoState) {
        when (demoState) {
            MarketDemoState.CONTENT -> load()
            MarketDemoState.EMPTY -> {
                originalQuotesSnapshot = emptyList()
                sourceStatus = MarketSourceStatus.EMPTY
                publishMarket()
            }
            MarketDemoState.ERROR -> {
                sourceStatus = MarketSourceStatus.ERROR
                updateState(
                    state.copy(
                        quotes = LoadState.Error(DEMO_ERROR_MESSAGE),
                        totalCount = 0,
                    ),
                )
            }
            MarketDemoState.LOADING -> {
                sourceStatus = MarketSourceStatus.LOADING
                updateState(state.copy(quotes = LoadState.Loading, totalCount = 0))
            }
        }
    }

    fun retry() = load()

    /** Intent: Normalize the query and republish results using case-insensitive symbol/name/exchange matching. */
    fun updateQuery(query: String) {
        updateDiscoveryState(
            state.copy(query = query.trim()),
        )
    }

    /** Intent: Change market scope and republish results using the fixed exchange-to-market mapping contract. */
    fun selectFilter(filter: MarketFilter) {
        updateDiscoveryState(
            state.copy(filter = filter),
        )
    }

    /** Intent: Change ordering and republish the already filtered result set with stable default order or change-percent ranking. */
    fun selectSort(sort: MarketSort) {
        updateDiscoveryState(
            state.copy(sort = sort),
        )
    }

    /** Intent: Toggle one symbol in favorites and immediately affect favorites-only derivation without mutating the source snapshot. */
    fun toggleFavorite(symbol: String) {
        val normalizedSymbol = normalizeSymbol(symbol)
        if (normalizedSymbol.isEmpty()) return

        val nextFavorites = state.favoriteSymbols.toMutableSet().apply {
            if (!add(normalizedSymbol)) {
                remove(normalizedSymbol)
            }
        }
        updateDiscoveryState(
            state.copy(favoriteSymbols = nextFavorites),
        )
    }

    /** Intent: Toggle favorites-only mode and republish using the current query, filter, and sort controls. */
    fun toggleFavoritesOnly() {
        updateDiscoveryState(
            state.copy(favoritesOnly = !state.favoritesOnly),
        )
    }

    /** Intent: Clear transient discovery controls back to default while retaining favorite selections and recovering from derived empty states. */
    fun clearDiscoveryFilters() {
        updateDiscoveryState(
            state.copy(
                query = "",
                filter = MarketFilter.ALL,
                sort = MarketSort.DEFAULT,
                favoritesOnly = false,
            ),
        )
    }

    /** Intent: Resolve one stock detail request from repository data and return either complete content or a user-readable error state. */
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
                recordRecentQuote(content.quote)
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

    private fun updateDiscoveryState(newState: MarketState) {
        state = newState
        when (sourceStatus) {
            MarketSourceStatus.CONTENT,
            MarketSourceStatus.EMPTY,
            -> publishMarket()
            MarketSourceStatus.LOADING,
            MarketSourceStatus.ERROR,
            -> updateState(state.copy(totalCount = 0))
        }
    }

    private fun publishMarket() {
        val derivedQuotes = state.sort.applyTo(
            state.favoriteSymbols.filterFavorites(
                state.favoritesOnly,
                state.filter.applyTo(
                    state.query.applyTo(originalQuotesSnapshot),
                ),
            ),
        )
        val derivedLoadState = if (derivedQuotes.isEmpty()) {
            LoadState.Empty
        } else {
            LoadState.Content(derivedQuotes)
        }
        updateState(
            state.copy(
                quotes = derivedLoadState,
                totalCount = derivedQuotes.size,
            ),
        )
    }

    private fun updateState(newState: MarketState) {
        state = newState
        onStateChanged(newState)
    }

    private fun recordRecentQuote(quote: StockQuote) {
        val normalizedSymbol = normalizeSymbol(quote.symbol)
        val recentQuotes = buildList {
            add(quote)
            addAll(state.recentQuotes.filterNot { normalizeSymbol(it.symbol) == normalizedSymbol })
        }.take(MAX_RECENT_QUOTES)
        updateState(state.copy(recentQuotes = recentQuotes))
    }
}

internal fun Throwable.readableMessage(): String = when (this) {
    is StockNotFoundException -> "未找到股票：$symbol"
    else -> message?.takeIf(String::isNotBlank) ?: DEFAULT_ERROR_MESSAGE
}

private const val DEMO_ERROR_MESSAGE = "行情加载失败，请重试"
private const val EMPTY_SYMBOL_MESSAGE = "股票代码不能为空"
private const val DEFAULT_ERROR_MESSAGE = "服务暂时不可用，请重试"
private const val MAX_RECENT_QUOTES = 3

private enum class MarketSourceStatus {
    CONTENT,
    EMPTY,
    ERROR,
    LOADING,
}

private fun String.applyTo(quotes: List<StockQuote>): List<StockQuote> {
    val normalizedQuery = trim().lowercase()
    if (normalizedQuery.isEmpty()) return quotes

    return quotes.filter { quote ->
        quote.symbol.lowercase().contains(normalizedQuery) ||
            quote.name.lowercase().contains(normalizedQuery) ||
            quote.exchange.lowercase().contains(normalizedQuery)
    }
}

private fun MarketFilter.applyTo(quotes: List<StockQuote>): List<StockQuote> = when (this) {
    MarketFilter.ALL -> quotes
    MarketFilter.HK -> quotes.filter { it.exchange.matchesAnyOf("HKEX", "SEHK", "HONGKONG", "HK") }
    MarketFilter.CN -> quotes.filter { it.exchange.matchesAnyOf("SSE", "SZSE", "SHSE", "SZ", "SH", "BSE") }
    MarketFilter.US -> quotes.filter { it.exchange.matchesAnyOf("NASDAQ", "NYSE", "AMEX", "NYSEARCA", "OTC") }
}

private fun Set<String>.filterFavorites(
    favoritesOnly: Boolean,
    quotes: List<StockQuote>,
): List<StockQuote> {
    if (!favoritesOnly) return quotes

    return quotes.filter { quote -> contains(normalizeSymbol(quote.symbol)) }
}

private fun MarketSort.applyTo(quotes: List<StockQuote>): List<StockQuote> = when (this) {
    MarketSort.DEFAULT -> quotes
    MarketSort.GAINERS -> quotes.sortedByDescending(StockQuote::changePercent)
    MarketSort.LOSERS -> quotes.sortedBy(StockQuote::changePercent)
    MarketSort.VOLATILITY -> quotes.sortedByDescending(StockQuote::intradayAmplitudePercent)
}

private val StockQuote.intradayAmplitudePercent: Double
    get() = if (previousClose == 0.0) 0.0 else (high - low) / previousClose * 100.0

private fun String.matchesAnyOf(vararg exchanges: String): Boolean {
    val normalizedExchange = normalizeExchange(this)
    return exchanges.any { expected -> normalizedExchange.startsWith(expected) }
}

private fun normalizeExchange(exchange: String): String =
    exchange.uppercase().filter(Char::isLetterOrDigit)

private fun normalizeSymbol(symbol: String): String = symbol.trim().uppercase()
