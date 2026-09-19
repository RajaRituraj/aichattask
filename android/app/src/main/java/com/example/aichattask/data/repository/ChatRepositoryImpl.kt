package com.example.aichattask.data.repository

import com.example.aichattask.data.local.dao.MessageDao
import com.example.aichattask.data.mapper.toDomain
import com.example.aichattask.data.mapper.toEntity
import com.example.aichattask.data.remote.socket.SocketService
import com.example.aichattask.domain.model.ChatUser
import com.example.aichattask.domain.model.Message
import com.example.aichattask.domain.repository.ChatRepository
import com.example.aichattask.domain.repository.ConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val socketService: SocketService,
    private val messageDao: MessageDao
) : ChatRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun getMessages(roomId: String): Flow<List<Message>> =
        messageDao.observeMessages(roomId).map { entities -> entities.map { it.toDomain() } }

    override val incomingMessages: SharedFlow<Message> = socketService.incomingMessages

    override val connectionState: Flow<ConnectionState> = socketService.connectionState

    override val onlineUsers: Flow<List<ChatUser>> = socketService.onlineUsers

    override val typingUsers: Flow<Map<String, String>> = socketService.typingUsers

    override suspend fun connect(
        serverUrl: String,
        roomId: String,
        userId: String,
        userName: String
    ) {
        socketService.connect(serverUrl, roomId, userId, userName) { historyMessages ->
            // Persist history to Room
            scope.launch {
                messageDao.insertMessages(historyMessages.map { it.toEntity() })
            }
        }

        // Persist all incoming real-time messages to Room
        scope.launch {
            socketService.incomingMessages.collect { message ->
                messageDao.insertMessage(message.toEntity())
            }
        }
    }

    override suspend fun sendMessage(roomId: String, message: Message) {
        // Optimistic insert to Room first (so UI shows immediately)
        messageDao.insertMessage(message.toEntity())
        // Emit to server
        socketService.sendMessage(roomId, message)
    }

    override suspend fun sendTyping(roomId: String, userId: String, userName: String, isTyping: Boolean) {
        socketService.sendTyping(roomId, userId, userName, isTyping)
    }

    override suspend fun disconnect(roomId: String, userId: String) {
        socketService.disconnect(roomId, userId)
    }
}
