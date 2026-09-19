package com.example.aichattask.data.mapper

import com.example.aichattask.data.local.entity.MessageEntity
import com.example.aichattask.data.local.entity.TaskEntity
import com.example.aichattask.data.remote.api.MessageDto
import com.example.aichattask.domain.model.Message
import com.example.aichattask.domain.model.Task

// ─── Message mappers ──────────────────────────────────────────────────────────

fun Message.toEntity(): MessageEntity = MessageEntity(
    id = id,
    roomId = roomId,
    senderId = senderId,
    senderName = senderName,
    content = content,
    timestamp = timestamp,
    isOwn = isOwn,
    serverTimestamp = serverTimestamp
)

fun MessageEntity.toDomain(): Message = Message(
    id = id,
    roomId = roomId,
    senderId = senderId,
    senderName = senderName,
    content = content,
    timestamp = timestamp,
    isOwn = isOwn,
    serverTimestamp = serverTimestamp
)

fun Message.toDto(): MessageDto = MessageDto(
    id = id,
    senderId = senderId,
    senderName = senderName,
    content = content,
    timestamp = timestamp
)

// ─── Task mappers ─────────────────────────────────────────────────────────────

fun Task.toEntity(): TaskEntity = TaskEntity(
    id = id,
    title = title,
    owner = owner,
    dueDate = dueDate,
    isDone = isDone,
    sourceMessageId = sourceMessageId,
    createdAt = createdAt
)

fun TaskEntity.toDomain(): Task = Task(
    id = id,
    title = title,
    owner = owner,
    dueDate = dueDate,
    isDone = isDone,
    sourceMessageId = sourceMessageId,
    createdAt = createdAt
)
