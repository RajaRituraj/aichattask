package com.example.aichattask.domain.model

enum class AiResultType { SUMMARY, ANSWER, TASKS }

data class AiAnswer(
    val answer: String,
    val referencedMessageId: String?,
    val excerpt: String?
)

data class AiTaskSuggestion(
    val title: String,
    val owner: String?,
    val dueDate: String?,
    val sourceMessageId: String?
)

sealed class AiResult {
    data class Summary(val text: String, val isStreaming: Boolean = false) : AiResult()
    data class Answer(val data: AiAnswer) : AiResult()
    data class Tasks(val suggestions: List<AiTaskSuggestion>) : AiResult()
    data class Error(val message: String) : AiResult()
    object Loading : AiResult()
}
