package com.ourcx.kuiklystock.presentation

import com.ourcx.kuiklystock.data.ChatRepository
import com.ourcx.kuiklystock.data.InMemoryChatRepository
import com.ourcx.kuiklystock.data.ResilientChatRepository
import com.ourcx.kuiklystock.data.ResearchServiceConfiguration
import com.ourcx.kuiklystock.data.OpenAiChatRepository
import com.ourcx.kuiklystock.domain.ChatContext
import com.ourcx.kuiklystock.domain.ChatContentBlock
import com.ourcx.kuiklystock.domain.ChatMessage
import com.ourcx.kuiklystock.domain.ChatMessageStatus
import com.ourcx.kuiklystock.domain.ChatProvider
import com.ourcx.kuiklystock.domain.ChatRequest
import com.ourcx.kuiklystock.domain.ChatRole
import com.ourcx.kuiklystock.domain.ChatState
import com.ourcx.kuiklystock.domain.ParsedStockAiContent
import com.ourcx.kuiklystock.domain.WorkBuddyConnectionStatus
import com.ourcx.kuiklystock.domain.buildChatContentBlocks

class ChatController(
    private val chatRepository: ChatRepository = InMemoryChatRepository(),
    private val onStateChanged: (ChatState) -> Unit = {},
    private val contextProvider: () -> ChatContext = { ChatContext() },
    private val serviceConfiguration: ResearchServiceConfiguration? = null,
) {
    var state: ChatState = initialState()
        private set

    private var nextMessageId = 1L

    fun updateDraft(draft: String) {
        updateState(state.copy(draft = draft, error = null))
    }

    fun send() {
        if (state.isSending) return

        val question = state.draft.trim()
        if (question.isEmpty()) return

        val userMessage = ChatMessage(
            id = newMessageId(),
            role = ChatRole.USER,
            blocks = listOf(ChatContentBlock.Markdown(question)),
            status = ChatMessageStatus.COMPLETE,
        )
        updateState(
            state.copy(
                messages = state.messages + userMessage,
                draft = "",
                isSending = true,
                error = null,
                connectionStatus = WorkBuddyConnectionStatus.SENDING,
            ),
        )
        requestAssistant(question)
    }

    fun retry() {
        if (state.isSending) return

        val failedIndex = state.messages.indexOfLast { message ->
            message.role == ChatRole.ASSISTANT &&
                message.status == ChatMessageStatus.FAILED &&
                !message.retryQuestion.isNullOrBlank()
        }
        if (failedIndex < 0) return

        val question = state.messages[failedIndex].retryQuestion ?: return
        updateState(
            state.copy(
                messages = state.messages.filterIndexed { index, _ -> index != failedIndex },
                isSending = true,
                error = null,
                connectionStatus = WorkBuddyConnectionStatus.SENDING,
            ),
        )
        requestAssistant(question)
    }

    /**
     * Observable contract:
     * - Ignores clear attempts while a request is in flight.
     * - Resets transient chat state to the repository-backed initial experience.
     * - Preserves retry/dedup behavior by leaving message id generation monotonic.
     */
    fun clearConversation() {
        if (state.isSending) return
        updateState(initialState())
    }

    fun toggleServiceSettings() {
        if (state.isSending) return
        val visible = !state.serviceSettingsVisible
        updateState(
            state.copy(
                serviceSettingsVisible = visible,
                serviceUrlDraft = if (visible) serviceConfiguration?.currentUrl().orEmpty() else "",
                serviceSettingsError = null,
            ),
        )
    }

    fun updateServiceUrl(url: String) {
        updateState(state.copy(serviceUrlDraft = url, serviceSettingsError = null))
    }

    fun saveServiceUrl() {
        val configuration = serviceConfiguration ?: return
        val url = state.serviceUrlDraft.trim()
        configuration.save(url).fold(
            onSuccess = {
                updateState(
                    state.copy(
                        serviceSettingsVisible = false,
                        serviceUrlDraft = "",
                        serviceSettingsError = null,
                        connectionStatus = WorkBuddyConnectionStatus.AVAILABLE,
                    ),
                )
            },
            onFailure = { error ->
                updateState(state.copy(serviceSettingsError = error.readableMessage()))
            },
        )
    }

    fun clearServiceUrl() {
        val configuration = serviceConfiguration ?: return
        configuration.clear()
        val remainsConfigured = configuration.isConfigured
        updateState(
            state.copy(
                serviceSettingsVisible = false,
                serviceUrlDraft = "",
                serviceSettingsError = null,
                connectionStatus = if (remainsConfigured) {
                    WorkBuddyConnectionStatus.AVAILABLE
                } else {
                    WorkBuddyConnectionStatus.UNCONFIGURED
                },
                provider = ChatProvider.LOCAL,
                conversationId = null,
            ),
        )
    }

    private fun requestAssistant(question: String) {
        val assistantMessageId = newMessageId()
        updateState(
            state.copy(
                messages = state.messages + ChatMessage(
                    id = assistantMessageId,
                    role = ChatRole.ASSISTANT,
                    blocks = emptyList(),
                    status = ChatMessageStatus.GENERATING,
                ),
            ),
        )

        if (!chatRepository.isConfigured) {
            completeWithFailure(assistantMessageId, question, UNCONFIGURED_MESSAGE)
            return
        }

        val request = runCatching {
            ChatRequest(
                question = question,
                conversationId = state.conversationId,
                context = contextProvider(),
            )
        }.getOrElse { error ->
            completeWithFailure(assistantMessageId, question, error.readableMessage())
            return
        }
        runCatching {
            chatRepository.ask(request) { result ->
                result.fold(
                    onSuccess = { response ->
                        val assistantMessage = ChatMessage(
                            id = assistantMessageId,
                            role = ChatRole.ASSISTANT,
                            blocks = buildChatContentBlocks(
                                ParsedStockAiContent(
                                    markdown = response.answer,
                                    symbols = response.symbols,
                                    showTrend = response.showTrend,
                                ),
                            ),
                            status = ChatMessageStatus.COMPLETE,
                        )
                        updateState(
                            state.copy(
                                messages = state.messages.replaceMessage(assistantMessage),
                                isSending = false,
                                error = null,
                                conversationId = response.conversationId ?: state.conversationId,
                                connectionStatus = response.connectionStatus(),
                                provider = response.provider,
                            ),
                        )
                    },
                    onFailure = { error ->
                        completeWithFailure(assistantMessageId, question, error.readableMessage())
                    },
                )
            }
        }.onFailure { error ->
            completeWithFailure(assistantMessageId, question, error.readableMessage())
        }
    }

    private fun completeWithFailure(assistantMessageId: String, question: String, message: String) {
        val failedAssistant = ChatMessage(
            id = assistantMessageId,
            role = ChatRole.ASSISTANT,
            blocks = listOf(ChatContentBlock.Markdown(message)),
            status = ChatMessageStatus.FAILED,
            retryQuestion = question,
        )
        updateState(
            state.copy(
                messages = state.messages.replaceMessage(failedAssistant),
                isSending = false,
                error = message,
                connectionStatus = WorkBuddyConnectionStatus.ERROR,
            ),
        )
    }

    private fun initialState(): ChatState = ChatState(
        connectionStatus = initialConnectionStatus(),
        provider = initialProvider(),
        serviceUrlDraft = serviceConfiguration?.currentUrl().orEmpty(),
    )

    private fun initialConnectionStatus(): WorkBuddyConnectionStatus {
        val openAiConfigured = serviceConfiguration?.isConfigured ?: when (chatRepository) {
            is ResilientChatRepository -> chatRepository.isRemoteConfigured
            is InMemoryChatRepository -> false
            else -> chatRepository.isConfigured
        }
        return if (openAiConfigured) {
            WorkBuddyConnectionStatus.AVAILABLE
        } else {
            WorkBuddyConnectionStatus.UNCONFIGURED
        }
    }

    private fun initialProvider(): ChatProvider = when (chatRepository) {
        is InMemoryChatRepository, is ResilientChatRepository -> ChatProvider.LOCAL
        is OpenAiChatRepository -> ChatProvider.OPENAI
        else -> if (chatRepository.isConfigured) ChatProvider.OPENAI else ChatProvider.LOCAL
    }

    private fun com.ourcx.kuiklystock.domain.ChatResponse.connectionStatus(): WorkBuddyConnectionStatus =
        when {
            provider == ChatProvider.OPENAI -> WorkBuddyConnectionStatus.AVAILABLE
            serviceConfiguration?.isConfigured == true -> WorkBuddyConnectionStatus.ERROR
            else -> WorkBuddyConnectionStatus.UNCONFIGURED
        }

    private fun newMessageId(): String = "message-${nextMessageId++}"

    private fun updateState(newState: ChatState) {
        state = newState
        onStateChanged(newState)
    }
}

private fun List<ChatMessage>.replaceMessage(replacement: ChatMessage): List<ChatMessage> =
    map { message -> if (message.id == replacement.id) replacement else message }

private const val UNCONFIGURED_MESSAGE = "OpenAI 服务尚未配置，请先配置 HTTPS 代理地址"
