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
)

data class StockInsight(
    val symbol: String,
    val trendLabel: String,
    val summary: String,
    val signals: List<String>,
    val risks: List<String>,
    val updatedAt: String,
)
