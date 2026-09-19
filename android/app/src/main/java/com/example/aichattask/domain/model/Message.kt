package com.example.aichattask.domain.model

data class Message(
    val id: String,
    val roomId: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val timestamp: Long,
    val isOwn: Boolean,
    val serverTimestamp: Long? = null
)
