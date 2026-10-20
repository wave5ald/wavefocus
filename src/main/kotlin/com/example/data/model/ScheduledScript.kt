package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scheduled_scripts")
data class ScheduledScript(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val encryptedContent: String = "",
    val iv: String = "",
    val startDateEpochDay: Long,
    val endDateEpochDay: Long = startDateEpochDay,
    val category: String = "Quest", // "Quest", "Goal", "Challenge", "Note", "Daily Script"
    val updatedAt: Long = System.currentTimeMillis(),
    val isEncrypted: Boolean = false,
    val isCompleted: Boolean = false,
    val targetValue: String = "", // e.g. "5 km", "45 mins", "Chapter 3", "100 reps"
    val xpReward: Int = 50,
    val questType: String = "Daily Quest", // "Daily Quest", "Milestone Goal", "Epic Challenge", "Focus Target"
    val emoji: String = "🎯"
)
