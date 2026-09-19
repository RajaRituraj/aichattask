package com.example.aichattask.domain.repository

import com.example.aichattask.domain.model.AiResult
import com.example.aichattask.domain.model.Message
import com.example.aichattask.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface AiRepository {
    /** Summarize messages, streaming the result as a Flow of AiResult */
    fun summarize(messages: List<Message>): Flow<AiResult>

    /** Ask a question about the chat; returns a single Answer */
    suspend fun ask(messages: List<Message>, question: String): AiResult

    /** Extract tasks from messages */
    suspend fun extractTasks(messages: List<Message>): AiResult

    // Task persistence
    fun getAllTasks(): Flow<List<Task>>
    suspend fun saveTask(task: Task)
    suspend fun updateTask(task: Task)
    suspend fun deleteTask(taskId: String)
}
