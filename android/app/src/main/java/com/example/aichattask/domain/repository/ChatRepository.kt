package com.example.aichattask.domain.repository

import com.example.aichattask.domain.model.ChatUser
import com.example.aichattask.domain.model.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

interface ChatRepository {
    /** Live stream of messages from local DB for a room */
    fun getMessages(roomId: String): Flow<List<Message>>

    /** Incoming socket events: new messages pushed from server */
    val incomingMessages: SharedFlow<Message>

    /** Socket connection state */
    val connectionState: Flow<ConnectionState>

    /** Online users in the current room */
    val onlineUsers: Flow<List<ChatUser>>

    /** Typing indicators: userId → isTyping */
    val typingUsers: Flow<Map<String, String>>

    /** Connect to the socket server and join a room */
    suspend fun connect(serverUrl: String, roomId: String, userId: String, userName: String)

    /** Send a message through the socket */
    suspend fun sendMessage(roomId: String, message: Message)

    /** Emit typing indicator */
    suspend fun sendTyping(roomId: String, userId: String, userName: String, isTyping: Boolean)

    /** Disconnect from the socket */
    suspend fun disconnect(roomId: String, userId: String)
}

enum class ConnectionState { CONNECTING, CONNECTED, DISCONNECTED, RECONNECTING, ERROR }
