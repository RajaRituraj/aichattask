package com.example.aichattask.presentation.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aichattask.domain.model.*
import com.example.aichattask.domain.usecase.AskChatUseCase
import com.example.aichattask.domain.usecase.ExtractTasksUseCase
import com.example.aichattask.domain.usecase.SummarizeUseCase
import com.example.aichattask.domain.repository.AiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

enum class AiMode { IDLE, SUMMARY, ASK, TASKS }

data class AiPanelUiState(
    val mode: AiMode = AiMode.IDLE,
    val isLoading: Boolean = false,
    val summaryText: String = "",
    val isSummaryStreaming: Boolean = false,
    val answerData: AiAnswer? = null,
    val taskSuggestions: List<AiTaskSuggestion> = emptyList(),
    val confirmedTasks: Set<String> = emptySet(),
    val question: String = "",
    val error: String? = null
)

@HiltViewModel
class AiPanelViewModel @Inject constructor(
    private val summarizeUseCase: SummarizeUseCase,
    private val askChatUseCase: AskChatUseCase,
    private val extractTasksUseCase: ExtractTasksUseCase,
    private val aiRepository: AiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiPanelUiState())
    val uiState: StateFlow<AiPanelUiState> = _uiState.asStateFlow()

    private var currentMessages: List<Message> = emptyList()

    fun setMessages(messages: List<Message>) {
        currentMessages = messages
    }

    fun summarize() {
        if (currentMessages.isEmpty()) return
        _uiState.update { it.copy(mode = AiMode.SUMMARY, summaryText = "", error = null, isLoading = true) }

        viewModelScope.launch {
            summarizeUseCase(currentMessages).collect { result ->
                when (result) {
                    is AiResult.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is AiResult.Summary -> _uiState.update {
                        it.copy(
                            isLoading = false,
                            summaryText = result.text,
                            isSummaryStreaming = result.isStreaming
                        )
                    }
                    is AiResult.Error -> _uiState.update {
                        it.copy(isLoading = false, error = result.message)
                    }
                    else -> {}
                }
            }
        }
    }

    fun onQuestionChange(text: String) = _uiState.update { it.copy(question = text) }

    fun askQuestion() {
        val question = _uiState.value.question.trim()
        if (question.isEmpty() || currentMessages.isEmpty()) return

        _uiState.update { it.copy(mode = AiMode.ASK, answerData = null, error = null, isLoading = true) }
        viewModelScope.launch {
            val result = askChatUseCase(currentMessages, question)
            when (result) {
                is AiResult.Answer -> _uiState.update { it.copy(isLoading = false, answerData = result.data) }
                is AiResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                else -> {}
            }
        }
    }

    fun extractTasks() {
        if (currentMessages.isEmpty()) return
        _uiState.update { it.copy(mode = AiMode.TASKS, taskSuggestions = emptyList(), error = null, isLoading = true) }
        viewModelScope.launch {
            val result = extractTasksUseCase(currentMessages)
            when (result) {
                is AiResult.Tasks -> _uiState.update { it.copy(isLoading = false, taskSuggestions = result.suggestions) }
                is AiResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                else -> {}
            }
        }
    }

    fun confirmTask(suggestion: AiTaskSuggestion) {
        viewModelScope.launch {
            val task = Task(
                id = UUID.randomUUID().toString(),
                title = suggestion.title,
                owner = suggestion.owner,
                dueDate = suggestion.dueDate,
                isDone = false,
                sourceMessageId = suggestion.sourceMessageId
            )
            aiRepository.saveTask(task)
            _uiState.update { it.copy(confirmedTasks = it.confirmedTasks + suggestion.title) }
        }
    }

    fun reset() = _uiState.update { AiPanelUiState() }
}
