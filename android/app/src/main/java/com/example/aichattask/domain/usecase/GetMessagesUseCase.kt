package com.example.aichattask.domain.usecase

import com.example.aichattask.domain.model.Message
import com.example.aichattask.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMessagesUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(roomId: String): Flow<List<Message>> =
        chatRepository.getMessages(roomId)
}
