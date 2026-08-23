package com.ourcx.kuiklystock.ui.component

import com.ourcx.kuiklystock.domain.LoadState
import com.ourcx.kuiklystock.domain.MarketFilter
import com.ourcx.kuiklystock.domain.MarketSort
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
import com.tencent.kuikly.core.views.Input
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View

/** Renders the market gallery while keeping data and navigation actions at the controller boundary. */
fun ViewContainer<*, *>.marketContentSlot(
    state: MarketState,
    onRetry: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onSelectDemo: (MarketDemoState) -> Unit,
    onSelectStock: (String) -> Unit,
    onUpdateQuery: (String) -> Unit,
    onSelectFilter: (MarketFilter) -> Unit,
    onSelectSort: (MarketSort) -> Unit,
    onToggleFavoritesOnly: () -> Unit,
    onClearDiscoveryFilters: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onAskAi: (String) -> Unit,
) {
    View {
        attr {
            flex(DesignTokens.Size.FILL)
            borderRadius(DesignTokens.Radius.LG)
            backgroundColor(DesignTokens.Colors.surfaceAlt)
        }
        marketHeader()
        marketDiscoveryToolbar(
            state = state,
            onUpdateQuery = onUpdateQuery,
            onSelectFilter = onSelectFilter,
            onSelectSort = onSelectSort,
            onToggleFavoritesOnly = onToggleFavoritesOnly,
        )
        when (val quotes = state.quotes) {
            LoadState.Loading -> marketStatusPanel(
                eyebrow = "LIVE MARKET",
                title = "正在加载行情",
                description = "正在获取最新市场数据，请稍候…",
            )
            LoadState.Empty -> marketStatusPanel(
                eyebrow = "DISCOVERY EMPTY",
                title = "没有找到匹配的行情",
                description = state.emptyDiscoveryDescription(),
                action = if (state.hasDiscoveryFiltersApplied()) "清除筛选" else null,
                onAction = if (state.hasDiscoveryFiltersApplied()) onClearDiscoveryFilters else null,
            )
            is LoadState.Error -> marketStatusPanel(
                eyebrow = "CONNECTION LOST",
                title = "行情加载失败",
                description = quotes.message,
                action = "重新加载",
                onAction = onRetry,
            )
            is LoadState.Content -> marketQuoteGallery(
                quotes = quotes.value,
                favoriteSymbols = state.favoriteSymbols,
                onSelectStock = onSelectStock,
                onToggleFavorite = onToggleFavorite,
                onAskAi = onAskAi,
            )
        }
    }
}

private fun ViewContainer<*, *>.marketHeader() {
    View {
        attr {
            padding(DesignTokens.Size.PAGE_GUTTER)
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

private fun ViewContainer<*, *>.marketDiscoveryToolbar(
    state: MarketState,
    onUpdateQuery: (String) -> Unit,
    onSelectFilter: (MarketFilter) -> Unit,
    onSelectSort: (MarketSort) -> Unit,
    onToggleFavoritesOnly: () -> Unit,
) {
    View {
        attr {
            padding(
                top = DesignTokens.Spacing.SM,
                bottom = DesignTokens.Spacing.SM,
                left = DesignTokens.Size.PAGE_GUTTER,
                right = DesignTokens.Size.PAGE_GUTTER,
            )
        }
        marketSearchField(
            query = state.query,
            onUpdateQuery = onUpdateQuery,
        )
        marketChipRow {
            marketFilterChip("全部", state.filter == MarketFilter.ALL) {
                onSelectFilter(MarketFilter.ALL)
            }
            marketFilterChip("港股", state.filter == MarketFilter.HK) {
                onSelectFilter(MarketFilter.HK)
            }
            marketFilterChip("A股", state.filter == MarketFilter.CN) {
                onSelectFilter(MarketFilter.CN)
            }
            marketFilterChip("美股", state.filter == MarketFilter.US) {
                onSelectFilter(MarketFilter.US)
            }
        }
        View {
            attr {
                flexDirectionRow()
                alignItemsCenter()
                marginTop(DesignTokens.Spacing.SM)
            }
            marketCompactChip("默认", state.sort == MarketSort.DEFAULT) {
                onSelectSort(MarketSort.DEFAULT)
            }
            marketCompactChip("涨幅", state.sort == MarketSort.GAINERS) {
                onSelectSort(MarketSort.GAINERS)
            }
            marketCompactChip("跌幅", state.sort == MarketSort.LOSERS) {
                onSelectSort(MarketSort.LOSERS)
            }
            marketCompactChip("仅看自选", state.favoritesOnly, onToggleFavoritesOnly)
        }
        View {
            attr {
                marginTop(DesignTokens.Spacing.SM)
            }
            Text {
                attr {
                    text("共 ${state.totalCount} 个结果")
                    fontSize(DesignTokens.Typography.CAPTION)
                    color(DesignTokens.Colors.onSurfaceMuted)
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.marketSearchField(
    query: String,
    onUpdateQuery: (String) -> Unit,
) {
    View {
        attr {
            height(DesignTokens.Size.CHAT_INPUT_HEIGHT)
            padding(DesignTokens.Spacing.XXS)
            borderRadius(DesignTokens.Radius.LG)
            backgroundColor(DesignTokens.Colors.borderStrong)
        }
        View {
            attr {
                flex(DesignTokens.Size.FILL)
                borderRadius(DesignTokens.Radius.LG)
                padding(left = DesignTokens.Spacing.SM, right = DesignTokens.Spacing.SM)
                backgroundColor(DesignTokens.Colors.primary)
            }
            Input {
                attr {
                    flex(DesignTokens.Size.FILL)
                    backgroundColor(DesignTokens.Colors.primary)
                    text(query)
                    placeholder("搜索名称 / 代码 / 交易所")
                    placeholderColor(DesignTokens.Colors.onSurfaceMuted)
                    color(DesignTokens.Colors.onSurface)
                    fontSize(DesignTokens.Typography.BODY)
                    returnKeyTypeSearch()
                }
                event {
                    textDidChange { params -> onUpdateQuery(params.text) }
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.marketChipRow(
    content: ViewContainer<*, *>.() -> Unit,
) {
    View {
        attr {
            flexDirectionRow()
            marginTop(DesignTokens.Spacing.SM)
        }
        content()
    }
}

private fun ViewContainer<*, *>.marketFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
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
        event { click { onClick() } }
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

private fun ViewContainer<*, *>.marketCompactChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
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
                if (selected) DesignTokens.Colors.accentTertiary else DesignTokens.Colors.surfaceElevated,
            )
        }
        event { click { onClick() } }
        Text {
            attr {
                text(label)
                fontSize(DesignTokens.Typography.CAPTION)
                fontWeightBold()
                color(
                    if (selected) {
                        DesignTokens.Colors.onSecondary
                    } else {
                        DesignTokens.Colors.onSurfaceMuted
                    },
                )
            }
        }
    }
}

private fun ViewContainer<*, *>.marketQuoteGallery(
    quotes: kotlin.collections.List<StockQuote>,
    favoriteSymbols: Set<String>,
    onSelectStock: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onAskAi: (String) -> Unit,
) {
    val observableQuotes = ObservableList(quotes.toMutableList())
    List {
        attr {
            flex(DesignTokens.Size.FILL)
            flexDirectionColumn()
            firstContentLoadMaxIndex(quotes.size)
            padding(left = DesignTokens.Size.PAGE_GUTTER, right = DesignTokens.Size.PAGE_GUTTER)
        }
        marketOverview(quotes)
        vfor({ observableQuotes }) { quote ->
            marketQuoteCard(
                quote = quote,
                isFavorite = favoriteSymbols.contains(quote.symbol.uppercase()),
                onSelectStock = onSelectStock,
                onToggleFavorite = onToggleFavorite,
                onAskAi = onAskAi,
            )
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
    isFavorite: Boolean,
    onSelectStock: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onAskAi: (String) -> Unit,
) {
    val trendColor = quote.trendColor()
    val trendLabel = quote.trendLabel()
    View {
        attr {
            padding(DesignTokens.Spacing.MD)
            marginBottom(DesignTokens.Spacing.SM)
            borderRadius(DesignTokens.Radius.LG)
            backgroundColor(DesignTokens.Colors.surfaceElevated)
        }
        View {
            attr {
                flexDirectionRow()
                alignItemsCenter()
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
                attr {
                    width(DesignTokens.Size.MARKET_PRICE_COLUMN)
                    alignItemsFlexEnd()
                }
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
        View {
            attr {
                height(DesignTokens.Size.HAIRLINE)
                marginTop(DesignTokens.Spacing.SM)
                backgroundColor(DesignTokens.Colors.borderSubtle)
            }
        }
        View {
            attr {
                flexDirectionRow()
                marginTop(DesignTokens.Spacing.SM)
            }
            marketCardAction(
                label = if (isFavorite) "★ 已自选" else "☆ 加自选",
                selected = isFavorite,
                onClick = { onToggleFavorite(quote.symbol) },
            )
            marketCardAction(
                label = "✦ 一键问 AI",
                selected = true,
                onClick = { onAskAi(quote.symbol) },
            )
        }
    }
}

private fun ViewContainer<*, *>.marketCardAction(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    View {
        attr {
            flex(DesignTokens.Size.FILL)
            height(DesignTokens.Size.MARKET_ACTION_HEIGHT)
            allCenter()
            borderRadius(DesignTokens.Radius.MD)
            marginRight(DesignTokens.Spacing.XS)
            backgroundColor(
                if (selected) DesignTokens.Colors.primarySoft else DesignTokens.Colors.surfaceAlt,
            )
        }
        event { click { onClick() } }
        Text {
            attr {
                text(label)
                fontSize(DesignTokens.Typography.CAPTION)
                fontWeightBold()
                color(
                    if (selected) DesignTokens.Colors.accentTertiary else DesignTokens.Colors.onSurfaceMuted,
                )
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

private fun MarketState.hasDiscoveryFiltersApplied(): Boolean =
    query.isNotBlank() || filter != MarketFilter.ALL || sort != MarketSort.DEFAULT || favoritesOnly

private fun MarketState.emptyDiscoveryDescription(): String {
    val reasons = buildList {
        if (query.isNotBlank()) add("关键词“$query”")
        if (filter != MarketFilter.ALL) add("${filter.label()}市场")
        if (favoritesOnly) add("仅看自选")
    }
    return if (reasons.isEmpty()) {
        "当前条件下暂时没有可展示的股票，请稍后再试。"
    } else {
        "${reasons.joinToString("、")}下暂时没有匹配的股票，清除筛选后可恢复完整行情列表。"
    }
}

private fun MarketFilter.label(): String = when (this) {
    MarketFilter.ALL -> "全部"
    MarketFilter.HK -> "港股"
    MarketFilter.CN -> "A股"
    MarketFilter.US -> "美股"
}
