package com.ourcx.kuiklystock.data

import com.ourcx.kuiklystock.domain.StockInsight
import com.ourcx.kuiklystock.domain.StockQuote
import kotlinx.atomicfu.atomic

/** Remote-first quotes with a deterministic Fixture fallback for offline demonstrations. */
class ResilientStockRepository(
    private val remoteRepository: StockRepository,
    private val fixtureRepository: StockRepository,
) : StockRepository {
    private var activeRepository: StockRepository = remoteRepository
    override val directory: List<StockDirectoryEntry>
        get() = activeRepository.directory

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

    override fun addQuote(symbol: String, callback: (Result<StockQuote>) -> Unit) {
        remoteRepository.addQuote(symbol) { result ->
            result.onSuccess { activeRepository = remoteRepository }
            callback(result)
        }
    }

    override fun getQuotesPage(offset: Int, limit: Int, callback: (Result<List<StockQuote>>) -> Unit) {
        if (offset == 0) {
            remoteRepository.getQuotesPage(offset, limit) { result ->
                result.fold(
                    onSuccess = { quotes ->
                        activeRepository = remoteRepository
                        callback(Result.success(quotes))
                    },
                    onFailure = {
                        activeRepository = fixtureRepository
                        // Fixture 没有远端目录，首屏回退时一次返回完整集合，避免目录总数与分页状态不一致。
                        fixtureRepository.getQuotes(callback)
                    },
                )
            }
            return
        }

        // 首屏已展示实时数据后，下一页失败不能悄悄拼入另一套 Fixture 价格。
        activeRepository.getQuotesPage(offset, limit, callback)
    }
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
