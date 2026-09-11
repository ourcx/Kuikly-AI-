package com.ourcx.kuiklystock.data

import com.ourcx.kuiklystock.domain.StockInsight
import com.ourcx.kuiklystock.domain.StockQuote
import kotlinx.atomicfu.atomic

/** Remote-first quotes with a deterministic Fixture fallback for offline demonstrations. */
class ResilientStockRepository(
    private val remoteRepository: StockRepository,
    private val fixtureRepository: StockRepository,
) : StockRepository {
    private var activeRepository: StockRepository = fixtureRepository

    override fun getQuotes(callback: (Result<List<StockQuote>>) -> Unit) {
        val completed = atomic(false)
        fun finish(result: Result<List<StockQuote>>) {
            if (completed.compareAndSet(expect = false, update = true)) callback(result)
        }
        fun loadFixture() {
            activeRepository = fixtureRepository
            runCatching { fixtureRepository.getQuotes(::finish) }
                .onFailure { finish(Result.failure(it)) }
        }

        runCatching {
            remoteRepository.getQuotes { result ->
                result.fold(
                    onSuccess = { quotes ->
                        activeRepository = remoteRepository
                        finish(Result.success(quotes))
                    },
                    onFailure = { loadFixture() },
                )
            }
        }.onFailure { loadFixture() }
    }

    override fun getQuote(symbol: String): StockQuote =
        runCatching { activeRepository.getQuote(symbol) }
            .recoverCatching { fixtureRepository.getQuote(symbol) }
            .getOrThrow()
}

/** Remote AI insight when configured, with local analysis as a stable fallback. */
class ResilientInsightRepository(
    private val remoteRepository: InsightRepository,
    private val fixtureRepository: InsightRepository,
    private val remoteConfigured: () -> Boolean,
) : InsightRepository {
    override fun getInsight(quote: StockQuote, callback: (Result<StockInsight>) -> Unit) {
        val completed = atomic(false)
        fun finish(result: Result<StockInsight>) {
            if (completed.compareAndSet(expect = false, update = true)) callback(result)
        }
        fun loadFixture() {
            runCatching { fixtureRepository.getInsight(quote, ::finish) }
                .onFailure { finish(Result.failure(it)) }
        }

        if (!remoteConfigured()) {
            loadFixture()
            return
        }
        runCatching {
            remoteRepository.getInsight(quote) { result ->
                result.fold(
                    onSuccess = { finish(Result.success(it)) },
                    onFailure = { loadFixture() },
                )
            }
        }.onFailure { loadFixture() }
    }
}
