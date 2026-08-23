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

/** Renders the market gallery while keeping data and navigation actions at the controller boundary. */
fun ViewContainer<*, *>.marketContentSlot(
    state: MarketState,
    onRetry: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onSelectDemo: (MarketDemoState) -> Unit,
    onSelectStock: (String) -> Unit,
) {
    View {
        attr {
            flex(DesignTokens.Size.FILL)
            borderRadius(DesignTokens.Radius.LG)
            backgroundColor(DesignTokens.Colors.surfaceAlt)
        }
        marketHeader()
        marketFilters()
        when (val quotes = state.quotes) {
            LoadState.Loading -> marketStatusPanel(
                eyebrow = "LIVE MARKET",
                title = "正在加载行情",
                description = "正在获取最新市场数据，请稍候…",
            )
            LoadState.Empty -> marketStatusPanel(
                eyebrow = "NO SHOTS YET",
                title = "暂无行情",
                description = "当前分类还没有可展示的股票",
            )
            is LoadState.Error -> marketStatusPanel(
                eyebrow = "CONNECTION LOST",
                title = "行情加载失败",
                description = quotes.message,
                action = "重新加载",
                onAction = onRetry,
            )
            is LoadState.Content -> marketQuoteGallery(quotes.value, onSelectStock)
        }
    }
}

private fun ViewContainer<*, *>.marketHeader() {
    View {
        attr {
            padding(DesignTokens.Spacing.MD)
            backgroundColor(DesignTokens.Colors.primary)
        }
        Text {
            attr {
                text("市场灵感库")
                fontSize(DesignTokens.Typography.H2)
                fontWeightBold()
                color(DesignTokens.Colors.onSurface)
            }
        }
        Text {
            attr {
                text("捕捉全球市场脉搏，发现值得关注的价格作品")
                fontSize(DesignTokens.Typography.BODY)
                color(DesignTokens.Colors.onSurfaceMuted)
                marginTop(DesignTokens.Spacing.XS)
            }
        }
    }
}

private fun ViewContainer<*, *>.marketFilters() {
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
        marketFilterChip("全部", selected = true)
        marketFilterChip("港股", selected = false)
        marketFilterChip("A股", selected = false)
        marketFilterChip("美股", selected = false)
    }
}

private fun ViewContainer<*, *>.marketFilterChip(label: String, selected: Boolean) {
    View {
        attr {
            borderRadius(DesignTokens.Radius.FULL)
            padding(
                top = DesignTokens.Spacing.XS,
                bottom = DesignTokens.Spacing.XS,
                left = DesignTokens.Spacing.SM,
                right = DesignTokens.Spacing.SM,
            )
            marginRight(DesignTokens.Spacing.XS)
            backgroundColor(
                if (selected) DesignTokens.Colors.accentPrimary else DesignTokens.Colors.surfaceElevated,
            )
        }
        Text {
            attr {
                text(label)
                fontSize(DesignTokens.Typography.CAPTION)
                fontWeightBold()
                color(if (selected) DesignTokens.Colors.onPrimary else DesignTokens.Colors.onSurfaceMuted)
            }
        }
    }
}

private fun ViewContainer<*, *>.marketQuoteGallery(
    quotes: kotlin.collections.List<StockQuote>,
    onSelectStock: (String) -> Unit,
) {
    val observableQuotes = ObservableList(quotes.toMutableList())
    List {
        attr {
            flex(DesignTokens.Size.FILL)
            flexDirectionColumn()
            firstContentLoadMaxIndex(quotes.size)
            padding(left = DesignTokens.Spacing.MD, right = DesignTokens.Spacing.MD)
        }
        marketOverview(quotes)
        vfor({ observableQuotes }) { quote ->
            marketQuoteCard(quote, onSelectStock)
        }
    }
}

private fun ViewContainer<*, *>.marketOverview(quotes: kotlin.collections.List<StockQuote>) {
    val risingCount = quotes.count { it.change > 0.0 }
    val fallingCount = quotes.count { it.change < 0.0 }
    View {
        attr {
            flexDirectionRow()
            alignItemsCenter()
            padding(DesignTokens.Spacing.MD)
            marginBottom(DesignTokens.Spacing.SM)
            borderRadius(DesignTokens.Radius.LG)
            backgroundColor(DesignTokens.Colors.surfaceElevated)
        }
        View {
            attr { flex(DesignTokens.Size.FILL) }
            Text {
                attr {
                    text("今日观察")
                    fontSize(DesignTokens.Typography.H4)
                    fontWeightBold()
                    color(DesignTokens.Colors.onSurface)
                }
            }
            Text {
                attr {
                    text("${quotes.size} 个标的正在更新")
                    fontSize(DesignTokens.Typography.CAPTION)
                    color(DesignTokens.Colors.onSurfaceMuted)
                    marginTop(DesignTokens.Spacing.XXS)
                }
            }
        }
        marketOverviewMetric("↑ 上涨", risingCount.toString(), DesignTokens.Colors.accentPrimary)
        marketOverviewMetric("↓ 下跌", fallingCount.toString(), DesignTokens.Colors.accentTertiary)
    }
}

private fun ViewContainer<*, *>.marketOverviewMetric(label: String, value: String, color: Color) {
    View {
        attr {
            marginLeft(DesignTokens.Spacing.MD)
            alignItemsCenter()
        }
        Text {
            attr {
                text(value)
                fontSize(DesignTokens.Typography.H3)
                fontWeightBold()
                color(color)
            }
        }
        Text {
            attr {
                text(label)
                fontSize(DesignTokens.Typography.CAPTION)
                color(DesignTokens.Colors.onSurfaceMuted)
                marginTop(DesignTokens.Spacing.XXS)
            }
        }
    }
}

private fun ViewContainer<*, *>.marketQuoteCard(
    quote: StockQuote,
    onSelectStock: (String) -> Unit,
) {
    val trendColor = quote.trendColor()
    val trendLabel = quote.trendLabel()
    View {
        attr {
            flexDirectionRow()
            alignItemsCenter()
            padding(DesignTokens.Spacing.MD)
            marginBottom(DesignTokens.Spacing.SM)
            borderRadius(DesignTokens.Radius.LG)
            backgroundColor(DesignTokens.Colors.surfaceElevated)
        }
        event { click { onSelectStock(quote.symbol) } }
        View {
            attr {
                width(DesignTokens.Spacing.XXS)
                height(DesignTokens.Spacing.XL)
                borderRadius(DesignTokens.Radius.FULL)
                backgroundColor(trendColor)
                marginRight(DesignTokens.Spacing.SM)
            }
        }
        View {
            attr { flex(DesignTokens.Size.FILL) }
            Text {
                attr {
                    text(quote.name)
                    fontSize(DesignTokens.Typography.BODY_LARGE)
                    fontWeightBold()
                    color(DesignTokens.Colors.onSurface)
                }
            }
            Text {
                attr {
                    text("${quote.exchange} · ${quote.symbol}")
                    fontSize(DesignTokens.Typography.CAPTION)
                    color(DesignTokens.Colors.onSurfaceMuted)
                    marginTop(DesignTokens.Spacing.XXS)
                }
            }
        }
        View {
            attr { alignItemsCenter() }
            Text {
                attr {
                    text(formatStockPrice(quote.price))
                    fontSize(DesignTokens.Typography.H4)
                    fontWeightBold()
                    color(DesignTokens.Colors.onSurface)
                }
            }
            Text {
                attr {
                    text("$trendLabel ${formatStockChange(quote.change)}  ${formatStockChangePercent(quote.changePercent)}")
                    fontSize(DesignTokens.Typography.CAPTION)
                    fontWeightBold()
                    color(trendColor)
                    marginTop(DesignTokens.Spacing.XXS)
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.marketStatusPanel(
    eyebrow: String,
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
                text(eyebrow)
                fontSize(DesignTokens.Typography.CAPTION)
                fontWeightBold()
                color(DesignTokens.Colors.accentTertiary)
            }
        }
        Text {
            attr {
                text(title)
                fontSize(DesignTokens.Typography.H3)
                fontWeightBold()
                color(DesignTokens.Colors.onSurface)
                marginTop(DesignTokens.Spacing.XS)
            }
        }
        Text {
            attr {
                text(description)
                fontSize(DesignTokens.Typography.BODY)
                color(DesignTokens.Colors.onSurfaceMuted)
                marginTop(DesignTokens.Spacing.XS)
            }
        }
        if (action != null && onAction != null) {
            View {
                attr {
                    borderRadius(DesignTokens.Radius.FULL)
                    padding(
                        top = DesignTokens.Spacing.XS,
                        bottom = DesignTokens.Spacing.XS,
                        left = DesignTokens.Spacing.MD,
                        right = DesignTokens.Spacing.MD,
                    )
                    marginTop(DesignTokens.Spacing.MD)
                    backgroundColor(DesignTokens.Colors.accentPrimary)
                }
                event { click { onAction() } }
                Text {
                    attr {
                        text(action)
                        fontSize(DesignTokens.Typography.BODY)
                        fontWeightBold()
                        color(DesignTokens.Colors.onPrimary)
                    }
                }
            }
        }
    }
}

private fun StockQuote.trendColor(): Color = when {
    change > 0.0 -> DesignTokens.Colors.accentPrimary
    change < 0.0 -> DesignTokens.Colors.success
    else -> DesignTokens.Colors.onSurfaceMuted
}

private fun StockQuote.trendLabel(): String = when {
    change > 0.0 -> "↑ 上涨"
    change < 0.0 -> "↓ 下跌"
    else -> "— 持平"
}
