package com.ourcx.kuiklystock.data

import com.ourcx.kuiklystock.domain.ChatRequest
import com.ourcx.kuiklystock.domain.ChatResponse
import com.ourcx.kuiklystock.domain.StockInsight
import com.ourcx.kuiklystock.domain.StockQuote

interface StockRepository {
    /** Metadata for the symbols that can be loaded page by page. Empty means this source is not paged. */
    val directory: List<StockDirectoryEntry>
        get() = emptyList()

    /** Returns the complete quote snapshot in a stable display order without blocking the UI thread. */
    fun getQuotes(callback: (Result<List<StockQuote>>) -> Unit)

    /** Returns one stable directory slice. Non-paged sources use the complete snapshot as a compatibility fallback. */
    fun getQuotesPage(offset: Int, limit: Int, callback: (Result<List<StockQuote>>) -> Unit) {
        if (offset < 0 || limit <= 0) {
            callback(Result.failure(IllegalArgumentException("Invalid quote page")))
            return
        }
        getQuotes { result -> callback(result.map { quotes -> quotes.drop(offset).take(limit) }) }
    }

    /** Validates one user-entered symbol against the backing source and adds it to the directory. */
    fun addQuote(symbol: String, callback: (Result<StockQuote>) -> Unit) {
        callback(Result.failure(UnsupportedOperationException("This quote source does not support custom symbols")))
    }

    /**
     * Returns the quote for [symbol].
     *
     * @throws StockNotFoundException when the symbol is not available.
     */
    fun getQuote(symbol: String): StockQuote

}

data class StockDirectoryEntry(
    val symbol: String,
    val name: String,
    val exchange: String,
)

interface InsightRepository {
    /** Generates an AI insight from the supplied quote facts. */
    fun getInsight(quote: StockQuote, callback: (Result<StockInsight>) -> Unit)
}

interface ChatRepository {
    /** Whether this repository can currently accept chat requests. */
    val isConfigured: Boolean

    /**
     * Starts a chat turn and reports its eventual response through [callback].
     * Success and failure are both delivered through [Result].
     */
    fun ask(request: ChatRequest, callback: (Result<ChatResponse>) -> Unit)

    /** Emits assistant text as it arrives, then reports one terminal response. */
    fun askStreaming(
        request: ChatRequest,
        onDelta: (String) -> Unit,
        callback: (Result<ChatResponse>) -> Unit,
    ) {
        ask(request) { result ->
            result.onSuccess { response -> onDelta(response.answer) }
            callback(result)
        }
    }
}

interface ResearchServiceConfiguration {
    val isConfigured: Boolean
    fun current(): ResearchServiceSettings
    fun save(baseUrl: String, token: String, model: String): Result<Unit>
    fun clear()
}

data class ResearchServiceSettings(
    val baseUrl: String = "",
    val model: String = "",
    val hasToken: Boolean = false,
)

class StockNotFoundException(
    val symbol: String,
) : IllegalArgumentException("Unknown stock symbol: $symbol")

class ChatFixtureException(
    message: String,
) : IllegalStateException(message)
