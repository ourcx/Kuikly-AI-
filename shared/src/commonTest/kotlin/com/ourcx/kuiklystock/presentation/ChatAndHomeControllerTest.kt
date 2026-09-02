package com.ourcx.kuiklystock.presentation

import com.ourcx.kuiklystock.data.ChatRepository
import com.ourcx.kuiklystock.data.InMemoryChatRepository
import com.ourcx.kuiklystock.data.InMemoryStockRepository
import com.ourcx.kuiklystock.data.ResearchServiceConfiguration
import com.ourcx.kuiklystock.domain.AppDestination
import com.ourcx.kuiklystock.domain.AppTab
import com.ourcx.kuiklystock.domain.ChatContentBlock
import com.ourcx.kuiklystock.domain.ChatMessageStatus
import com.ourcx.kuiklystock.domain.ChatProvider
import com.ourcx.kuiklystock.domain.ChatRequest
import com.ourcx.kuiklystock.domain.ChatResponse
import com.ourcx.kuiklystock.domain.ChatRole
import com.ourcx.kuiklystock.domain.LoadState
import com.ourcx.kuiklystock.domain.WorkBuddyConnectionStatus
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
            chatRepository = StubChatRepository { request ->
                Result.success(
                    ChatResponse(
                        answer = "Structured answer",
                        conversationId = "conversation-1",
                        symbols = listOf("AAPL"),
                        showTrend = true,
                    ),
                )
            },
        )

        controller.updateDraft("   " )
        controller.send()
        assertTrue(controller.state.messages.isEmpty())

        controller.updateDraft("  Analyze Apple  " )
        controller.send()

        assertEquals(2, controller.state.messages.size)
        assertEquals(ChatRole.USER, controller.state.messages[0].role)
        assertEquals("Analyze Apple", assertIs<ChatContentBlock.Markdown>(controller.state.messages[0].blocks.single()).text)
        val assistant = controller.state.messages[1]
        assertEquals(ChatMessageStatus.COMPLETE, assistant.status)
        assertEquals(3, assistant.blocks.size)
        assertEquals("conversation-1", controller.state.conversationId)
        assertEquals(WorkBuddyConnectionStatus.AVAILABLE, controller.state.connectionStatus)
        assertFalse(controller.state.isSending)
        assertNull(controller.state.error)
    }

    @Test
    fun chatFailureCanRetryWithoutDuplicatingUserMessage() {
        var attempts = 0
        val controller = ChatController(StubChatRepository { _ ->
            attempts += 1
            if (attempts == 1) Result.failure(IllegalStateException("Temporary failure"))
            else Result.success(ChatResponse(answer = "Recovered"))
        })

        controller.updateDraft("Analyze Tencent")
        controller.send()
        assertEquals(ChatMessageStatus.FAILED, controller.state.messages.last().status)
        assertEquals("Temporary failure", controller.state.error)

        controller.retry()

        assertEquals(2, controller.state.messages.size)
        assertEquals(1, controller.state.messages.count { it.role == ChatRole.USER })
        assertEquals(ChatMessageStatus.COMPLETE, controller.state.messages.last().status)
        assertEquals(2, attempts)
    }

    @Test
    fun chatSendPreventsDuplicateRequestsAndContinuesConversation() {
        val repository = DeferredChatRepository()
        val controller = ChatController(chatRepository = repository)

        controller.updateDraft("First question")
        controller.send()
        controller.updateDraft("Ignored while sending")
        controller.send()

        assertEquals(1, repository.requests.size)
        assertEquals(WorkBuddyConnectionStatus.SENDING, controller.state.connectionStatus)
        repository.complete(ChatResponse(answer = "First answer", conversationId = "conversation-7"))

        controller.updateDraft("Follow up")
        controller.send()

        assertEquals("conversation-7", repository.requests.last().conversationId)
    }

    @Test
    fun unconfiguredChatReportsConfigurationErrorWithoutCallingRepository() {
        val repository = DeferredChatRepository(isConfigured = false)
        val controller = ChatController(chatRepository = repository)

        controller.updateDraft("Analyze AAPL")
        controller.send()

        assertTrue(repository.requests.isEmpty())
        assertEquals(ChatMessageStatus.FAILED, controller.state.messages.last().status)
        assertEquals(WorkBuddyConnectionStatus.ERROR, controller.state.connectionStatus)
        assertFalse(controller.state.isSending)
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

    @Test
    fun clearConversationResetsMessagesAndKeepsLocalProviderAvailable() {
        val controller = ChatController(InMemoryChatRepository())
        controller.updateDraft("分析 AAPL")
        controller.send()
        assertTrue(controller.state.messages.isNotEmpty())
        assertEquals(ChatProvider.LOCAL, controller.state.provider)

        controller.clearConversation()

        assertTrue(controller.state.messages.isEmpty())
        assertEquals("", controller.state.draft)
        assertEquals(ChatProvider.LOCAL, controller.state.provider)
        assertEquals(WorkBuddyConnectionStatus.UNCONFIGURED, controller.state.connectionStatus)
    }

    @Test
    fun clearConversationResetsDraftErrorAndConversationIdAfterCompletedTurns() {
        var attempts = 0
        val controller = ChatController(StubChatRepository { _ ->
            attempts += 1
            if (attempts == 1) {
                Result.success(ChatResponse(answer = "Complete", conversationId = "conversation-9"))
            } else {
                Result.failure(IllegalStateException("Unavailable"))
            }
        })
        controller.updateDraft("First question")
        controller.send()
        controller.updateDraft("Second question")
        controller.send()
        controller.updateDraft("Unsent draft")

        controller.clearConversation()

        assertTrue(controller.state.messages.isEmpty())
        assertEquals("", controller.state.draft)
        assertNull(controller.state.error)
        assertNull(controller.state.conversationId)
        assertFalse(controller.state.isSending)
    }

    @Test
    fun clearConversationIsIgnoredWhileRequestIsInFlight() {
        val repository = DeferredChatRepository()
        val controller = ChatController(repository)
        controller.updateDraft("Pending question")
        controller.send()
        val pendingState = controller.state

        controller.clearConversation()

        assertEquals(pendingState, controller.state)
        assertTrue(controller.state.isSending)
    }

    @Test
    fun oneTapAiSwitchesTabAndSendsStockSpecificQuestion() {
        val repository = DeferredChatRepository()
        val controller = StockHomeController(
            stockRepository = InMemoryStockRepository(),
            chatRepository = repository,
        )

        controller.askAiAboutStock("AAPL")

        assertEquals(AppTab.AI, controller.state.selectedTab)
        assertIs<AppDestination.Home>(controller.state.destination)
        assertEquals(1, repository.requests.size)
        assertTrue(repository.requests.single().question.contains("苹果（AAPL）"))
    }

    @Test
    fun oneTapAiIgnoresBlankAndUnknownSymbolsWithoutChangingHomeState() {
        val repository = DeferredChatRepository()
        val controller = StockHomeController(
            stockRepository = InMemoryStockRepository(),
            chatRepository = repository,
        )
        controller.selectStock("AAPL")
        val originalState = controller.state

        controller.askAiAboutStock("   ")
        controller.askAiAboutStock("UNKNOWN")

        assertEquals(originalState, controller.state)
        assertTrue(repository.requests.isEmpty())
    }

    @Test
    fun serviceSettingsSaveAndClearRefreshConnectionState() {
        val configuration = FakeServiceConfiguration()
        val controller = ChatController(
            chatRepository = InMemoryChatRepository(),
            serviceConfiguration = configuration,
        )

        controller.toggleServiceSettings()
        controller.updateServiceUrl("https://proxy.example.com/chat")
        controller.saveServiceUrl()

        assertEquals("https://proxy.example.com/chat", configuration.url)
        assertFalse(controller.state.serviceSettingsVisible)
        assertEquals(WorkBuddyConnectionStatus.AVAILABLE, controller.state.connectionStatus)

        controller.clearServiceUrl()
        assertEquals("", configuration.url)
        assertEquals(WorkBuddyConnectionStatus.UNCONFIGURED, controller.state.connectionStatus)
        assertEquals(ChatProvider.LOCAL, controller.state.provider)
    }

    @Test
    fun invalidServiceSettingsRemainVisibleWithReadableError() {
        val configuration = FakeServiceConfiguration()
        val controller = ChatController(
            chatRepository = InMemoryChatRepository(),
            serviceConfiguration = configuration,
        )

        controller.toggleServiceSettings()
        controller.updateServiceUrl("http://unsafe.example.com")
        controller.saveServiceUrl()

        assertTrue(controller.state.serviceSettingsVisible)
        assertEquals("请输入有效的 HTTPS 服务地址", controller.state.serviceSettingsError)
        assertEquals("", configuration.url)
    }

    @Test
    fun configuredServiceLocalFallbackIsReportedAsRemoteError() {
        val configuration = FakeServiceConfiguration().apply {
            url = "https://proxy.example.com/chat"
        }
        val controller = ChatController(
            chatRepository = InMemoryChatRepository(),
            serviceConfiguration = configuration,
        )

        controller.updateDraft("分析 AAPL")
        controller.send()

        assertEquals(ChatProvider.LOCAL, controller.state.provider)
        assertEquals(WorkBuddyConnectionStatus.ERROR, controller.state.connectionStatus)
    }
}

private class FakeServiceConfiguration : ResearchServiceConfiguration {
    var url: String = ""
    override val isConfigured: Boolean
        get() = url.isNotEmpty()

    override fun currentUrl(): String = url

    override fun save(url: String): Result<Unit> {
        if (!url.startsWith("https://")) {
            return Result.failure(IllegalArgumentException("请输入有效的 HTTPS 服务地址"))
        }
        this.url = url
        return Result.success(Unit)
    }

    override fun clear() {
        url = ""
    }
}

private class StubChatRepository(
    private val answer: (ChatRequest) -> Result<ChatResponse>,
) : ChatRepository {
    override val isConfigured: Boolean = true

    override fun ask(request: ChatRequest, callback: (Result<ChatResponse>) -> Unit) {
        callback(answer(request))
    }
}

private class DeferredChatRepository(
    override val isConfigured: Boolean = true,
) : ChatRepository {
    val requests = mutableListOf<ChatRequest>()
    private var callback: ((Result<ChatResponse>) -> Unit)? = null

    override fun ask(request: ChatRequest, callback: (Result<ChatResponse>) -> Unit) {
        requests += request
        this.callback = callback
    }

    fun complete(response: ChatResponse) {
        requireNotNull(callback).invoke(Result.success(response))
    }
}
