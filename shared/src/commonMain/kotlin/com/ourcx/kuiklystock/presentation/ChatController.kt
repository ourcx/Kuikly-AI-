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
        val settings = serviceConfiguration?.current()
        updateState(
            state.copy(
                serviceSettingsVisible = visible,
                serviceUrlDraft = if (visible) settings?.baseUrl.orEmpty() else "",
                serviceTokenDraft = "",
                serviceModelDraft = if (visible) settings?.model.orEmpty() else "",
                serviceTokenConfigured = visible && settings?.hasToken == true,
                serviceSettingsError = null,
            ),
        )
    }

    fun updateServiceUrl(url: String) {
        updateState(state.copy(serviceUrlDraft = url, serviceSettingsError = null))
    }

    fun updateServiceToken(token: String) {
        updateState(state.copy(serviceTokenDraft = token, serviceSettingsError = null))
    }

    fun updateServiceModel(model: String) {
        updateState(state.copy(serviceModelDraft = model, serviceSettingsError = null))
    }

    fun saveServiceUrl() {
        val configuration = serviceConfiguration ?: return
        configuration.save(
            baseUrl = state.serviceUrlDraft.trim(),
            token = state.serviceTokenDraft.trim(),
            model = state.serviceModelDraft.trim(),
        ).fold(
            onSuccess = {
                updateState(
                    state.copy(
                        serviceSettingsVisible = false,
                        serviceUrlDraft = "",
                        serviceTokenDraft = "",
                        serviceModelDraft = "",
                        serviceTokenConfigured = true,
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
                serviceTokenDraft = "",
                serviceModelDraft = "",
                serviceTokenConfigured = false,
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
        val streamedAnswer = StringBuilder()
        var completed = false
        runCatching {
            chatRepository.askStreaming(
                request = request,
                onDelta = { delta ->
                    if (completed || delta.isEmpty()) return@askStreaming
                    streamedAnswer.append(delta)
                    val generatingAssistant = ChatMessage(
                        id = assistantMessageId,
                        role = ChatRole.ASSISTANT,
                        blocks = buildChatContentBlocks(ParsedStockAiContent(streamedAnswer.toString())),
                        status = ChatMessageStatus.GENERATING,
                    )
                    updateState(state.copy(messages = state.messages.replaceMessage(generatingAssistant)))
                },
            ) { result ->
                if (completed) return@askStreaming
                completed = true
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
            blocks = listOf(ChatContentBlock.Error(message)),
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

    private fun initialState(): ChatState {
        val settings = serviceConfiguration?.current()
        return ChatState(
            connectionStatus = initialConnectionStatus(),
            provider = initialProvider(),
            serviceUrlDraft = settings?.baseUrl.orEmpty(),
            serviceModelDraft = settings?.model.orEmpty(),
            serviceTokenConfigured = settings?.hasToken == true,
        )
    }

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

private const val UNCONFIGURED_MESSAGE = "OpenAI 服务尚未配置，请先填写 Base URL、Token 和 Model"
