package com.ourcx.kuiklystock.domain

data class StockQuote(
    val symbol: String,
    val name: String,
    val exchange: String,
    val price: Double,
    val change: Double,
    val changePercent: Double,
    val open: Double,
    val high: Double,
    val low: Double,
    val previousClose: Double,
    val volume: Long,
    val trendPoints: List<Double>,
    val updatedAt: String = "",
    val dataSource: QuoteDataSource = QuoteDataSource.FIXTURE,
)

enum class QuoteDataSource(val label: String) {
    TENCENT("腾讯实时行情"),
    FIXTURE("离线演示数据"),
}

data class StockInsight(
    val symbol: String,
    val trendLabel: String,
    val summary: String,
    val signals: List<String>,
    val risks: List<String>,
    val updatedAt: String,
    val provider: ChatProvider = ChatProvider.LOCAL,
)
