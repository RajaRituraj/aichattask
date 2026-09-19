package com.example.aichattask.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val owner: String?,
    val dueDate: String?,
    val isDone: Boolean,
    val sourceMessageId: String?,
    val createdAt: Long
)
