package com.ourcx.kuiklystock.ui.component

import com.ourcx.kuiklystock.domain.StockQuote
import com.ourcx.kuiklystock.domain.analyzeTrend
import com.ourcx.kuiklystock.domain.formatStockChange
import com.ourcx.kuiklystock.domain.formatStockChangePercent
import com.ourcx.kuiklystock.domain.formatStockPrice
import com.ourcx.kuiklystock.domain.formatStockVolume
import com.ourcx.kuiklystock.domain.normalizeTrendPoints
import com.ourcx.kuiklystock.theme.DesignTokens
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.Canvas

/** Reusable compact quote card. When supplied, [onClick] receives the quote symbol. */
fun ViewContainer<*, *>.stockCard(quote: StockQuote, onClick: ((String) -> Unit)? = null) {
    val trendColor = quote.trendColor()
    View {
        attr {
            padding(DesignTokens.Spacing.MD)
            borderRadius(DesignTokens.Radius.LG)
            backgroundColor(DesignTokens.Colors.surfaceAlt)
            marginBottom(DesignTokens.Spacing.MD)
        }
        if (onClick != null) event { click { onClick(quote.symbol) } }
        Text {
            attr {
                text(quote.name)
                fontSize(DesignTokens.Typography.H3)
                fontWeightBold()
                color(DesignTokens.Colors.onSurface)
            }
        }
        Text {
            attr {
                text("${quote.exchange} · ${quote.symbol}  |  ${quote.dataSource.label}")
                fontSize(DesignTokens.Typography.CAPTION)
                color(DesignTokens.Colors.accentTertiary)
                marginTop(DesignTokens.Spacing.XXS)
            }
        }
        Text {
            attr {
                text(formatStockPrice(quote.price))
                fontSize(DesignTokens.Typography.H1)
                fontWeightBold()
                color(DesignTokens.Colors.onSurface)
                marginTop(DesignTokens.Spacing.LG)
            }
        }
        View {
            attr {
                padding(DesignTokens.Spacing.XS)
                borderRadius(DesignTokens.Radius.FULL)
                backgroundColor(trendColor)
                marginTop(DesignTokens.Spacing.SM)
            }
            Text {
                attr {
                    text("${quote.trendMarker()}  ${formatStockChange(quote.change)}  ${formatStockChangePercent(quote.changePercent)}")
                    fontSize(DesignTokens.Typography.BODY)
                    fontWeightBold()
                    color(DesignTokens.Colors.onPrimary)
                }
            }
        }
    }
}

/** Reusable two-column quote metric grid. */
fun ViewContainer<*, *>.metricGrid(quote: StockQuote) {
    View {
        attr {
            padding(DesignTokens.Spacing.MD)
            borderRadius(DesignTokens.Radius.LG)
            backgroundColor(DesignTokens.Colors.surfaceElevated)
            marginBottom(DesignTokens.Spacing.MD)
        }
        Text {
            attr {
                text("关键指标")
                fontSize(DesignTokens.Typography.H4)
                fontWeightBold()
                color(DesignTokens.Colors.accentTertiary)
                marginBottom(DesignTokens.Spacing.MD)
            }
        }
        metricRow("开盘", formatStockPrice(quote.open), "最高", formatStockPrice(quote.high))
        metricRow("最低", formatStockPrice(quote.low), "昨收", formatStockPrice(quote.previousClose))
        metricRow("成交量", formatStockVolume(quote.volume), "交易所", quote.exchange)
    }
}

private fun ViewContainer<*, *>.metricRow(firstLabel: String, firstValue: String, secondLabel: String, secondValue: String) {
    View {
        attr {
            flexDirectionRow()
            marginBottom(DesignTokens.Spacing.XS)
        }
        metricCell(firstLabel, firstValue)
        metricCell(secondLabel, secondValue)
    }
}

private fun ViewContainer<*, *>.metricCell(label: String, value: String) {
    View {
        attr {
            flex(DesignTokens.Size.FILL)
            padding(DesignTokens.Spacing.SM)
            borderRadius(DesignTokens.Radius.MD)
            backgroundColor(DesignTokens.Colors.surfaceAlt)
            marginRight(DesignTokens.Spacing.XS)
        }
        Text {
            attr {
                text(label)
                fontSize(DesignTokens.Typography.CAPTION)
                color(DesignTokens.Colors.onSurfaceMuted)
            }
        }
        Text {
            attr {
                text(value)
                fontSize(DesignTokens.Typography.BODY)
                fontWeightBold()
                color(DesignTokens.Colors.onSurface)
                marginTop(DesignTokens.Spacing.XXS)
            }
        }
    }
}

/** Kuikly Canvas price-range trajectory with snapshot-derived analysis. */
fun ViewContainer<*, *>.sparkline(quote: StockQuote) {
    val normalizedPoints = normalizeTrendPoints(quote.trendPoints)
    val chartColor = trendColor(quote.change)
    val analysis = analyzeTrend(quote)
    View {
        attr {
            padding(DesignTokens.Spacing.MD)
            borderRadius(DesignTokens.Radius.LG)
            backgroundColor(DesignTokens.Colors.surfaceAlt)
            marginBottom(DesignTokens.Spacing.MD)
        }
        View {
            attr { flexDirectionRow() }
            View {
                attr { flex(DesignTokens.Size.FILL) }
                Text {
                    attr {
                        text("价格趋势")
                        fontSize(DesignTokens.Typography.H4)
                        fontWeightBold()
                        color(DesignTokens.Colors.onSurface)
                    }
                }
            }
            Text {
                attr {
                    text(trendDescription(quote.change))
                    fontSize(DesignTokens.Typography.CAPTION)
                    fontWeightBold()
                    color(chartColor)
                }
            }
        }
        Text {
            attr {
                text("价格区间轨迹 · ${quote.dataSource.label}")
                fontSize(DesignTokens.Typography.CAPTION)
                color(DesignTokens.Colors.accentTertiary)
                marginTop(DesignTokens.Spacing.XXS)
                marginBottom(DesignTokens.Spacing.MD)
            }
        }
        if (normalizedPoints.isEmpty()) {
            Text {
                attr {
                    text("暂无趋势数据")
                    fontSize(DesignTokens.Typography.BODY)
                    color(DesignTokens.Colors.onSurfaceMuted)
                }
            }
        } else {
            Canvas({
                attr { height(DesignTokens.Size.SPARKLINE_HEIGHT) }
            }) { canvas, width, height ->
                val horizontalInset = 5f
                val verticalInset = 8f
                val drawableWidth = (width - horizontalInset * 2).coerceAtLeast(1f)
                val drawableHeight = (height - verticalInset * 2).coerceAtLeast(1f)

                canvas.beginPath()
                canvas.setLineDash(listOf(4f, 4f))
                canvas.strokeStyle(DesignTokens.Colors.onSurfaceMuted)
                canvas.lineWidth(1f)
                canvas.moveTo(horizontalInset, verticalInset + drawableHeight / 2f)
                canvas.lineTo(width - horizontalInset, verticalInset + drawableHeight / 2f)
                canvas.stroke()

                canvas.beginPath()
                canvas.setLineDash(emptyList())
                normalizedPoints.forEachIndexed { index, point ->
                    val x = horizontalInset + drawableWidth * index / (normalizedPoints.size - 1).coerceAtLeast(1)
                    val y = verticalInset + drawableHeight * (1f - point)
                    if (index == 0) canvas.moveTo(x, y) else canvas.lineTo(x, y)
                }
                canvas.strokeStyle(chartColor)
                canvas.lineWidth(3f)
                canvas.lineCapRound()
                canvas.stroke()

                val last = normalizedPoints.last()
                val lastX = width - horizontalInset
                val lastY = verticalInset + drawableHeight * (1f - last)
                canvas.beginPath()
                canvas.arc(lastX, lastY, 4f, 0f, (kotlin.math.PI * 2).toFloat(), false)
                canvas.fillStyle(chartColor)
                canvas.fill()
            }
            trendMetrics(analysis.rangePercent, analysis.rangePositionPercent, analysis.relativeToPreviousClosePercent)
        }
    }
}

private fun ViewContainer<*, *>.trendMetrics(range: Double, position: Double, relative: Double) {
    View {
        attr {
            flexDirectionRow()
            marginTop(DesignTokens.Spacing.SM)
        }
        trendMetric("振幅", "${formatStockPrice(range)}%")
        trendMetric("区间位置", "${formatStockPrice(position)}%")
        trendMetric("较昨收", formatStockChangePercent(relative))
    }
}

private fun ViewContainer<*, *>.trendMetric(label: String, value: String) {
    View {
        attr { flex(DesignTokens.Size.FILL) }
        Text {
            attr {
                text(label)
                fontSize(DesignTokens.Typography.CAPTION)
                color(DesignTokens.Colors.onSurfaceMuted)
            }
        }
        Text {
            attr {
                text(value)
                fontSize(DesignTokens.Typography.BODY)
                fontWeightBold()
                color(DesignTokens.Colors.onSurface)
                marginTop(DesignTokens.Spacing.XXS)
            }
        }
    }
}

private fun StockQuote.trendColor(): Color = trendColor(change)
private fun StockQuote.trendMarker(): String = when {
    change > 0.0 -> "▲ 上涨"
    change < 0.0 -> "▼ 下跌"
    else -> "— 持平"
}
private fun trendColor(change: Double): Color = when {
    change > 0.0 -> DesignTokens.Colors.danger
    change < 0.0 -> DesignTokens.Colors.success
    else -> DesignTokens.Colors.onSurfaceMuted
}
private fun trendDescription(change: Double): String = when {
    change > 0.0 -> "▲ 上涨"
    change < 0.0 -> "▼ 下跌"
    else -> "— 持平"
}
