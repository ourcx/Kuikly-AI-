package com.ourcx.kuiklystock.ui.component

import com.ourcx.kuiklystock.domain.ChatContentBlock
import com.ourcx.kuiklystock.domain.ChatMessage
import com.ourcx.kuiklystock.domain.ChatMessageStatus
import com.ourcx.kuiklystock.domain.ChatProvider
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
    onClear: () -> Unit,
    onSelectStock: (String) -> Unit,
) {
    View {
        attr {
            flex(DesignTokens.Size.FILL)
            borderRadius(DesignTokens.Radius.LG)
            backgroundColor(DesignTokens.Colors.surfaceAlt)
        }
        chatHeader(
            status = state.connectionStatus,
            provider = state.provider,
            canClear = state.messages.isNotEmpty() && !state.isSending,
            onClear = onClear,
        )
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

private fun ViewContainer<*, *>.chatHeader(
    status: WorkBuddyConnectionStatus,
    provider: ChatProvider,
    canClear: Boolean,
    onClear: () -> Unit,
) {
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
                        text("AI 投研")
                        fontSize(DesignTokens.Typography.H3)
                        fontWeightBold()
                        color(DesignTokens.Colors.onSurface)
                    }
                }
            }
            connectionStatus(status)
        }
        Text {
            attr {
                text(provider.description())
                fontSize(DesignTokens.Typography.CAPTION)
                color(DesignTokens.Colors.onSurfaceMuted)
                marginTop(DesignTokens.Spacing.XXS)
            }
        }
        View {
            attr {
                flexDirectionRow()
                alignItemsCenter()
                marginTop(DesignTokens.Spacing.SM)
            }
            if (canClear) {
                View {
                    attr {
                        marginLeft(DesignTokens.Spacing.SM)
                        padding(
                            top = DesignTokens.Spacing.XXS,
                            bottom = DesignTokens.Spacing.XXS,
                            left = DesignTokens.Spacing.SM,
                            right = DesignTokens.Spacing.SM,
                        )
                        borderRadius(DesignTokens.Radius.MD)
                    }
                    event { click { onClear() } }
                    Text {
                        attr {
                            text("清空会话")
                            fontSize(DesignTokens.Typography.CAPTION)
                            color(DesignTokens.Colors.onSurfaceMuted)
                        }
                    }
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.connectionStatus(status: WorkBuddyConnectionStatus) {
    val label = when (status) {
        WorkBuddyConnectionStatus.UNCONFIGURED -> "本地可用"
        WorkBuddyConnectionStatus.AVAILABLE -> "WorkBuddy 在线"
        WorkBuddyConnectionStatus.SENDING -> "分析中"
        WorkBuddyConnectionStatus.ERROR -> "已切换本地"
    }
    val color = when (status) {
        WorkBuddyConnectionStatus.UNCONFIGURED -> DesignTokens.Colors.onSurfaceMuted
        WorkBuddyConnectionStatus.AVAILABLE -> DesignTokens.Colors.success
        WorkBuddyConnectionStatus.SENDING -> DesignTokens.Colors.accentTertiary
        WorkBuddyConnectionStatus.ERROR -> DesignTokens.Colors.danger
    }
    Text {
        attr {
            text(label)
            fontSize(DesignTokens.Typography.CAPTION)
            color(color)
        }
    }
}

private fun ChatProvider.description(): String = if (this == ChatProvider.LOCAL) {
    "基于当前行情的本地分析，无需配置"
} else {
    "由 WorkBuddy 结合当前行情生成分析"
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
            chatWelcome(state.provider, onQuestion = { question ->
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
    provider: ChatProvider,
    onQuestion: (String) -> Unit,
) {
    View {
        attr {
            padding(top = DesignTokens.Spacing.MD, bottom = DesignTokens.Spacing.MD)
        }
        Text {
            attr {
                text("开始研究")
                fontSize(DesignTokens.Typography.H4)
                fontWeightBold()
                color(DesignTokens.Colors.onSurface)
            }
        }
        Text {
            attr {
                text(
                    if (provider == ChatProvider.LOCAL) {
                        "输入股票名称或代码。本地分析可提供价格、趋势和风险概览。"
                    } else {
                        "输入股票名称或代码，WorkBuddy 将结合当前行情生成分析。"
                    },
                )
                fontSize(DesignTokens.Typography.BODY)
                color(DesignTokens.Colors.onSurfaceMuted)
                marginTop(DesignTokens.Spacing.XS)
            }
        }
        Text {
            attr {
                text("常用任务")
                fontSize(DesignTokens.Typography.CAPTION)
                color(DesignTokens.Colors.onSurfaceMuted)
                marginTop(DesignTokens.Spacing.LG)
            }
        }
        researchTask("腾讯控股：趋势与关键价位", "请分析腾讯控股当前趋势、支撑位、压力位和需要关注的风险。", onQuestion)
        researchTask("AAPL：日内异动原因", "请分析 AAPL 当前日内波动、可能的驱动因素和后续观察信号。", onQuestion)
        researchTask("自选股：风险检查", "请根据当前行情梳理自选股中波动较大、下行风险较高的标的。", onQuestion)
    }
}

private fun ViewContainer<*, *>.researchTask(
    label: String,
    question: String,
    onQuestion: (String) -> Unit,
) {
    View {
        attr {
            flexDirectionRow()
            alignItemsCenter()
            padding(top = DesignTokens.Spacing.SM, bottom = DesignTokens.Spacing.SM)
            marginTop(DesignTokens.Spacing.SM)
            backgroundColor(DesignTokens.Colors.surfaceBase)
        }
        event { click { onQuestion(question) } }
        Text {
            attr {
                flex(DesignTokens.Size.FILL)
                text(label)
                fontSize(DesignTokens.Typography.BODY)
                color(DesignTokens.Colors.onSurface)
            }
        }
        Text {
            attr {
                text("查看")
                fontSize(DesignTokens.Typography.CAPTION)
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
                text(if (isUser) "你的问题" else "分析结果")
                fontSize(DesignTokens.Typography.CAPTION)
                color(DesignTokens.Colors.onSurfaceMuted)
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
        chatStatusLabel("正在生成分析，连接异常时将自动切换本地", failed = false)
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
        chatStatusLabel("生成失败，点击重新连接并重试", failed = true)
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
