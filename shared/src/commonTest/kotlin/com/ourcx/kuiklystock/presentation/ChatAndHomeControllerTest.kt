package com.ourcx.kuiklystock.presentation

import com.ourcx.kuiklystock.data.ChatRepository
import com.ourcx.kuiklystock.data.InMemoryStockRepository
import com.ourcx.kuiklystock.domain.AppDestination
import com.ourcx.kuiklystock.domain.AppTab
import com.ourcx.kuiklystock.domain.ChatContentBlock
import com.ourcx.kuiklystock.domain.ChatMessageStatus
import com.ourcx.kuiklystock.domain.ChatRole
import com.ourcx.kuiklystock.domain.LoadState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChatAndHomeControllerTest {
    @Test
    fun chatSendIgnoresBlankAndBuildsStructuredAssistantResponse() {
        val controller = ChatController(
            chatRepository = StubChatRepository {
                "正文<!--stock-ai:{\"symbols\":[\"AAPL\"],\"showTrend\":true}-->"
            },
        )

        controller.updateDraft("   " )
        controller.send()
        assertTrue(controller.state.messages.isEmpty())

        controller.updateDraft("  分析苹果  " )
        controller.send()

        assertEquals(2, controller.state.messages.size)
        assertEquals(ChatRole.USER, controller.state.messages[0].role)
        assertEquals("分析苹果", assertIs<ChatContentBlock.Markdown>(controller.state.messages[0].blocks.single()).text)
        val assistant = controller.state.messages[1]
        assertEquals(ChatMessageStatus.COMPLETE, assistant.status)
        assertEquals(3, assistant.blocks.size)
        assertFalse(controller.state.isSending)
        assertNull(controller.state.error)
    }

    @Test
    fun chatFailureCanRetryWithoutDuplicatingUserMessage() {
        var attempts = 0
        val controller = ChatController(StubChatRepository {
            attempts += 1
            if (attempts == 1) error("暂时失败") else "恢复成功"
        })

        controller.updateDraft("分析腾讯")
        controller.send()
        assertEquals(ChatMessageStatus.FAILED, controller.state.messages.last().status)
        assertEquals("暂时失败", controller.state.error)

        controller.retry()

        assertEquals(2, controller.state.messages.size)
        assertEquals(1, controller.state.messages.count { it.role == ChatRole.USER })
        assertEquals(ChatMessageStatus.COMPLETE, controller.state.messages.last().status)
        assertEquals(2, attempts)
    }

    @Test
    fun homeNavigationPreservesOriginTabAndClearsDetailOnExit() {
        val observedDestinations = mutableListOf<AppDestination>()
        val controller = StockHomeController(
            stockRepository = InMemoryStockRepository(),
            onStateChanged = { observedDestinations += it.destination },
        )

        controller.selectTab(AppTab.AI)
        val detail = controller.selectStock("AAPL")

        assertIs<LoadState.Content<*>>(detail.content)
        val destination = assertIs<AppDestination.Detail>(controller.state.destination)
        assertEquals(AppTab.AI, destination.previousTab)
        assertEquals("AAPL", destination.symbol)

        controller.backFromDetail()
        assertEquals(AppTab.AI, controller.state.selectedTab)
        assertIs<AppDestination.Home>(controller.state.destination)
        assertNull(controller.state.detail)
        assertTrue(observedDestinations.isNotEmpty())
    }

    @Test
    fun selectingTabAlwaysReturnsToItsHomeSurface() {
        val controller = StockHomeController()
        controller.selectStock("00700")

        controller.selectTab(AppTab.AI)

        assertEquals(AppTab.AI, controller.state.selectedTab)
        assertIs<AppDestination.Home>(controller.state.destination)
        assertNull(controller.state.detail)
    }
}

private class StubChatRepository(
    private val answer: (String) -> String,
) : ChatRepository {
    override fun ask(question: String): String = answer(question)
}
