package com.ourcx.kuiklystock.ui.component

import com.ourcx.kuiklystock.domain.ChatContentBlock
import com.ourcx.kuiklystock.domain.ChatMessage
import com.ourcx.kuiklystock.domain.ChatMessageStatus
import com.ourcx.kuiklystock.domain.ChatRole
import com.ourcx.kuiklystock.domain.ChatState
import com.ourcx.kuiklystock.domain.LoadState
import com.ourcx.kuiklystock.domain.MarketState
import com.ourcx.kuiklystock.domain.StockQuote
import com.ourcx.kuiklystock.domain.WorkBuddyConnectionStatus
import com.ourcx.kuiklystock.theme.DesignTokens
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuiklybase.KuiklyMarkdown
import com.tencent.kuiklybase.config.MarkdownConfig
import com.tencent.kuiklybase.config.MarkdownColors
import com.tencent.kuiklybase.config.MarkdownTypography
import com.tencent.kuiklybase.config.TextStyleConfig

/**
 * Builds the complete WorkBuddy research experience from immutable chat/market state.
 * User intents are emitted only through callbacks; selecting structured stock content forwards its symbol.
 */
fun ViewContainer<*, *>.aiResearchContentSlot(
    state: ChatState,
    marketState: MarketState,
    onUpdateDraft: (String) -> Unit,
    onSend: () -> Unit,
    onRetry: () -> Unit,
    onSelectStock: (String) -> Unit,
) {
    View {
        attr {
            flex(DesignTokens.Size.FILL)
            borderRadius(DesignTokens.Radius.LG)
            backgroundColor(DesignTokens.Colors.surfaceAlt)
        }
        chatHeader(state.connectionStatus)
        chatConversation(
            state = state,
            marketState = marketState,
            onUpdateDraft = onUpdateDraft,
            onSend = onSend,
            onRetry = onRetry,
            onSelectStock = onSelectStock,
        )
        chatComposer(state, onUpdateDraft, onSend)
    }
}

private fun ViewContainer<*, *>.chatHeader(status: WorkBuddyConnectionStatus) {
    View {
        attr {
            padding(DesignTokens.Spacing.MD)
            backgroundColor(DesignTokens.Colors.surfaceAlt)
        }
        View {
            attr {
                flexDirectionRow()
                alignItemsCenter()
            }
            View {
                attr { flex(DesignTokens.Size.FILL) }
                Text {
                    attr {
                        text("WorkBuddy AI 投研")
                        fontSize(DesignTokens.Typography.H3)
                        fontWeightBold()
                        color(DesignTokens.Colors.onSurface)
                    }
                }
            }
            connectionBadge(status)
        }
        Text {
            attr {
                text("让行情、趋势与 AI 观点在一次对话中汇合")
                fontSize(DesignTokens.Typography.CAPTION)
                color(DesignTokens.Colors.onSurfaceMuted)
                marginTop(DesignTokens.Spacing.XXS)
            }
        }
    }
}

private fun ViewContainer<*, *>.connectionBadge(status: WorkBuddyConnectionStatus) {
    val label = when (status) {
        WorkBuddyConnectionStatus.UNCONFIGURED -> "○ 未配置"
        WorkBuddyConnectionStatus.AVAILABLE -> "● 已配置"
        WorkBuddyConnectionStatus.SENDING -> "● 响应中"
        WorkBuddyConnectionStatus.ERROR -> "! 连接异常"
    }
    val color = when (status) {
        WorkBuddyConnectionStatus.UNCONFIGURED -> DesignTokens.Colors.onSurfaceMuted
        WorkBuddyConnectionStatus.AVAILABLE -> DesignTokens.Colors.success
        WorkBuddyConnectionStatus.SENDING -> DesignTokens.Colors.accentTertiary
        WorkBuddyConnectionStatus.ERROR -> DesignTokens.Colors.danger
    }
    View {
        attr {
            padding(
                top = DesignTokens.Spacing.XXS,
                bottom = DesignTokens.Spacing.XXS,
                left = DesignTokens.Spacing.XS,
                right = DesignTokens.Spacing.XS,
            )
            borderRadius(DesignTokens.Radius.FULL)
            backgroundColor(DesignTokens.Colors.surfaceElevated)
        }
        Text {
            attr {
                text(label)
                fontSize(DesignTokens.Typography.CAPTION)
                fontWeightBold()
                color(color)
            }
        }
    }
}

private fun ViewContainer<*, *>.chatConversation(
    state: ChatState,
    marketState: MarketState,
    onUpdateDraft: (String) -> Unit,
    onSend: () -> Unit,
    onRetry: () -> Unit,
    onSelectStock: (String) -> Unit,
) {
    List {
        attr {
            flex(DesignTokens.Size.FILL)
            flexDirectionColumn()
            padding(DesignTokens.Spacing.MD)
            backgroundColor(DesignTokens.Colors.surfaceBase)
        }
        if (state.messages.isEmpty() && !state.isSending) {
            chatWelcome(state.connectionStatus, onQuestion = { question ->
                onUpdateDraft(question)
                onSend()
            })
        } else {
            state.messages.forEach { message ->
                chatMessage(message, marketState, onRetry, onSelectStock)
            }
        }
        if (state.isSending) {
            chatGeneratingBubble()
        }
    }
}

private fun ViewContainer<*, *>.chatWelcome(
    status: WorkBuddyConnectionStatus,
    onQuestion: (String) -> Unit,
) {
    View {
        attr {
            padding(DesignTokens.Spacing.LG)
            borderRadius(DesignTokens.Radius.LG)
            backgroundColor(DesignTokens.Colors.surfaceElevated)
        }
        Text {
            attr {
                text(if (status == WorkBuddyConnectionStatus.UNCONFIGURED) "连接 WorkBuddy，开启智能投研" else "今天想研究哪只股票？")
                fontSize(DesignTokens.Typography.H3)
                fontWeightBold()
                color(DesignTokens.Colors.onSurface)
            }
        }
        Text {
            attr {
                text(
                    if (status == WorkBuddyConnectionStatus.UNCONFIGURED) {
                        "当前尚未配置服务。完成连接后，可围绕名称、代码和走势发起问题。"
                    } else {
                        "输入股票名称或代码，WorkBuddy 会结合实时行情生成结构化分析。"
                    },
                )
                fontSize(DesignTokens.Typography.BODY_LARGE)
                color(DesignTokens.Colors.onSurfaceMuted)
                marginTop(DesignTokens.Spacing.XS)
            }
        }
        if (status != WorkBuddyConnectionStatus.UNCONFIGURED) {
            suggestionChip("腾讯控股有哪些积极信号？", onQuestion)
            suggestionChip("AAPL 最近走势与风险如何？", onQuestion)
        }
    }
}

private fun ViewContainer<*, *>.suggestionChip(question: String, onQuestion: (String) -> Unit) {
    View {
        attr {
            padding(DesignTokens.Spacing.SM)
            borderRadius(DesignTokens.Radius.LG)
            backgroundColor(DesignTokens.Colors.primarySoft)
            marginTop(DesignTokens.Spacing.SM)
        }
        event { click { onQuestion(question) } }
        Text {
            attr {
                text(question)
                fontSize(DesignTokens.Typography.BODY)
                color(DesignTokens.Colors.accentTertiary)
            }
        }
    }
}

private fun ViewContainer<*, *>.chatMessage(
    message: ChatMessage,
    marketState: MarketState,
    onRetry: () -> Unit,
    onSelectStock: (String) -> Unit,
) {
    val isUser = message.role == ChatRole.USER
    View {
        attr {
            if (isUser) marginLeft(DesignTokens.Spacing.XL) else marginRight(DesignTokens.Spacing.XL)
            marginBottom(DesignTokens.Spacing.SM)
            padding(DesignTokens.Spacing.MD)
            borderRadius(DesignTokens.Radius.LG)
            backgroundColor(
                if (isUser) DesignTokens.Colors.primarySoft else DesignTokens.Colors.surfaceElevated,
            )
        }
        Text {
            attr {
                text(if (isUser) "YOU / 你" else "WORKBUDDY / AI")
                fontSize(DesignTokens.Typography.CAPTION)
                fontWeightBold()
                color(if (isUser) DesignTokens.Colors.accentTertiary else DesignTokens.Colors.accentPrimary)
                marginBottom(DesignTokens.Spacing.XS)
            }
        }
        message.blocks.forEach { block ->
            chatContentBlock(block, marketState, onSelectStock)
        }
        when (message.status) {
            ChatMessageStatus.COMPLETE -> Unit
            ChatMessageStatus.GENERATING -> chatStatusLabel("正在生成回答…", failed = false)
            ChatMessageStatus.FAILED -> chatRetryAction(onRetry)
        }
    }
}

private fun ViewContainer<*, *>.chatContentBlock(
    block: ChatContentBlock,
    marketState: MarketState,
    onSelectStock: (String) -> Unit,
) {
    when (block) {
        is ChatContentBlock.Markdown -> View {
            attr {
                padding(DesignTokens.Spacing.XS)
                backgroundColor(DesignTokens.Colors.surfaceAlt)
                borderRadius(DesignTokens.Radius.MD)
                marginBottom(DesignTokens.Spacing.XS)
            }
            KuiklyMarkdown(block.text, stockMarkdownConfig)
        }
        is ChatContentBlock.StockCard -> {
            val quote = marketState.findQuote(block.symbol)
            if (quote == null) missingQuote(block.symbol) else stockCard(quote, onSelectStock)
        }
        is ChatContentBlock.Trend -> {
            val quote = marketState.findQuote(block.symbol)
            if (quote == null) missingQuote(block.symbol) else sparkline(quote.trendPoints, quote.change)
        }
    }
}

private fun ViewContainer<*, *>.chatGeneratingBubble() {
    View {
        attr {
            marginRight(DesignTokens.Spacing.XL)
            padding(DesignTokens.Spacing.MD)
            borderRadius(DesignTokens.Radius.LG)
            backgroundColor(DesignTokens.Colors.surfaceElevated)
        }
        chatStatusLabel("✦ WorkBuddy 正在生成分析…", failed = false)
    }
}

private fun ViewContainer<*, *>.chatStatusLabel(label: String, failed: Boolean) {
    Text {
        attr {
            text(label)
            fontSize(DesignTokens.Typography.BODY)
            color(if (failed) DesignTokens.Colors.danger else DesignTokens.Colors.accentTertiary)
        }
    }
}

private fun ViewContainer<*, *>.chatRetryAction(onRetry: () -> Unit) {
    View {
        attr {
            padding(DesignTokens.Spacing.SM)
            borderRadius(DesignTokens.Radius.LG)
            backgroundColor(DesignTokens.Colors.primarySoft)
            marginTop(DesignTokens.Spacing.SM)
        }
        event { click { onRetry() } }
        chatStatusLabel("! 生成失败 · 点击重新连接并重试", failed = true)
    }
}

private fun ViewContainer<*, *>.missingQuote(symbol: String) {
    Text {
        attr {
            text("暂未找到 $symbol 的行情数据")
            fontSize(DesignTokens.Typography.BODY)
            color(DesignTokens.Colors.onSurfaceMuted)
            marginBottom(DesignTokens.Spacing.XS)
        }
    }
}

private fun MarketState.findQuote(symbol: String): StockQuote? = when (val state = quotes) {
    is LoadState.Content -> state.value.firstOrNull { quote ->
        quote.symbol.equals(symbol.trim(), ignoreCase = true)
    }
    else -> null
}

private val stockMarkdownConfig = MarkdownConfig(
    colors = MarkdownColors(
        text = DesignTokens.ColorValues.ON_SURFACE,
        codeBackground = DesignTokens.ColorValues.SURFACE_BASE,
        inlineCodeBackground = DesignTokens.ColorValues.SURFACE_ALT,
        dividerColor = DesignTokens.ColorValues.BORDER_SUBTLE,
        tableBackground = DesignTokens.ColorValues.SURFACE_ALT,
        blockQuoteBar = DesignTokens.ColorValues.ACCENT_PRIMARY,
        blockQuoteBackground = DesignTokens.ColorValues.PRIMARY_SOFT,
        linkColor = DesignTokens.ColorValues.ACCENT_PRIMARY,
        codeText = DesignTokens.ColorValues.ON_SURFACE_MUTED,
    ),
    typography = MarkdownTypography(
        text = TextStyleConfig(fontSize = DesignTokens.Typography.BODY),
        paragraph = TextStyleConfig(fontSize = DesignTokens.Typography.BODY),
        h1 = TextStyleConfig(fontSize = DesignTokens.Typography.H2),
        h2 = TextStyleConfig(fontSize = DesignTokens.Typography.H3),
        h3 = TextStyleConfig(fontSize = DesignTokens.Typography.BODY_LARGE),
        quote = TextStyleConfig(fontSize = DesignTokens.Typography.BODY),
        list = TextStyleConfig(fontSize = DesignTokens.Typography.BODY),
        textLink = TextStyleConfig(fontSize = DesignTokens.Typography.BODY),
    ),
    codeHighlightDarkTheme = true,
)
