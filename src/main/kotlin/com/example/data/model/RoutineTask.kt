package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routine_tasks")
data class RoutineTask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String = "Health", // Health, Focus, Mind, Productivity, Habit
    val timeOfDay: String = "Morning", // Morning, Afternoon, Evening, Anytime
    val targetMinutes: Int = 0,
    val daysOfWeek: String = "1,2,3,4,5,6,7", // 1=Mon .. 7=Sun
    val repeatType: String = "WEEKLY", // "WEEKLY", "INTERVAL"
    val repeatIntervalDays: Int = 1, // 2 for every 2 days, 3 for every 3 days, etc.
    val startEpochDay: Long = 0L, // Anchor epoch day for intervals
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val displayOrder: Int = 0
)
