package org.example.project.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.domain.usecase.AskRagQuestionUseCase
import org.example.project.models.RagAnswer
import org.example.project.models.RagSourceReference

data class ChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val sources: List<RagSourceReference> = emptyList(),
    val isInsufficientData: Boolean = false,
    val isError: Boolean = false
)

data class AskPanelUiState(
    val messages: List<ChatMessage> = emptyList(),
    val currentInput: String = "",
    val isThinking: Boolean = false,
    val isPanelOpen: Boolean = false
)

class AskPanelViewModel(
    private val askRagQuestionUseCase: AskRagQuestionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AskPanelUiState(
            messages = listOf(
                ChatMessage(
                    id = "welcome",
                    text = "Hello! I am the science assistant for NASA microgravity combustion experiments. Ask me about the real experiments in the database (FLEX, SAFFIRE, BASS and more).",
                    isUser = false
                )
            )
        )
    )
    val uiState: StateFlow<AskPanelUiState> = _uiState.asStateFlow()

    fun onInputChange(input: String) {
        _uiState.update { it.copy(currentInput = input) }
    }

    fun togglePanel() {
        _uiState.update { it.copy(isPanelOpen = !it.isPanelOpen) }
    }

    fun setPanelOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isPanelOpen = isOpen) }
    }

    fun sendQuestion(questionText: String? = null) {
        val query = (questionText ?: _uiState.value.currentInput).trim()
        if (query.isBlank() || _uiState.value.isThinking) return

        val userMessageId = "user_${query.hashCode()}_${_uiState.value.messages.size}"
        val userMsg = ChatMessage(
            id = userMessageId,
            text = query,
            isUser = true
        )

        _uiState.update {
            it.copy(
                messages = it.messages + userMsg,
                currentInput = "",
                isThinking = true
            )
        }

        viewModelScope.launch {
            val result = askRagQuestionUseCase(query)

            result.fold(
                onSuccess = { answer: RagAnswer ->
                    val isInsufficient = answer.isInsufficientData ||
                            (answer.answer.length < 200 && (
                                answer.answer.contains("insufficient data", ignoreCase = true) ||
                                answer.answer.contains("yeterli veri bulunamadı", ignoreCase = true)))

                    val aiMsg = ChatMessage(
                        id = "ai_${answer.hashCode()}_${_uiState.value.messages.size}",
                        text = if (isInsufficient) "The dataset does not contain enough information on this topic." else answer.answer,
                        isUser = false,
                        sources = answer.sources,
                        isInsufficientData = isInsufficient
                    )

                    _uiState.update {
                        it.copy(
                            messages = it.messages + aiMsg,
                            isThinking = false
                        )
                    }
                },
                onFailure = { error: Throwable ->
                    val errorMsg = ChatMessage(
                        id = "err_${error.hashCode()}_${_uiState.value.messages.size}",
                        text = "An error occurred while sending the question: ${error.message ?: "Unknown error"}",
                        isUser = false,
                        isError = true
                    )

                    _uiState.update {
                        it.copy(
                            messages = it.messages + errorMsg,
                            isThinking = false
                        )
                    }
                }
            )
        }
    }
}
