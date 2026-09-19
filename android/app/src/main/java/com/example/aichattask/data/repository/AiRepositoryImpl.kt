package com.example.aichattask.data.repository

import com.example.aichattask.data.local.dao.TaskDao
import com.example.aichattask.data.mapper.toDomain
import com.example.aichattask.data.mapper.toDto
import com.example.aichattask.data.mapper.toEntity
import com.example.aichattask.data.remote.api.AiApiService
import com.example.aichattask.data.remote.api.AskRequest
import com.example.aichattask.data.remote.api.ExtractTasksRequest
import com.example.aichattask.data.remote.api.SummarizeRequest
import com.example.aichattask.domain.model.AiAnswer
import com.example.aichattask.domain.model.AiResult
import com.example.aichattask.domain.model.AiTaskSuggestion
import com.example.aichattask.domain.model.Message
import com.example.aichattask.domain.model.Task
import com.example.aichattask.domain.repository.AiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiRepositoryImpl @Inject constructor(
    private val aiApiService: AiApiService,
    private val taskDao: TaskDao
) : AiRepository {

    /** SSE streaming summary */
    override fun summarize(messages: List<Message>): Flow<AiResult> = flow {
        emit(AiResult.Loading)
        try {
            val response = aiApiService.summarize(SummarizeRequest(messages.map { it.toDto() }))
            if (!response.isSuccessful) {
                emit(AiResult.Error("Server error: ${response.code()}"))
                return@flow
            }
            val body = response.body() ?: run {
                emit(AiResult.Error("Empty response"))
                return@flow
            }
            val reader = BufferedReader(InputStreamReader(body.byteStream()))
            val accumulated = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val l = line ?: continue
                if (l.startsWith("data: ")) {
                    val data = l.removePrefix("data: ").trim()
                    if (data == "[DONE]") break
                    try {
                        val json = org.json.JSONObject(data)
                        if (json.has("error")) {
                            emit(AiResult.Error(json.getString("error")))
                            return@flow
                        }
                        val text = json.optString("text", "")
                        if (text.isNotEmpty()) {
                            accumulated.append(text)
                            emit(AiResult.Summary(text = accumulated.toString(), isStreaming = true))
                        }
                    } catch (_: Exception) { }
                }
            }
            emit(AiResult.Summary(text = accumulated.toString(), isStreaming = false))
        } catch (e: Exception) {
            emit(AiResult.Error(e.message ?: "Network error"))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun ask(messages: List<Message>, question: String): AiResult =
        withContext(Dispatchers.IO) {
            try {
                val response = aiApiService.ask(
                    AskRequest(messages = messages.map { it.toDto() }, question = question)
                )
                AiResult.Answer(
                    AiAnswer(
                        answer = response.answer,
                        referencedMessageId = response.messageId,
                        excerpt = response.excerpt
                    )
                )
            } catch (e: Exception) {
                AiResult.Error(e.message ?: "Network error")
            }
        }

    override suspend fun extractTasks(messages: List<Message>): AiResult =
        withContext(Dispatchers.IO) {
            try {
                val response = aiApiService.extractTasks(
                    ExtractTasksRequest(messages = messages.map { it.toDto() })
                )
                AiResult.Tasks(
                    suggestions = response.tasks.map {
                        AiTaskSuggestion(
                            title = it.title,
                            owner = it.owner,
                            dueDate = it.dueDate,
                            sourceMessageId = it.sourceMessageId
                        )
                    }
                )
            } catch (e: Exception) {
                AiResult.Error(e.message ?: "Network error")
            }
        }

    // Task persistence
    override fun getAllTasks(): Flow<List<Task>> =
        taskDao.observeTasks().map { entities -> entities.map { it.toDomain() } }

    override suspend fun saveTask(task: Task) = withContext(Dispatchers.IO) {
        taskDao.insertTask(task.toEntity())
    }

    override suspend fun updateTask(task: Task) = withContext(Dispatchers.IO) {
        taskDao.updateTask(task.toEntity())
    }

    override suspend fun deleteTask(taskId: String) = withContext(Dispatchers.IO) {
        taskDao.deleteTask(taskId)
    }
}
