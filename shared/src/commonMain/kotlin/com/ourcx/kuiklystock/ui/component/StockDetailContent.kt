package com.ourcx.kuiklystock.ui.component

import com.ourcx.kuiklystock.domain.LoadState
import com.ourcx.kuiklystock.domain.StockDetailContent
import com.ourcx.kuiklystock.domain.StockDetailState
import com.ourcx.kuiklystock.domain.StockInsight
import com.ourcx.kuiklystock.domain.StockQuote
import com.ourcx.kuiklystock.domain.formatStockChange
import com.ourcx.kuiklystock.domain.formatStockChangePercent
import com.ourcx.kuiklystock.domain.formatStockPrice
import com.ourcx.kuiklystock.domain.formatStockVolume
import com.ourcx.kuiklystock.domain.normalizeTrendPoints
import com.ourcx.kuiklystock.theme.DesignTokens
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View

/** Renders stock-detail state while keeping navigation at the page-controller boundary. */
fun ViewContainer<*, *>.stockDetailContentSlot(
    state: StockDetailState?,
    onBack: () -> Unit,
) {
    View {
        attr {
            flex(DesignTokens.Size.FILL)
            borderRadius(DesignTokens.Radius.LG)
            backgroundColor(DesignTokens.Colors.surface)
        }
        detailBackButton(onBack)
        when (val content = state?.content) {
            null -> detailStatus("未找到详情", "当前股票详情不可用，请返回行情列表重新选择。")
            LoadState.Loading -> detailStatus("正在加载详情", "正在获取股票行情与 AI 洞察…")
            LoadState.Empty -> detailStatus("暂无详情", "当前股票没有可展示的详情数据。")
            is LoadState.Error -> detailStatus("详情加载失败", content.message)
            is LoadState.Content -> detailContent(content.value)
        }
    }
}

private fun ViewContainer<*, *>.detailBackButton(onBack: () -> Unit) {
    View {
        attr {
            padding(DesignTokens.Spacing.MD)
            backgroundColor(DesignTokens.Colors.surface)
        }
        event { click { onBack() } }
        Text {
            attr {
                text("‹ 返回行情")
                fontSize(DesignTokens.Typography.LABEL)
                fontWeightBold()
                color(DesignTokens.Colors.accent)
            }
        }
    }
}

private fun ViewContainer<*, *>.detailContent(content: StockDetailContent) {
    List {
        attr {
            flex(DesignTokens.Size.FILL)
            flexDirectionColumn()
            padding(DesignTokens.Spacing.MD)
        }
        stockCard(content.quote)
        metricGrid(content.quote)
        sparkline(content.quote.trendPoints, content.quote.change)
        insightCard(content.insight)
        disclaimer()
    }
}

/** Reusable compact quote card. When supplied, [onClick] receives the quote symbol. */
fun ViewContainer<*, *>.stockCard(
    quote: StockQuote,
    onClick: ((String) -> Unit)? = null,
) {
    val trendColor = quote.trendColor()
    View {
        attr {
            padding(DesignTokens.Spacing.MD)
            borderRadius(DesignTokens.Radius.MD)
            backgroundColor(DesignTokens.Colors.surfaceElevated)
            marginBottom(DesignTokens.Spacing.SM)
        }
        if (onClick != null) event { click { onClick(quote.symbol) } }
        View {
            attr { flexDirectionRow() }
            View {
                attr { flex(DesignTokens.Size.FILL) }
                Text {
                    attr {
                        text(quote.name)
                        fontSize(DesignTokens.Typography.TITLE)
                        fontWeightBold()
                        color(DesignTokens.Colors.textPrimary)
                    }
                }
                Text {
                    attr {
                        text("${quote.exchange} · ${quote.symbol}")
                        fontSize(DesignTokens.Typography.CAPTION)
                        color(DesignTokens.Colors.textMuted)
                        marginTop(DesignTokens.Spacing.XXS)
                    }
                }
            }
            Text {
                attr {
                    text(formatStockPrice(quote.price))
                    fontSize(DesignTokens.Typography.TITLE)
                    fontWeightBold()
                    color(DesignTokens.Colors.textPrimary)
                }
            }
        }
        Text {
            attr {
                text("${quote.trendMarker()} ${formatStockChange(quote.change)}  ${formatStockChangePercent(quote.changePercent)}")
                fontSize(DesignTokens.Typography.LABEL)
                fontWeightBold()
                color(trendColor)
                marginTop(DesignTokens.Spacing.SM)
            }
        }
    }
}

/** Reusable two-column quote metric grid. */
fun ViewContainer<*, *>.metricGrid(quote: StockQuote) {
    View {
        attr {
            padding(DesignTokens.Spacing.MD)
            borderRadius(DesignTokens.Radius.MD)
            backgroundColor(DesignTokens.Colors.surfaceElevated)
            marginBottom(DesignTokens.Spacing.SM)
        }
        Text {
            attr {
                text("关键指标")
                fontSize(DesignTokens.Typography.LABEL)
                fontWeightBold()
                color(DesignTokens.Colors.textPrimary)
                marginBottom(DesignTokens.Spacing.SM)
            }
        }
        metricRow("开盘", formatStockPrice(quote.open), "最高", formatStockPrice(quote.high))
        metricRow("最低", formatStockPrice(quote.low), "昨收", formatStockPrice(quote.previousClose))
        metricRow("成交量", formatStockVolume(quote.volume), "交易所", quote.exchange)
    }
}

private fun ViewContainer<*, *>.metricRow(
    firstLabel: String,
    firstValue: String,
    secondLabel: String,
    secondValue: String,
) {
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
        attr { flex(DesignTokens.Size.FILL) }
        Text {
            attr {
                text(label)
                fontSize(DesignTokens.Typography.CAPTION)
                color(DesignTokens.Colors.textMuted)
            }
        }
        Text {
            attr {
                text(value)
                fontSize(DesignTokens.Typography.BODY)
                fontWeightBold()
                color(DesignTokens.Colors.textPrimary)
                marginTop(DesignTokens.Spacing.XXS)
            }
        }
    }
}

/** Reusable mini trend chart backed by normalized point heights. */
fun ViewContainer<*, *>.sparkline(points: kotlin.collections.List<Double>, change: Double = 0.0) {
    val normalizedPoints = normalizeTrendPoints(points)
    val chartColor = trendColor(change)
    View {
        attr {
            padding(DesignTokens.Spacing.MD)
            borderRadius(DesignTokens.Radius.MD)
            backgroundColor(DesignTokens.Colors.surfaceElevated)
            marginBottom(DesignTokens.Spacing.SM)
        }
        Text {
            attr {
                text("价格趋势 · ${trendDescription(change)}")
                fontSize(DesignTokens.Typography.LABEL)
                fontWeightBold()
                color(DesignTokens.Colors.textPrimary)
                marginBottom(DesignTokens.Spacing.SM)
            }
        }
        if (normalizedPoints.isEmpty()) {
            Text {
                attr {
                    text("暂无趋势数据")
                    fontSize(DesignTokens.Typography.BODY)
                    color(DesignTokens.Colors.textSecondary)
                }
            }
        } else {
            View {
                attr {
                    height(DesignTokens.Size.SPARKLINE_HEIGHT)
                    flexDirectionRow()
                }
                normalizedPoints.forEach { point ->
                    View {
                        attr {
                            flex(DesignTokens.Size.FILL)
                            height(DesignTokens.Size.SPARKLINE_HEIGHT)
                            marginRight(DesignTokens.Size.SPARKLINE_BAR_GAP)
                        }
                        View {
                            attr {
                                height(
                                    DesignTokens.Size.SPARKLINE_BAR_MIN_HEIGHT +
                                        point * (DesignTokens.Size.SPARKLINE_HEIGHT - DesignTokens.Size.SPARKLINE_BAR_MIN_HEIGHT),
                                )
                                absolutePosition(bottom = 0f, left = 0f, right = 0f)
                                borderRadius(DesignTokens.Radius.SM)
                                backgroundColor(chartColor)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.insightCard(insight: StockInsight) {
    View {
        attr {
            padding(DesignTokens.Spacing.MD)
            borderRadius(DesignTokens.Radius.MD)
            backgroundColor(DesignTokens.Colors.accentMuted)
            marginBottom(DesignTokens.Spacing.SM)
        }
        Text {
            attr {
                text("AI 洞察 · ${insight.trendLabel}")
                fontSize(DesignTokens.Typography.LABEL)
                fontWeightBold()
                color(DesignTokens.Colors.accent)
            }
        }
        Text {
            attr {
                text(insight.summary)
                fontSize(DesignTokens.Typography.BODY)
                color(DesignTokens.Colors.textPrimary)
                marginTop(DesignTokens.Spacing.SM)
            }
        }
        insightItems("积极信号", insight.signals, "＋")
        insightItems("风险提示", insight.risks, "！")
        Text {
            attr {
                text("更新时间：${insight.updatedAt}")
                fontSize(DesignTokens.Typography.CAPTION)
                color(DesignTokens.Colors.textMuted)
                marginTop(DesignTokens.Spacing.SM)
            }
        }
    }
}

private fun ViewContainer<*, *>.insightItems(title: String, items: kotlin.collections.List<String>, marker: String) {
    Text {
        attr {
            text(title)
            fontSize(DesignTokens.Typography.CAPTION)
            fontWeightBold()
            color(DesignTokens.Colors.textSecondary)
            marginTop(DesignTokens.Spacing.SM)
        }
    }
    if (items.isEmpty()) {
        insightLine("— 暂无")
    } else {
        items.forEach { insightLine("$marker $it") }
    }
}

private fun ViewContainer<*, *>.insightLine(value: String) {
    Text {
        attr {
            text(value)
            fontSize(DesignTokens.Typography.BODY)
            color(DesignTokens.Colors.textPrimary)
            marginTop(DesignTokens.Spacing.XXS)
        }
    }
}

private fun ViewContainer<*, *>.disclaimer() {
    Text {
        attr {
            text("免责声明：以上行情与 AI 洞察仅供信息参考，不构成任何投资建议。投资有风险，决策需谨慎。")
            fontSize(DesignTokens.Typography.CAPTION)
            color(DesignTokens.Colors.textMuted)
            marginBottom(DesignTokens.Spacing.MD)
        }
    }
}

private fun ViewContainer<*, *>.detailStatus(title: String, description: String) {
    View {
        attr {
            flex(DesignTokens.Size.FILL)
            allCenter()
            padding(DesignTokens.Spacing.LG)
        }
        Text {
            attr {
                text(title)
                fontSize(DesignTokens.Typography.TITLE)
                fontWeightBold()
                color(DesignTokens.Colors.textPrimary)
            }
        }
        Text {
            attr {
                text(description)
                fontSize(DesignTokens.Typography.BODY)
                color(DesignTokens.Colors.textSecondary)
                marginTop(DesignTokens.Spacing.XS)
            }
        }
        Text {
            attr {
                text("可使用上方“返回行情”返回。")
                fontSize(DesignTokens.Typography.CAPTION)
                color(DesignTokens.Colors.textMuted)
                marginTop(DesignTokens.Spacing.SM)
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
    change > 0.0 -> DesignTokens.Colors.rise
    change < 0.0 -> DesignTokens.Colors.fall
    else -> DesignTokens.Colors.neutral
}

private fun trendDescription(change: Double): String = when {
    change > 0.0 -> "▲ 上涨"
    change < 0.0 -> "▼ 下跌"
    else -> "— 持平"
}
