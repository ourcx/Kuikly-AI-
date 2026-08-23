package com.ourcx.kuiklystock.ui.component

import com.ourcx.kuiklystock.domain.LoadState
import com.ourcx.kuiklystock.domain.MarketState
import com.ourcx.kuiklystock.domain.StockQuote
import com.ourcx.kuiklystock.domain.formatStockChange
import com.ourcx.kuiklystock.domain.formatStockChangePercent
import com.ourcx.kuiklystock.domain.formatStockPrice
import com.ourcx.kuiklystock.presentation.MarketDemoState
import com.ourcx.kuiklystock.theme.DesignTokens
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View

/** Renders the market state and keeps user actions at the controller boundary. */
fun ViewContainer<*, *>.marketContentSlot(
    state: MarketState,
    onRetry: () -> Unit,
    onSelectDemo: (MarketDemoState) -> Unit,
    onSelectStock: (String) -> Unit,
) {
    View {
        attr {
            flex(DesignTokens.Size.FILL)
            borderRadius(DesignTokens.Radius.LG)
            backgroundColor(DesignTokens.Colors.surface)
        }
        marketToolbar(onSelectDemo)
        View {
            attr {
                height(DesignTokens.Size.HAIRLINE)
                backgroundColor(DesignTokens.Colors.border)
            }
        }
        when (val quotes = state.quotes) {
            LoadState.Loading -> marketStatusPanel(
                title = "正在加载行情",
                description = "正在获取最新市场数据…",
            )
            LoadState.Empty -> marketStatusPanel(
                title = "暂无行情",
                description = "当前没有可展示的股票数据",
            )
            is LoadState.Error -> marketStatusPanel(
                title = "行情加载失败",
                description = quotes.message,
                action = "点击重试",
                onAction = onRetry,
            )
            is LoadState.Content -> marketQuoteList(quotes.value, onSelectStock)
        }
    }
}

private fun ViewContainer<*, *>.marketToolbar(
    onSelectDemo: (MarketDemoState) -> Unit,
) {
    View {
        attr {
            padding(DesignTokens.Spacing.MD)
        }
        Text {
            attr {
                text("市场行情")
                fontSize(DesignTokens.Typography.TITLE)
                fontWeightBold()
                color(DesignTokens.Colors.textPrimary)
            }
        }
        Text {
            attr {
                text("DEMO 状态")
                fontSize(DesignTokens.Typography.CAPTION)
                color(DesignTokens.Colors.textMuted)
                marginTop(DesignTokens.Spacing.SM)
                marginBottom(DesignTokens.Spacing.XS)
            }
        }
        View {
            attr { flexDirectionRow() }
            marketDemoButton("内容", MarketDemoState.CONTENT, onSelectDemo)
            marketDemoButton("加载", MarketDemoState.LOADING, onSelectDemo)
            marketDemoButton("空数据", MarketDemoState.EMPTY, onSelectDemo)
            marketDemoButton("错误", MarketDemoState.ERROR, onSelectDemo)
        }
    }
}

private fun ViewContainer<*, *>.marketDemoButton(
    label: String,
    demoState: MarketDemoState,
    onSelectDemo: (MarketDemoState) -> Unit,
) {
    View {
        attr {
            borderRadius(DesignTokens.Radius.SM)
            padding(
                top = DesignTokens.Spacing.XXS,
                bottom = DesignTokens.Spacing.XXS,
                left = DesignTokens.Spacing.XS,
                right = DesignTokens.Spacing.XS,
            )
            marginRight(DesignTokens.Spacing.XS)
            backgroundColor(DesignTokens.Colors.surfaceElevated)
        }
        event { click { onSelectDemo(demoState) } }
        Text {
            attr {
                text(label)
                fontSize(DesignTokens.Typography.CAPTION)
                color(DesignTokens.Colors.textSecondary)
            }
        }
    }
}

private fun ViewContainer<*, *>.marketQuoteList(
    quotes: kotlin.collections.List<StockQuote>,
    onSelectStock: (String) -> Unit,
) {
    val observableQuotes = ObservableList(quotes.toMutableList())
    View {
        attr {
            flexDirectionRow()
            padding(
                top = DesignTokens.Spacing.SM,
                bottom = DesignTokens.Spacing.SM,
                left = DesignTokens.Spacing.MD,
                right = DesignTokens.Spacing.MD,
            )
        }
        marketColumnLabel("名称 / 代码", DesignTokens.Size.FILL)
        marketColumnLabel("最新价", PRICE_COLUMN_FLEX)
        marketColumnLabel("涨跌额", CHANGE_COLUMN_FLEX)
        marketColumnLabel("涨跌幅", CHANGE_COLUMN_FLEX)
    }
    List {
        attr {
            flex(DesignTokens.Size.FILL)
            flexDirectionColumn()
            firstContentLoadMaxIndex(quotes.size)
        }
        vfor({ observableQuotes }) { quote ->
            marketQuoteRow(quote, onSelectStock)
        }
    }
}

private fun ViewContainer<*, *>.marketColumnLabel(label: String, flex: Float) {
    Text {
        attr {
            text(label)
            flex(flex)
            fontSize(DesignTokens.Typography.CAPTION)
            color(DesignTokens.Colors.textMuted)
            textAlignRight()
        }
    }
}

private fun ViewContainer<*, *>.marketQuoteRow(
    quote: StockQuote,
    onSelectStock: (String) -> Unit,
) {
    val trendColor = quote.trendColor()
    View {
        attr {
            flexDirectionRow()
            alignItemsCenter()
            padding(DesignTokens.Spacing.MD)
            margin(
                left = DesignTokens.Spacing.XS,
                right = DesignTokens.Spacing.XS,
                bottom = DesignTokens.Spacing.XS,
            )
            borderRadius(DesignTokens.Radius.MD)
            backgroundColor(DesignTokens.Colors.surfaceElevated)
        }
        event { click { onSelectStock(quote.symbol) } }
        View {
            attr { flex(DesignTokens.Size.FILL) }
            Text {
                attr {
                    text(quote.name)
                    fontSize(DesignTokens.Typography.LABEL)
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
        marketValue(formatStockPrice(quote.price), PRICE_COLUMN_FLEX, DesignTokens.Colors.textPrimary)
        marketValue(formatStockChange(quote.change), CHANGE_COLUMN_FLEX, trendColor)
        marketValue(formatStockChangePercent(quote.changePercent), CHANGE_COLUMN_FLEX, trendColor)
    }
}

private fun ViewContainer<*, *>.marketValue(value: String, flex: Float, color: Color) {
    Text {
        attr {
            text(value)
            flex(flex)
            fontSize(DesignTokens.Typography.BODY)
            fontWeightBold()
            textAlignRight()
            color(color)
        }
    }
}

private fun ViewContainer<*, *>.marketStatusPanel(
    title: String,
    description: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    View {
        attr {
            flex(DesignTokens.Size.FILL)
            allCenter()
            padding(DesignTokens.Spacing.LG)
        }
        Text {
            attr {
                text(title)
                fontSize(DesignTokens.Typography.LABEL)
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
        if (action != null && onAction != null) {
            View {
                attr {
                    borderRadius(DesignTokens.Radius.PILL)
                    padding(
                        top = DesignTokens.Spacing.XS,
                        bottom = DesignTokens.Spacing.XS,
                        left = DesignTokens.Spacing.MD,
                        right = DesignTokens.Spacing.MD,
                    )
                    marginTop(DesignTokens.Spacing.MD)
                    backgroundColor(DesignTokens.Colors.accentMuted)
                }
                event { click { onAction() } }
                Text {
                    attr {
                        text(action)
                        fontSize(DesignTokens.Typography.LABEL)
                        fontWeightBold()
                        color(DesignTokens.Colors.accent)
                    }
                }
            }
        }
    }
}

private fun StockQuote.trendColor(): Color = when {
    change > 0.0 -> DesignTokens.Colors.rise
    change < 0.0 -> DesignTokens.Colors.fall
    else -> DesignTokens.Colors.neutral
}

private const val PRICE_COLUMN_FLEX = 0.72f
private const val CHANGE_COLUMN_FLEX = 0.68f
