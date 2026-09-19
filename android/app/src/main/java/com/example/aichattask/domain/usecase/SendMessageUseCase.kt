package com.example.aichattask.domain.usecase

import com.example.aichattask.domain.model.Message
import com.example.aichattask.domain.repository.ChatRepository
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(roomId: String, message: Message) {
        chatRepository.sendMessage(roomId, message)
    }
}
