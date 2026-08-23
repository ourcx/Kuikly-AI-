package com.ourcx.kuiklystock

import com.ourcx.kuiklystock.base.BasePager
import com.ourcx.kuiklystock.base.bridgeModule
import com.ourcx.kuiklystock.data.InMemoryStockRepository
import com.ourcx.kuiklystock.data.InMemoryChatRepository
import com.ourcx.kuiklystock.data.ResilientChatRepository
import com.ourcx.kuiklystock.data.WorkBuddyChatRepository
import com.ourcx.kuiklystock.domain.AppDestination
import com.ourcx.kuiklystock.domain.AppTab
import com.ourcx.kuiklystock.domain.MarketFilter
import com.ourcx.kuiklystock.domain.MarketSort
import com.ourcx.kuiklystock.domain.StockHomeState
import com.ourcx.kuiklystock.presentation.MarketDemoState
import com.ourcx.kuiklystock.presentation.StockHomeController
import com.ourcx.kuiklystock.theme.DesignTokens
import com.ourcx.kuiklystock.ui.component.aiResearchContentSlot
import com.ourcx.kuiklystock.ui.component.marketContentSlot
import com.ourcx.kuiklystock.ui.component.stockDetailContentSlot
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.directives.vbind
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View

@Page("StockHome", supportInLocal = true)
internal class StockHomePage : BasePager() {
    private var viewState: StockHomeState by observable(StockHomeState())

    private val controller by lazy {
        val bridgeModule = bridgeModule
        StockHomeController(
            stockRepository = InMemoryStockRepository(),
            chatRepository = ResilientChatRepository(
                remoteRepository = WorkBuddyChatRepository(
                    isConfigured = bridgeModule.isWorkBuddyConfigured(),
                    requestInvoker = bridgeModule::requestWorkBuddy,
                ),
                localRepository = InMemoryChatRepository(),
            ),
            onStateChanged = { state -> viewState = state },
        )
    }

    override fun created() {
        super.created()
        controller.marketController.load()
    }

    /** Builds the persistent shell and binds its replaceable content slots to controller state. */
    override fun body(): ViewBuilder {
        val context = this
        return {
            attr {
                flexDirectionColumn()
                backgroundColor(DesignTokens.Colors.surfaceBase)
            }

            View {
                attr {
                    height(pagerData.statusBarHeight)
                    backgroundColor(DesignTokens.Colors.primary)
                }
            }
            stockHomeHeader()

            vbind({ context.viewState }) {
                stockHomeContent(
                    state = context.viewState,
                    onBack = context.controller::backFromDetail,
                    onRetryMarket = context.controller.marketController::retry,
                    onSelectMarketDemo = context.controller.marketController::showDemoState,
                    onSelectStock = { symbol -> context.controller.selectStock(symbol) },
                    onUpdateMarketQuery = context.controller.marketController::updateQuery,
                    onSelectMarketFilter = context.controller.marketController::selectFilter,
                    onSelectMarketSort = context.controller.marketController::selectSort,
                    onToggleMarketFavoritesOnly = context.controller.marketController::toggleFavoritesOnly,
                    onClearMarketDiscoveryFilters = context.controller.marketController::clearDiscoveryFilters,
                    onToggleMarketFavorite = context.controller.marketController::toggleFavorite,
                    onAskAiAboutStock = context.controller::askAiAboutStock,
                    onUpdateChatDraft = context.controller.chatController::updateDraft,
                    onSendChat = context.controller.chatController::send,
                    onRetryChat = context.controller.chatController::retry,
                    onClearChat = context.controller.chatController::clearConversation,
                )
                stockHomeTabBar(
                    selectedTab = context.viewState.selectedTab,
                    onSelectTab = context.controller::selectTab,
                )
            }
        }
    }
}

/** Renders the route-level content slot selected by [StockHomeState]. */
fun ViewContainer<*, *>.stockHomeContent(
    state: StockHomeState,
    onBack: () -> Unit,
    onRetryMarket: () -> Unit,
    onSelectMarketDemo: (MarketDemoState) -> Unit,
    onSelectStock: (String) -> Unit,
    onUpdateMarketQuery: (String) -> Unit,
    onSelectMarketFilter: (MarketFilter) -> Unit,
    onSelectMarketSort: (MarketSort) -> Unit,
    onToggleMarketFavoritesOnly: () -> Unit,
    onClearMarketDiscoveryFilters: () -> Unit,
    onToggleMarketFavorite: (String) -> Unit,
    onAskAiAboutStock: (String) -> Unit,
    onUpdateChatDraft: (String) -> Unit,
    onSendChat: () -> Unit,
    onRetryChat: () -> Unit,
    onClearChat: () -> Unit,
) {
    View {
        attr {
            flex(DesignTokens.Size.FILL)
            padding(DesignTokens.Size.PAGE_GUTTER)
            backgroundColor(DesignTokens.Colors.surfaceBase)
        }
        when (state.destination) {
            AppDestination.Home -> when (state.selectedTab) {
                AppTab.MARKET -> marketContentSlot(
                    state = state.market,
                    onRetry = onRetryMarket,
                    onSelectDemo = onSelectMarketDemo,
                    onSelectStock = onSelectStock,
                    onUpdateQuery = onUpdateMarketQuery,
                    onSelectFilter = onSelectMarketFilter,
                    onSelectSort = onSelectMarketSort,
                    onToggleFavoritesOnly = onToggleMarketFavoritesOnly,
                    onClearDiscoveryFilters = onClearMarketDiscoveryFilters,
                    onToggleFavorite = onToggleMarketFavorite,
                    onAskAi = onAskAiAboutStock,
                )
                AppTab.AI -> aiResearchContentSlot(
                    state = state.chat,
                    marketState = state.market,
                    onUpdateDraft = onUpdateChatDraft,
                    onSend = onSendChat,
                    onRetry = onRetryChat,
                    onClear = onClearChat,
                    onSelectStock = onSelectStock,
                )
            }

            is AppDestination.Detail -> stockDetailContentSlot(
                state = state.detail,
                onBack = onBack,
            )
        }
    }
}

/** Renders the persistent two-tab navigation and reports selections to the controller. */
fun ViewContainer<*, *>.stockHomeTabBar(
    selectedTab: AppTab,
    onSelectTab: (AppTab) -> Unit,
) {
    View {
        attr {
            height(DesignTokens.Size.TAB_BAR)
            padding(
                top = DesignTokens.Spacing.XS,
                bottom = DesignTokens.Spacing.XS,
                left = DesignTokens.Size.PAGE_GUTTER,
                right = DesignTokens.Size.PAGE_GUTTER,
            )
            backgroundColor(DesignTokens.Colors.surfaceBase)
        }
        View {
            attr {
                flex(DesignTokens.Size.FILL)
                flexDirectionRow()
                borderRadius(DesignTokens.Radius.LG)
                padding(DesignTokens.Spacing.XXS)
                backgroundColor(DesignTokens.Colors.surfaceAlt)
            }
            stockHomeTabItem("行情", "MARKET", AppTab.MARKET, selectedTab, onSelectTab)
            stockHomeTabItem("AI 投研", "WORKBUDDY", AppTab.AI, selectedTab, onSelectTab)
        }
    }
}

private fun ViewContainer<*, *>.stockHomeHeader() {
    View {
        attr {
            height(DesignTokens.Size.HEADER)
            flexDirectionRow()
            alignItemsCenter()
            padding(left = DesignTokens.Size.PAGE_GUTTER, right = DesignTokens.Size.PAGE_GUTTER)
            backgroundColor(DesignTokens.Colors.primary)
        }
        View {
            attr {
                width(DesignTokens.Spacing.XL)
                height(DesignTokens.Spacing.XL)
                allCenter()
                borderRadius(DesignTokens.Radius.LG)
                backgroundColor(DesignTokens.Colors.accentPrimary)
                marginRight(DesignTokens.Spacing.SM)
            }
            Text {
                attr {
                    text("K")
                    fontSize(DesignTokens.Typography.H4)
                    fontWeightBold()
                    color(DesignTokens.Colors.onPrimary)
                }
            }
        }
        View {
            attr {
                flex(DesignTokens.Size.FILL)
            }
            Text {
                attr {
                    text("KuiklyStock")
                    fontSize(DesignTokens.Typography.H2)
                    fontWeightBold()
                    color(DesignTokens.Colors.onSurface)
                }
            }
            Text {
                attr {
                    text("跨端智能行情终端")
                    fontSize(DesignTokens.Typography.CAPTION)
                    color(DesignTokens.Colors.onSurfaceMuted)
                    marginTop(DesignTokens.Spacing.XXS)
                }
            }
        }
        View {
            attr {
                flexDirectionRow()
                alignItemsCenter()
                borderRadius(DesignTokens.Radius.FULL)
                padding(
                    top = DesignTokens.Spacing.XXS,
                    bottom = DesignTokens.Spacing.XXS,
                    left = DesignTokens.Spacing.SM,
                    right = DesignTokens.Spacing.SM,
                )
                backgroundColor(DesignTokens.Colors.surfaceElevated)
            }
            View {
                attr {
                    width(DesignTokens.Size.STATUS_DOT)
                    height(DesignTokens.Size.STATUS_DOT)
                    borderRadius(DesignTokens.Radius.FULL)
                    backgroundColor(DesignTokens.Colors.accentTertiary)
                    marginRight(DesignTokens.Spacing.XS)
                }
            }
            Text {
                attr {
                    text("在线")
                    fontSize(DesignTokens.Typography.CAPTION)
                    color(DesignTokens.Colors.onSurface)
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.stockHomeTabItem(
    label: String,
    eyebrow: String,
    tab: AppTab,
    selectedTab: AppTab,
    onSelectTab: (AppTab) -> Unit,
) {
    val selected = tab == selectedTab
    View {
        attr {
            flex(DesignTokens.Size.FILL)
            allCenter()
            borderRadius(DesignTokens.Radius.LG)
            backgroundColor(
                if (selected) DesignTokens.Colors.accentPrimary else DesignTokens.Colors.transparent,
            )
        }
        event { click { onSelectTab(tab) } }
        Text {
            attr {
                text(eyebrow)
                fontSize(DesignTokens.Typography.CAPTION)
                color(
                    if (selected) DesignTokens.Colors.onPrimary else DesignTokens.Colors.accentTertiary,
                )
            }
        }
        Text {
            attr {
                text(label)
                fontSize(DesignTokens.Typography.BODY_LARGE)
                color(
                    if (selected) DesignTokens.Colors.onPrimary else DesignTokens.Colors.onSurfaceMuted,
                )
                if (selected) {
                    fontWeightBold()
                }
            }
        }
    }
}
