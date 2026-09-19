package com.example.aichattask.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.aichattask.data.local.dao.MessageDao
import com.example.aichattask.data.local.dao.TaskDao
import com.example.aichattask.data.local.entity.MessageEntity
import com.example.aichattask.data.local.entity.TaskEntity

@Database(
    entities = [MessageEntity::class, TaskEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun taskDao(): TaskDao
}
