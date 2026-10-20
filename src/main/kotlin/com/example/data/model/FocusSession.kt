package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_sessions")
data class FocusSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long? = null,
    val taskTitle: String = "",
    val durationSeconds: Int,
    val mode: String = "Pomodoro", // Pomodoro, Deep Work, Short Break, Custom
    val completedAt: Long = System.currentTimeMillis(),
    val dateEpochDay: Long
)
