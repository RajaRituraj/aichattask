package com.example.aichattask.domain.usecase

import com.example.aichattask.domain.model.AiResult
import com.example.aichattask.domain.model.Message
import com.example.aichattask.domain.repository.AiRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SummarizeUseCase @Inject constructor(
    private val aiRepository: AiRepository
) {
    operator fun invoke(messages: List<Message>): Flow<AiResult> =
        aiRepository.summarize(messages)
}
