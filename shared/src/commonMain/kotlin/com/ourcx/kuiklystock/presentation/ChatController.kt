package com.ourcx.kuiklystock.presentation

import com.ourcx.kuiklystock.data.ChatRepository
import com.ourcx.kuiklystock.data.InMemoryChatRepository
import com.ourcx.kuiklystock.domain.ChatContentBlock
import com.ourcx.kuiklystock.domain.ChatMessage
import com.ourcx.kuiklystock.domain.ChatMessageStatus
import com.ourcx.kuiklystock.domain.ChatRole
import com.ourcx.kuiklystock.domain.ChatState
import com.ourcx.kuiklystock.domain.buildChatContentBlocks
import com.ourcx.kuiklystock.domain.parseStockAiMetadata

class ChatController(
    private val chatRepository: ChatRepository = InMemoryChatRepository(),
    private val onStateChanged: (ChatState) -> Unit = {},
) {
    var state: ChatState = ChatState()
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
            ),
        )
        requestAssistant(question)
    }

    private fun requestAssistant(question: String) {
        runCatching { chatRepository.ask(question) }
            .onSuccess { rawContent ->
                val assistantMessage = ChatMessage(
                    id = newMessageId(),
                    role = ChatRole.ASSISTANT,
                    blocks = buildChatContentBlocks(parseStockAiMetadata(rawContent)),
                    status = ChatMessageStatus.COMPLETE,
                )
                updateState(
                    state.copy(
                        messages = state.messages + assistantMessage,
                        isSending = false,
                        error = null,
                    ),
                )
            }
            .onFailure { error ->
                val message = error.readableMessage()
                val failedAssistant = ChatMessage(
                    id = newMessageId(),
                    role = ChatRole.ASSISTANT,
                    blocks = listOf(ChatContentBlock.Markdown(message)),
                    status = ChatMessageStatus.FAILED,
                    retryQuestion = question,
                )
                updateState(
                    state.copy(
                        messages = state.messages + failedAssistant,
                        isSending = false,
                        error = message,
                    ),
                )
            }
    }

    private fun newMessageId(): String = "message-${nextMessageId++}"

    private fun updateState(newState: ChatState) {
        state = newState
        onStateChanged(newState)
    }
}
