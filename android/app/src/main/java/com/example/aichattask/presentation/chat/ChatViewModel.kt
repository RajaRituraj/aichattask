package com.example.aichattask.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aichattask.domain.model.Message
import com.example.aichattask.domain.repository.ConnectionState
import com.example.aichattask.domain.usecase.GetMessagesUseCase
import com.example.aichattask.domain.usecase.SendMessageUseCase
import com.example.aichattask.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val inputText: String = "",
    val onlineUsers: List<String> = emptyList(),
    val typingUsers: List<String> = emptyList(),
    val isConnecting: Boolean = false
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val getMessagesUseCase: GetMessagesUseCase,
    private val sendMessageUseCase: SendMessageUseCase
) : ViewModel() {

    companion object {
        const val ROOM_ID = "general"
        const val SERVER_URL = "http://192.168.29.11:3000"
    }

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var currentUserId = ""
    private var currentUserName = ""
    private var typingJob: Job? = null

    fun initialize(userId: String, userName: String) {
        currentUserId = userId
        currentUserName = userName

        // Observe messages from Room
        viewModelScope.launch {
            getMessagesUseCase(ROOM_ID).collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }

        // Observe connection state
        viewModelScope.launch {
            chatRepository.connectionState.collect { state ->
                _uiState.update { it.copy(connectionState = state, isConnecting = state == ConnectionState.CONNECTING) }
            }
        }

        // Observe online users
        viewModelScope.launch {
            chatRepository.onlineUsers.collect { users ->
                _uiState.update { it.copy(onlineUsers = users.map { u -> u.name }) }
            }
        }

        // Observe typing users
        viewModelScope.launch {
            chatRepository.typingUsers.collect { typing ->
                _uiState.update { it.copy(typingUsers = typing.values.filter { n -> n != currentUserName }) }
            }
        }

        // Connect
        viewModelScope.launch {
            chatRepository.connect(SERVER_URL, ROOM_ID, userId, userName)
        }
    }

    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputText = text) }

        // Typing indicator — debounce stop after 2 seconds
        viewModelScope.launch {
            chatRepository.sendTyping(ROOM_ID, currentUserId, currentUserName, true)
        }
        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            delay(2000)
            chatRepository.sendTyping(ROOM_ID, currentUserId, currentUserName, false)
        }
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) return

        val message = Message(
            id = UUID.randomUUID().toString(),
            roomId = ROOM_ID,
            senderId = currentUserId,
            senderName = currentUserName,
            content = text,
            timestamp = System.currentTimeMillis(),
            isOwn = true
        )

        _uiState.update { it.copy(inputText = "") }

        viewModelScope.launch {
            // Stop typing indicator
            chatRepository.sendTyping(ROOM_ID, currentUserId, currentUserName, false)
            sendMessageUseCase(ROOM_ID, message)
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            chatRepository.disconnect(ROOM_ID, currentUserId)
        }
    }
}
