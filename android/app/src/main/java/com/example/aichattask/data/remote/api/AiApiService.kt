package com.example.aichattask.data.remote.api

import com.google.gson.JsonObject
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Streaming

data class SummarizeRequest(val messages: List<MessageDto>)
data class AskRequest(val messages: List<MessageDto>, val question: String)
data class ExtractTasksRequest(val messages: List<MessageDto>)

data class MessageDto(
    val id: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val timestamp: Long
)

data class AskResponse(
    val answer: String,
    val messageId: String?,
    val excerpt: String?
)

data class TaskDto(
    val title: String,
    val owner: String?,
    val dueDate: String?,
    val sourceMessageId: String?
)

data class ExtractTasksResponse(val tasks: List<TaskDto>)

interface AiApiService {
    @Streaming
    @POST("api/summarize")
    suspend fun summarize(@Body request: SummarizeRequest): Response<ResponseBody>

    @POST("api/ask")
    suspend fun ask(@Body request: AskRequest): AskResponse

    @POST("api/extract-tasks")
    suspend fun extractTasks(@Body request: ExtractTasksRequest): ExtractTasksResponse
}
