package com.ourcx.kuiklystock.ui.component

import com.ourcx.kuiklystock.domain.LoadState
import com.ourcx.kuiklystock.domain.StockDetailContent
import com.ourcx.kuiklystock.domain.StockDetailState
import com.ourcx.kuiklystock.theme.DesignTokens
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
            backgroundColor(DesignTokens.Colors.surfaceBase)
        }
        detailBackButton(onBack)
        when (val content = state?.content) {
            null -> detailStatus("未找到详情", "当前股票详情不可用，请返回行情列表重新选择。")
            LoadState.Loading -> detailStatus("正在加载详情", "正在获取股票行情与风险信息…")
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
            backgroundColor(DesignTokens.Colors.surfaceAlt)
        }
        event { click { onBack() } }
        View {
            attr {
                padding(DesignTokens.Spacing.SM)
                borderRadius(DesignTokens.Radius.FULL)
                backgroundColor(DesignTokens.Colors.accentPrimary)
            }
            Text {
                attr {
                    text("‹ 返回行情")
                    fontSize(DesignTokens.Typography.BODY_LARGE)
                    fontWeightBold()
                    color(DesignTokens.Colors.onPrimary)
                }
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
        stockInsightSection(content.insight)
        disclaimer()
    }
}

private fun ViewContainer<*, *>.disclaimer() {
    Text {
        attr {
            text("免责声明：以上行情与趋势判断仅供信息参考，不构成任何投资建议。投资有风险，决策需谨慎。")
            fontSize(DesignTokens.Typography.CAPTION)
            color(DesignTokens.Colors.onSurfaceMuted)
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
                fontSize(DesignTokens.Typography.H3)
                fontWeightBold()
                color(DesignTokens.Colors.onSurface)
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
        Text {
            attr {
                text("可使用上方“返回行情”返回。")
                fontSize(DesignTokens.Typography.CAPTION)
                color(DesignTokens.Colors.onSurfaceMuted)
                marginTop(DesignTokens.Spacing.SM)
            }
        }
    }
}
