package com.example.data.model

data class TaskWithStatus(
    val task: RoutineTask,
    val isCompleted: Boolean,
    val completedAt: Long? = null,
    val currentStreak: Int = 0
)

data class DaySummary(
    val epochDay: Long,
    val totalTasks: Int,
    val completedTasks: Int,
    val completionPercentage: Float,
    val focusMinutes: Int
)
