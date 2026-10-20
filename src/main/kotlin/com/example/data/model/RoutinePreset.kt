package com.example.data.model

import java.util.UUID

data class PresetTaskItem(
    val title: String,
    val category: String = "Health",
    val timeOfDay: String = "Morning", // Morning, Afternoon, Evening, Anytime
    val targetMinutes: Int = 15,
    val daysOfWeek: String = "1,2,3,4,5,6,7", // 1=Mon .. 7=Sun
    val repeatType: String = "WEEKLY", // "WEEKLY", "INTERVAL"
    val repeatIntervalDays: Int = 1
)

data class RoutinePreset(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val iconEmoji: String = "📋",
    val colorHex: String = "#6366F1",
    val tasks: List<PresetTaskItem> = emptyList(),
    val isBuiltIn: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class ScheduledPresetPlan(
    val id: String = UUID.randomUUID().toString(),
    val presetId: String,
    val presetName: String,
    val presetIcon: String = "📋",
    val presetColorHex: String = "#6366F1",
    val taskCount: Int = 0,
    val startDateEpochDay: Long,
    val note: String = "",
    val replaceExisting: Boolean = true,
    val isApplied: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
