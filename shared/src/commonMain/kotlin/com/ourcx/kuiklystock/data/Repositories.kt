package com.ourcx.kuiklystock.data

import com.ourcx.kuiklystock.domain.ChatRequest
import com.ourcx.kuiklystock.domain.ChatResponse
import com.ourcx.kuiklystock.domain.StockInsight
import com.ourcx.kuiklystock.domain.StockQuote

interface StockRepository {
    /** Returns the complete quote snapshot in a stable display order without blocking the UI thread. */
    fun getQuotes(callback: (Result<List<StockQuote>>) -> Unit)

    /**
     * Returns the quote for [symbol].
     *
     * @throws StockNotFoundException when the symbol is not available.
     */
    fun getQuote(symbol: String): StockQuote

    /**
     * Returns the analysis paired with [symbol].
     *
     * @throws StockNotFoundException when the symbol is not available.
     */
    fun getInsight(symbol: String): StockInsight
}

interface ChatRepository {
    /** Whether this repository can currently accept chat requests. */
    val isConfigured: Boolean

    /**
     * Starts a chat turn and reports its eventual response through [callback].
     * Success and failure are both delivered through [Result].
     */
    fun ask(request: ChatRequest, callback: (Result<ChatResponse>) -> Unit)
}

interface ResearchServiceConfiguration {
    val isConfigured: Boolean
    fun currentUrl(): String
    fun save(url: String): Result<Unit>
    fun clear()
}

class StockNotFoundException(
    val symbol: String,
) : IllegalArgumentException("Unknown stock symbol: $symbol")

class ChatFixtureException(
    message: String,
) : IllegalStateException(message)
