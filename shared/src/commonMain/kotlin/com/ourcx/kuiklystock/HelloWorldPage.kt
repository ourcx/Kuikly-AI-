package com.ourcx.kuiklystock

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.views.Text
import com.ourcx.kuiklystock.base.BasePager
import com.ourcx.kuiklystock.theme.DesignTokens

@Page("HelloWorld", supportInLocal = true)
internal class HelloWorldPage : BasePager() {

    override fun body(): ViewBuilder {
        return {
            attr {
                backgroundColor(DesignTokens.Colors.surfaceBase)
                allCenter()
            }

            Text {
                attr {
                    fontSize(DesignTokens.Typography.H2)
                    text("Hello, KuiklyStock!")
                    color(DesignTokens.Colors.onSurface)
                }
            }

            Text {
                attr {
                    fontSize(DesignTokens.Typography.BODY)
                    marginTop(DesignTokens.Spacing.SM)
                    text("Powered by Kuikly")
                    color(DesignTokens.Colors.onSurfaceMuted)
                }
            }
        }
    }
}
