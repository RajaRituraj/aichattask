package com.example.aichattask.data.remote.socket

import com.example.aichattask.domain.model.ChatUser
import com.example.aichattask.domain.model.Message
import com.example.aichattask.domain.repository.ConnectionState
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocketService @Inject constructor() {

    private var socket: Socket? = null

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<Message>(extraBufferCapacity = 64)
    val incomingMessages: SharedFlow<Message> = _incomingMessages.asSharedFlow()

    private val _onlineUsers = MutableStateFlow<List<ChatUser>>(emptyList())
    val onlineUsers: StateFlow<List<ChatUser>> = _onlineUsers.asStateFlow()

    private val _typingUsers = MutableStateFlow<Map<String, String>>(emptyMap())
    val typingUsers: StateFlow<Map<String, String>> = _typingUsers.asStateFlow()

    /** Connect to server and join room. Includes reconnection logic. */
    fun connect(
        serverUrl: String,
        roomId: String,
        userId: String,
        userName: String,
        onHistory: (List<Message>) -> Unit
    ) {
        try {
            _connectionState.value = ConnectionState.CONNECTING
            val options = IO.Options.builder()
                .setReconnection(true)
                .setReconnectionAttempts(Int.MAX_VALUE)
                .setReconnectionDelay(1000)
                .setReconnectionDelayMax(10000)
                .build()

            socket = IO.socket(serverUrl, options)

            socket?.apply {
                on(Socket.EVENT_CONNECT) {
                    _connectionState.value = ConnectionState.CONNECTED
                    val joinPayload = JSONObject().apply {
                        put("roomId", roomId)
                        put("userId", userId)
                        put("userName", userName)
                    }
                    emit("join", joinPayload)
                }

                on(Socket.EVENT_DISCONNECT) {
                    _connectionState.value = ConnectionState.DISCONNECTED
                }

                on(Socket.EVENT_CONNECT_ERROR) {
                    _connectionState.value = ConnectionState.RECONNECTING
                }

                on("history") { args ->
                    val array = args.getOrNull(0) as? JSONArray ?: return@on
                    val messages = mutableListOf<Message>()
                    for (i in 0 until array.length()) {
                        val json = array.getJSONObject(i)
                        messages.add(json.toMessage(currentUserId = userId))
                    }
                    onHistory(messages)
                }

                on("new_message") { args ->
                    val json = args.getOrNull(0) as? JSONObject ?: return@on
                    val message = json.toMessage(currentUserId = userId)
                    _incomingMessages.tryEmit(message)
                }

                on("user_list") { args ->
                    val array = args.getOrNull(0) as? JSONArray ?: return@on
                    val users = mutableListOf<ChatUser>()
                    for (i in 0 until array.length()) {
                        val u = array.getJSONObject(i)
                        users.add(ChatUser(id = u.getString("id"), name = u.getString("name")))
                    }
                    _onlineUsers.value = users
                }

                on("typing") { args ->
                    val json = args.getOrNull(0) as? JSONObject ?: return@on
                    val uid = json.getString("userId")
                    val uname = json.getString("userName")
                    val isTyping = json.getBoolean("isTyping")
                    val current = _typingUsers.value.toMutableMap()
                    if (isTyping) current[uid] = uname else current.remove(uid)
                    _typingUsers.value = current
                }

                connect()
            }
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.ERROR
        }
    }

    fun sendMessage(roomId: String, message: Message) {
        val payload = JSONObject().apply {
            put("roomId", roomId)
            val msg = JSONObject().apply {
                put("id", message.id)
                put("roomId", message.roomId)
                put("senderId", message.senderId)
                put("senderName", message.senderName)
                put("content", message.content)
                put("timestamp", message.timestamp)
            }
            put("message", msg)
        }
        socket?.emit("send_message", payload)
    }

    fun sendTyping(roomId: String, userId: String, userName: String, isTyping: Boolean) {
        val payload = JSONObject().apply {
            put("roomId", roomId)
            put("userId", userId)
            put("userName", userName)
            put("isTyping", isTyping)
        }
        socket?.emit("typing", payload)
    }

    fun disconnect(roomId: String, userId: String) {
        val payload = JSONObject().apply {
            put("roomId", roomId)
            put("userId", userId)
        }
        socket?.emit("leave_room", payload)
        socket?.disconnect()
        socket = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }
}

private fun JSONObject.toMessage(currentUserId: String): Message {
    val senderId = getString("senderId")
    return Message(
        id = getString("id"),
        roomId = getString("roomId"),
        senderId = senderId,
        senderName = getString("senderName"),
        content = getString("content"),
        timestamp = getLong("timestamp"),
        isOwn = senderId == currentUserId,
        serverTimestamp = optLong("serverTimestamp").takeIf { it > 0 }
    )
}
