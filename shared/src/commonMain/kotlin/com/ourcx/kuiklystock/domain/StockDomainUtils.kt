package com.ourcx.kuiklystock.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.math.abs
import kotlin.math.round

data class ParsedStockAiContent(
    val markdown: String,
    val symbols: List<String> = emptyList(),
    val showTrend: Boolean = false,
)

fun formatStockPrice(price: Double): String = formatDecimal(price)

fun formatStockChange(change: Double): String = formatSignedDecimal(change)

fun formatStockChangePercent(changePercent: Double): String =
    "${formatSignedDecimal(changePercent)}%"

fun formatStockVolume(volume: Long): String {
    val absoluteVolume = abs(volume.toDouble())
    return when {
        absoluteVolume >= ONE_HUNDRED_MILLION ->
            "${formatDecimal(volume / ONE_HUNDRED_MILLION)}亿"
        absoluteVolume >= TEN_THOUSAND -> "${formatDecimal(volume / TEN_THOUSAND)}万"
        else -> volume.toString()
    }
}

fun normalizeTrendPoints(points: List<Double>): List<Float> {
    if (points.isEmpty()) return emptyList()

    val finitePoints = points.filter(Double::isFinite)
    val minimum = finitePoints.minOrNull() ?: return List(points.size) { NEUTRAL_TREND_POINT }
    val maximum = finitePoints.maxOrNull() ?: return List(points.size) { NEUTRAL_TREND_POINT }
    val range = maximum - minimum
    if (range == 0.0) return List(points.size) { NEUTRAL_TREND_POINT }

    return points.map { point ->
        if (point.isFinite()) {
            ((point - minimum) / range).toFloat().coerceIn(0f, 1f)
        } else {
            NEUTRAL_TREND_POINT
        }
    }
}

fun parseStockAiMetadata(markdown: String): ParsedStockAiContent {
    val match = stockAiMetadataPattern.find(markdown) ?: return ParsedStockAiContent(markdown)
    val metadata = runCatching {
        stockAiJson.decodeFromString<StockAiMetadata>(match.groupValues[1])
    }.getOrNull() ?: return ParsedStockAiContent(markdown)

    return ParsedStockAiContent(
        markdown = markdown.removeRange(match.range).trimEnd(),
        symbols = metadata.symbols,
        showTrend = metadata.showTrend,
    )
}

fun buildChatContentBlocks(parsedContent: ParsedStockAiContent): List<ChatContentBlock> = buildList {
    add(ChatContentBlock.Markdown(parsedContent.markdown))
    parsedContent.symbols.forEach { symbol ->
        add(ChatContentBlock.StockCard(symbol))
        if (parsedContent.showTrend) {
            add(ChatContentBlock.Trend(symbol))
        }
    }
}

@Serializable
private data class StockAiMetadata(
    val symbols: List<String> = emptyList(),
    val showTrend: Boolean = false,
)

private val stockAiJson = Json {
    ignoreUnknownKeys = true
}

private val stockAiMetadataPattern = Regex("""<!--stock-ai:(\{[\s\S]*\})-->\s*$""")

private const val DECIMAL_PLACES = 2
private const val DECIMAL_FACTOR = 100.0
private const val TEN_THOUSAND = 10_000.0
private const val ONE_HUNDRED_MILLION = 100_000_000.0
private const val NEUTRAL_TREND_POINT = 0.5f

private fun formatSignedDecimal(value: Double): String {
    val rounded = roundToDisplayPrecision(value)
    val prefix = if (rounded > 0.0) "+" else ""
    return prefix + formatDecimal(rounded)
}

private fun formatDecimal(value: Double): String {
    if (!value.isFinite()) return value.toString()

    val rounded = roundToDisplayPrecision(value)
    val neutralized = if (rounded == 0.0) 0.0 else rounded
    val raw = neutralized.toString()
    val decimalIndex = raw.indexOf('.')
    if (decimalIndex < 0) return "$raw.${"0".repeat(DECIMAL_PLACES)}"

    val decimalCount = raw.length - decimalIndex - 1
    return when {
        decimalCount < DECIMAL_PLACES -> raw + "0".repeat(DECIMAL_PLACES - decimalCount)
        decimalCount > DECIMAL_PLACES -> raw.substring(0, decimalIndex + DECIMAL_PLACES + 1)
        else -> raw
    }
}

private fun roundToDisplayPrecision(value: Double): Double = round(value * DECIMAL_FACTOR) / DECIMAL_FACTOR
