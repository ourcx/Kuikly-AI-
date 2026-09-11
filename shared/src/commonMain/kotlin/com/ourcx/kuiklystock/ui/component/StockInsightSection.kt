package com.ourcx.kuiklystock.ui.component

import com.ourcx.kuiklystock.domain.LoadState
import com.ourcx.kuiklystock.domain.StockInsight
import com.ourcx.kuiklystock.theme.DesignTokens
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View

fun ViewContainer<*, *>.stockInsightSection(insight: LoadState<StockInsight>) {
    when (insight) {
        LoadState.Loading -> insightStatus("AI 洞察生成中", "正在结合当前行情分析趋势、信号与风险…")
        LoadState.Empty -> insightStatus("暂无 AI 洞察", "当前没有可展示的分析结果。")
        is LoadState.Error -> insightStatus("AI 洞察暂不可用", insight.message)
        is LoadState.Content -> insightCard(insight.value)
    }
}

private fun ViewContainer<*, *>.insightStatus(title: String, description: String) {
    View {
        attr {
            padding(DesignTokens.Spacing.MD)
            borderRadius(DesignTokens.Radius.LG)
            backgroundColor(DesignTokens.Colors.surfaceElevated)
            marginBottom(DesignTokens.Spacing.MD)
        }
        Text {
            attr {
                text(title)
                fontSize(DesignTokens.Typography.H4)
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
    }
}

private fun ViewContainer<*, *>.insightCard(insight: StockInsight) {
    View {
        attr {
            padding(DesignTokens.Spacing.MD)
            borderRadius(DesignTokens.Radius.LG)
            backgroundColor(DesignTokens.Colors.surfaceElevated)
            marginBottom(DesignTokens.Spacing.MD)
        }
        View {
            attr {
                padding(DesignTokens.Spacing.XS)
                borderRadius(DesignTokens.Radius.FULL)
                backgroundColor(DesignTokens.Colors.accentPrimary)
            }
            Text {
                attr {
                    text("趋势判断 · ${insight.trendLabel}")
                    fontSize(DesignTokens.Typography.CAPTION)
                    fontWeightBold()
                    color(DesignTokens.Colors.onPrimary)
                }
            }
        }
        Text {
            attr {
                text(insight.summary)
                fontSize(DesignTokens.Typography.BODY_LARGE)
                fontWeightBold()
                color(DesignTokens.Colors.onSurface)
                marginTop(DesignTokens.Spacing.MD)
            }
        }
        insightItems("积极信号", insight.signals, "＋", DesignTokens.Colors.accentTertiary)
        insightItems("风险提示", insight.risks, "！", DesignTokens.Colors.accentPrimary)
        Text {
            attr {
                text("更新时间：${insight.updatedAt}")
                fontSize(DesignTokens.Typography.CAPTION)
                color(DesignTokens.Colors.onSurfaceMuted)
                marginTop(DesignTokens.Spacing.SM)
            }
        }
    }
}

private fun ViewContainer<*, *>.insightItems(
    title: String,
    items: kotlin.collections.List<String>,
    marker: String,
    accentColor: Color,
) {
    Text {
        attr {
            text(title)
            fontSize(DesignTokens.Typography.CAPTION)
            fontWeightBold()
            color(accentColor)
            marginTop(DesignTokens.Spacing.MD)
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
            color(DesignTokens.Colors.onSurface)
            marginTop(DesignTokens.Spacing.XXS)
        }
    }
}
