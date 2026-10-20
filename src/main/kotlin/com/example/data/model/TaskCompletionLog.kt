package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "task_completion_logs",
    indices = [Index(value = ["taskId", "dateEpochDay"], unique = true)]
)
data class TaskCompletionLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val dateEpochDay: Long,
    val isCompleted: Boolean,
    val completedAt: Long = System.currentTimeMillis()
)
