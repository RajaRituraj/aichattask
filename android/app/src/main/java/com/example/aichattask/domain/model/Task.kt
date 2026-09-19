package com.example.aichattask.domain.model

data class Task(
    val id: String,
    val title: String,
    val owner: String?,
    val dueDate: String?,
    val isDone: Boolean = false,
    val sourceMessageId: String?,
    val createdAt: Long = System.currentTimeMillis()
)
