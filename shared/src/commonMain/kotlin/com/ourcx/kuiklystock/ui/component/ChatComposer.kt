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
    View {
        attr {
            flexDirectionRow()
            padding(DesignTokens.Spacing.MD)
            backgroundColor(DesignTokens.Colors.surface)
        }
        View {
            attr {
                flex(DesignTokens.Size.FILL)
                height(DesignTokens.Size.CHAT_INPUT_HEIGHT)
                padding(left = DesignTokens.Spacing.SM, right = DesignTokens.Spacing.SM)
                borderRadius(DesignTokens.Radius.MD)
                backgroundColor(DesignTokens.Colors.surfaceElevated)
            }
            Input {
                attr {
                    flex(DesignTokens.Size.FILL)
                    height(DesignTokens.Size.CHAT_INPUT_HEIGHT)
                    backgroundColor(DesignTokens.Colors.surfaceElevated)
                    text(state.draft)
                    placeholder("输入你的投研问题")
                    placeholderColor(DesignTokens.Colors.textMuted)
                    color(DesignTokens.Colors.textPrimary)
                    fontSize(DesignTokens.Typography.BODY)
                    returnKeyTypeSend()
                }
                event {
                    textDidChange { params -> onUpdateDraft(params.text) }
                    inputReturn { onSend() }
                }
            }
        }
        View {
            attr {
                width(DesignTokens.Size.CHAT_SEND_WIDTH)
                height(DesignTokens.Size.CHAT_INPUT_HEIGHT)
                allCenter()
                borderRadius(DesignTokens.Radius.MD)
                marginLeft(DesignTokens.Spacing.SM)
                backgroundColor(
                    if (state.isSending || state.draft.isBlank()) {
                        DesignTokens.Colors.surfaceElevated
                    } else {
                        DesignTokens.Colors.accent
                    },
                )
            }
            event { click { onSend() } }
            Text {
                attr {
                    text(if (state.isSending) "生成中" else "发送")
                    fontSize(DesignTokens.Typography.LABEL)
                    fontWeightBold()
                    color(
                        if (state.isSending || state.draft.isBlank()) {
                            DesignTokens.Colors.textMuted
                        } else {
                            DesignTokens.Colors.textPrimary
                        },
                    )
                }
            }
        }
    }
}
