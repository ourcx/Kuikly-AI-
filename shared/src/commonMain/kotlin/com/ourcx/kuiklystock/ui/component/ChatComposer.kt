package com.ourcx.kuiklystock.ui.component

import com.ourcx.kuiklystock.domain.ChatState
import com.ourcx.kuiklystock.theme.DesignTokens
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.views.Input
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View

/** Binds text input and submit events to the chat controller contract. */
internal fun ViewContainer<*, *>.chatComposer(
    state: ChatState,
    onUpdateDraft: (String) -> Unit,
    onSend: () -> Unit,
) {
    val canSend = state.draft.isNotBlank() && !state.isSending
    View {
        attr {
            flexDirectionRow()
            padding(DesignTokens.Spacing.MD)
            backgroundColor(DesignTokens.Colors.surfaceAlt)
        }
        View {
            attr {
                flex(DesignTokens.Size.FILL)
                height(DesignTokens.Size.CHAT_INPUT_HEIGHT)
                padding(DesignTokens.Spacing.XXS)
                borderRadius(DesignTokens.Radius.LG)
                backgroundColor(
                    if (state.isSending) {
                        DesignTokens.Colors.borderStrong
                    } else {
                        DesignTokens.Colors.accentTertiary
                    },
                )
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
                        text(state.draft)
                        placeholder(
                            if (state.isSending) "正在生成回答…" else "输入你的投研问题",
                        )
                        placeholderColor(DesignTokens.Colors.onSurfaceMuted)
                        color(DesignTokens.Colors.onSurface)
                        fontSize(DesignTokens.Typography.BODY)
                        returnKeyTypeSend()
                    }
                    event {
                        textDidChange { params -> onUpdateDraft(params.text) }
                        inputReturn { if (canSend) onSend() }
                    }
                }
            }
        }
        View {
            attr {
                width(DesignTokens.Size.CHAT_SEND_WIDTH)
                height(DesignTokens.Size.CHAT_INPUT_HEIGHT)
                allCenter()
                borderRadius(DesignTokens.Radius.LG)
                marginLeft(DesignTokens.Spacing.SM)
                backgroundColor(
                    if (canSend) {
                        DesignTokens.Colors.accentPrimary
                    } else {
                        DesignTokens.Colors.surfaceElevated
                    },
                )
            }
            event { click { if (canSend) onSend() } }
            Text {
                attr {
                    text(if (state.isSending) "生成中…" else "发送")
                    fontSize(DesignTokens.Typography.BODY_LARGE)
                    fontWeightBold()
                    color(
                        if (canSend) {
                            DesignTokens.Colors.onPrimary
                        } else {
                            DesignTokens.Colors.onSurfaceMuted
                        },
                    )
                }
            }
        }
    }
}
