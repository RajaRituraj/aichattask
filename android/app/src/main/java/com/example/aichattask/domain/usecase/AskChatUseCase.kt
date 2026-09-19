package com.example.aichattask.domain.usecase

import com.example.aichattask.domain.model.AiResult
import com.example.aichattask.domain.model.Message
import com.example.aichattask.domain.repository.AiRepository
import javax.inject.Inject

class AskChatUseCase @Inject constructor(
    private val aiRepository: AiRepository
) {
    suspend operator fun invoke(messages: List<Message>, question: String): AiResult =
        aiRepository.ask(messages, question)
}
