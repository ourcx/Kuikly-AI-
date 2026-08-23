package com.ourcx.kuiklystock.ui.component

import com.ourcx.kuiklystock.domain.ChatContentBlock
import com.ourcx.kuiklystock.domain.ChatMessage
import com.ourcx.kuiklystock.domain.ChatMessageStatus
import com.ourcx.kuiklystock.domain.ChatRole
import com.ourcx.kuiklystock.domain.ChatState
import com.ourcx.kuiklystock.domain.LoadState
import com.ourcx.kuiklystock.domain.MarketState
import com.ourcx.kuiklystock.domain.StockQuote
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

/** Builds the AI research conversation and keeps all mutations at controller boundaries. */
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
            backgroundColor(DesignTokens.Colors.surface)
        }
        chatHeader()
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

private fun ViewContainer<*, *>.chatHeader() {
    View {
        attr {
            padding(DesignTokens.Spacing.MD)
            backgroundColor(DesignTokens.Colors.surface)
        }
        Text {
            attr {
                text("AI 投研助手")
                fontSize(DesignTokens.Typography.TITLE)
                fontWeightBold()
                color(DesignTokens.Colors.textPrimary)
            }
        }
        Text {
            attr {
                text("结合行情卡片与趋势图，快速梳理股票信息")
                fontSize(DesignTokens.Typography.CAPTION)
                color(DesignTokens.Colors.textSecondary)
                marginTop(DesignTokens.Spacing.XXS)
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
            backgroundColor(DesignTokens.Colors.background)
        }
        if (state.messages.isEmpty() && !state.isSending) {
            chatWelcome(onQuestion = { question ->
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

private fun ViewContainer<*, *>.chatWelcome(onQuestion: (String) -> Unit) {
    View {
        attr {
            padding(DesignTokens.Spacing.LG)
            borderRadius(DesignTokens.Radius.MD)
            backgroundColor(DesignTokens.Colors.surfaceElevated)
        }
        Text {
            attr {
                text("从一个问题开始")
                fontSize(DesignTokens.Typography.LABEL)
                fontWeightBold()
                color(DesignTokens.Colors.textPrimary)
            }
        }
        Text {
            attr {
                text("可输入股票名称、代码，或选择下方示例。")
                fontSize(DesignTokens.Typography.BODY)
                color(DesignTokens.Colors.textSecondary)
                marginTop(DesignTokens.Spacing.XS)
            }
        }
        suggestionChip("分析腾讯控股", onQuestion)
        suggestionChip("AAPL 最近走势如何？", onQuestion)
    }
}

private fun ViewContainer<*, *>.suggestionChip(question: String, onQuestion: (String) -> Unit) {
    View {
        attr {
            padding(DesignTokens.Spacing.SM)
            borderRadius(DesignTokens.Radius.PILL)
            backgroundColor(DesignTokens.Colors.accentMuted)
            marginTop(DesignTokens.Spacing.SM)
        }
        event { click { onQuestion(question) } }
        Text {
            attr {
                text(question)
                fontSize(DesignTokens.Typography.BODY)
                color(DesignTokens.Colors.accent)
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
            borderRadius(DesignTokens.Radius.MD)
            backgroundColor(
                if (isUser) DesignTokens.Colors.accentMuted else DesignTokens.Colors.surface,
            )
        }
        Text {
            attr {
                text(if (isUser) "你" else "AI 助手")
                fontSize(DesignTokens.Typography.CAPTION)
                fontWeightBold()
                color(if (isUser) DesignTokens.Colors.accent else DesignTokens.Colors.textMuted)
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
                backgroundColor(DesignTokens.Colors.surfaceElevated)
                borderRadius(DesignTokens.Radius.SM)
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
            borderRadius(DesignTokens.Radius.MD)
            backgroundColor(DesignTokens.Colors.surface)
        }
        chatStatusLabel("AI 助手正在生成回答…", failed = false)
    }
}

private fun ViewContainer<*, *>.chatStatusLabel(label: String, failed: Boolean) {
    Text {
        attr {
            text(label)
            fontSize(DesignTokens.Typography.BODY)
            color(if (failed) DesignTokens.Colors.rise else DesignTokens.Colors.textSecondary)
        }
    }
}

private fun ViewContainer<*, *>.chatRetryAction(onRetry: () -> Unit) {
    View {
        attr {
            padding(DesignTokens.Spacing.SM)
            borderRadius(DesignTokens.Radius.PILL)
            backgroundColor(DesignTokens.Colors.accentMuted)
            marginTop(DesignTokens.Spacing.SM)
        }
        event { click { onRetry() } }
        chatStatusLabel("生成失败 · 点击重试", failed = true)
    }
}

private fun ViewContainer<*, *>.missingQuote(symbol: String) {
    Text {
        attr {
            text("暂未找到 $symbol 的行情数据")
            fontSize(DesignTokens.Typography.BODY)
            color(DesignTokens.Colors.textSecondary)
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
        text = DesignTokens.ColorValues.TEXT_PRIMARY,
        codeBackground = DesignTokens.ColorValues.BACKGROUND,
        inlineCodeBackground = DesignTokens.ColorValues.SURFACE,
        dividerColor = DesignTokens.ColorValues.BORDER,
        tableBackground = DesignTokens.ColorValues.SURFACE,
        blockQuoteBar = DesignTokens.ColorValues.ACCENT,
        blockQuoteBackground = DesignTokens.ColorValues.ACCENT_MUTED,
        linkColor = DesignTokens.ColorValues.ACCENT,
        codeText = DesignTokens.ColorValues.TEXT_SECONDARY,
    ),
    typography = MarkdownTypography(
        text = TextStyleConfig(fontSize = DesignTokens.Typography.BODY),
        paragraph = TextStyleConfig(fontSize = DesignTokens.Typography.BODY),
        h1 = TextStyleConfig(fontSize = DesignTokens.Typography.BRAND),
        h2 = TextStyleConfig(fontSize = DesignTokens.Typography.TITLE),
        h3 = TextStyleConfig(fontSize = DesignTokens.Typography.LABEL),
        quote = TextStyleConfig(fontSize = DesignTokens.Typography.BODY),
        list = TextStyleConfig(fontSize = DesignTokens.Typography.BODY),
        textLink = TextStyleConfig(fontSize = DesignTokens.Typography.BODY),
    ),
    codeHighlightDarkTheme = true,
)
