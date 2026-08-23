package com.ourcx.kuiklystock.data

import com.ourcx.kuiklystock.domain.StockInsight
import com.ourcx.kuiklystock.domain.StockQuote

interface StockRepository {
    /** Returns the complete quote snapshot in a stable display order. */
    fun getQuotes(): List<StockQuote>

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
    /**
     * Returns the assistant's raw Markdown, including optional trailing stock-ai metadata.
     *
     * @throws ChatFixtureException when the deterministic demonstration failure is requested.
     */
    fun ask(question: String): String
}

class StockNotFoundException(
    val symbol: String,
) : IllegalArgumentException("Unknown stock symbol: $symbol")

class ChatFixtureException(
    message: String,
) : IllegalStateException(message)
